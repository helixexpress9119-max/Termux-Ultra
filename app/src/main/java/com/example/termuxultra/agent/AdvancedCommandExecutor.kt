package com.example.termuxultra.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

class AdvancedCommandExecutor(private val context: Context) {
    
    suspend fun executeCommand(
        command: ParsedCommand, 
        commandContext: CommandContext
    ): String = withContext(Dispatchers.IO) {
        
        return@withContext when (command.action) {
            "cd" -> handleChangeDirectory(command, commandContext)
            "ls" -> handleListFiles(command, commandContext)
            "pwd" -> handlePrintWorkingDirectory(commandContext)
            "cat" -> handleCatFile(command, commandContext)
            "mkdir" -> handleMakeDirectory(command, commandContext)
            "rm" -> handleRemoveFile(command, commandContext)
            "gradle" -> handleGradleCommand(command, commandContext)
            "git" -> handleGitCommand(command, commandContext)
            "system_info" -> handleSystemInfo()
            "network_info" -> handleNetworkInfo()
            "help" -> handleHelp()
            "unknown" -> handleUnknownCommand(command)
            "noop" -> "No operation"
            else -> handleGenericShellCommand(command, commandContext)
        }
    }
    
    private fun handleChangeDirectory(command: ParsedCommand, commandContext: CommandContext): String {
        val target = command.target ?: return "❌ No directory specified"
        
        val newDir = when {
            target == ".." -> commandContext.currentDirectory.parentFile
            target == "~" -> File(context.filesDir, "home")
            target.startsWith("/") -> File(target)
            else -> File(commandContext.currentDirectory, target)
        }
        
        return if (newDir?.exists() == true && newDir.isDirectory) {
            val canonicalDir = newDir.canonicalFile
            // Update context file
            val ctxFile = File(context.filesDir, "ultra_context.json")
            ctxFile.writeText(canonicalDir.absolutePath)
            
            // Update the command context
            commandContext.currentDirectory = canonicalDir
            
            "✅ Changed directory to ${canonicalDir.absolutePath}"
        } else {
            "❌ Directory not found: ${newDir?.absolutePath}"
        }
    }
    
    private fun handleListFiles(command: ParsedCommand, context: CommandContext): String {
        val target = command.target?.let { File(context.currentDirectory, it) } 
                    ?: context.currentDirectory
        
        if (!target.exists()) {
            return "❌ Path not found: ${target.absolutePath}"
        }
        
        val files = target.listFiles() ?: return "❌ Cannot read directory"
        
        val showAll = command.options.containsKey("-a") || command.options.containsKey("-la")
        val longFormat = command.options.containsKey("-l") || command.options.containsKey("-la")
        
        val filteredFiles = if (showAll) files else files.filter { !it.name.startsWith(".") }
        
        return if (longFormat) {
            buildString {
                appendLine("📁 ${target.absolutePath}")
                appendLine("Total: ${filteredFiles.size} items")
                appendLine()
                
                filteredFiles.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name })
                    .forEach { file ->
                        val type = if (file.isDirectory) "📁" else "📄"
                        val size = if (file.isFile) formatFileSize(file.length()) else "-"
                        val permissions = getPermissions(file)
                        appendLine("$type $permissions ${size.padStart(8)} ${file.name}")
                    }
            }
        } else {
            buildString {
                appendLine("📁 ${target.absolutePath}")
                val grouped = filteredFiles.groupBy { it.isDirectory }
                
                grouped[true]?.sortedBy { it.name }?.forEach { dir ->
                    appendLine("📁 ${dir.name}/")
                }
                grouped[false]?.sortedBy { it.name }?.forEach { file ->
                    appendLine("📄 ${file.name}")
                }
            }
        }
    }
    
    private fun handlePrintWorkingDirectory(context: CommandContext): String {
        return "📍 ${context.currentDirectory.absolutePath}"
    }
    
    private fun handleCatFile(command: ParsedCommand, context: CommandContext): String {
        val filename = command.target ?: return "❌ No file specified"
        val file = File(context.currentDirectory, filename)
        
        return if (file.exists() && file.isFile) {
            try {
                val content = file.readText()
                if (content.length > 2000) {
                    "${content.take(2000)}\n... (file truncated, ${content.length} total characters)"
                } else {
                    content
                }
            } catch (e: Exception) {
                "❌ Error reading file: ${e.message}"
            }
        } else {
            "❌ File not found: ${file.absolutePath}"
        }
    }
    
    private fun handleMakeDirectory(command: ParsedCommand, context: CommandContext): String {
        val dirname = command.target ?: return "❌ No directory name specified"
        val newDir = File(context.currentDirectory, dirname)
        
        return if (newDir.exists()) {
            "⚠️ Directory already exists: ${newDir.name}"
        } else {
            try {
                val created = newDir.mkdirs()
                if (created) {
                    "✅ Created directory: ${newDir.name}"
                } else {
                    "❌ Failed to create directory: ${newDir.name}"
                }
            } catch (e: Exception) {
                "❌ Error creating directory: ${e.message}"
            }
        }
    }
    
    private fun handleRemoveFile(command: ParsedCommand, context: CommandContext): String {
        val filename = command.target ?: return "❌ No file specified"
        val file = File(context.currentDirectory, filename)
        
        return if (!file.exists()) {
            "❌ File not found: ${file.name}"
        } else {
            try {
                val isRecursive = command.options.containsKey("-r") || command.options.containsKey("-rf")
                val deleted = if (file.isDirectory && isRecursive) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                
                if (deleted) {
                    "✅ Deleted: ${file.name}"
                } else {
                    "❌ Failed to delete: ${file.name}"
                }
            } catch (e: Exception) {
                "❌ Error deleting: ${e.message}"
            }
        }
    }
    
    private suspend fun handleGradleCommand(command: ParsedCommand, context: CommandContext): String {
        val task = command.target ?: "help"
        val gradlew = File(context.currentDirectory, "gradlew")
        
        return if (gradlew.exists()) {
            executeShellCommand("./gradlew $task", context.currentDirectory)
        } else {
            "❌ gradlew not found in current directory"
        }
    }
    
    private suspend fun handleGitCommand(command: ParsedCommand, context: CommandContext): String {
        val gitCommand = command.target ?: "status"
        val options = command.options.entries.joinToString(" ") { "${it.key} ${it.value}" }
        val fullCommand = "git $gitCommand $options".trim()
        
        return executeShellCommand(fullCommand, context.currentDirectory)
    }
    
    private fun handleSystemInfo(): String {
        return buildString {
            appendLine("🤖 Termux-Ultra System Information")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📱 Platform: Android")
            appendLine("🏗️ Architecture: ${System.getProperty("os.arch")}")
            appendLine("☕ Java Version: ${System.getProperty("java.version")}")
            appendLine("🧠 Available Processors: ${Runtime.getRuntime().availableProcessors()}")
            appendLine("💾 Max Memory: ${formatFileSize(Runtime.getRuntime().maxMemory())}")
            appendLine("📦 Free Memory: ${formatFileSize(Runtime.getRuntime().freeMemory())}")
        }
    }
    
    private fun handleNetworkInfo(): String {
        return buildString {
            appendLine("🌐 Network Information")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📡 Network access through Android system")
            appendLine("🔗 Internet connectivity depends on device settings")
            appendLine("📍 Use system settings to check WiFi/mobile data")
        }
    }
    
    private fun handleHelp(): String {
        return buildString {
            appendLine("🤖 Termux-Ultra Chat Agent Help")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("📁 File Operations:")
            appendLine("  • 'list files' or 'ls' - Show directory contents")
            appendLine("  • 'show content of file.txt' - Display file content")
            appendLine("  • 'create folder test' - Create directory")
            appendLine("  • 'delete file.txt' - Remove file/directory")
            appendLine()
            appendLine("🧭 Navigation:")
            appendLine("  • 'go to documents' - Change directory")
            appendLine("  • 'where am i' - Show current directory")
            appendLine("  • 'go back' - Go to parent directory")
            appendLine()
            appendLine("🔨 Development:")
            appendLine("  • 'build app' - Run gradle build")
            appendLine("  • 'test app' - Run tests")
            appendLine("  • 'clean build' - Clean project")
            appendLine()
            appendLine("🌐 Git Operations:")
            appendLine("  • 'git status' - Check git status")
            appendLine("  • 'git commit \"message\"' - Commit changes")
            appendLine()
            appendLine("ℹ️ System:")
            appendLine("  • 'system info' - Show system information")
            appendLine("  • 'network info' - Show network status")
            appendLine("  • 'help' - Show this help")
        }
    }
    
    private fun handleUnknownCommand(command: ParsedCommand): String {
        return buildString {
            appendLine("❓ Command not recognized: '${command.target}'")
            appendLine()
            appendLine("💡 Try:")
            appendLine("  • Type 'help' for available commands")
            appendLine("  • Use natural language like 'list files'")
            appendLine("  • Or use shell commands like 'ls -la'")
        }
    }
    
    private suspend fun handleGenericShellCommand(command: ParsedCommand, context: CommandContext): String {
        val fullCommand = buildString {
            append(command.action)
            command.target?.let { append(" $it") }
            command.options.forEach { (key, value) ->
                append(" $key")
                if (value != "true") append(" $value")
            }
        }
        
        return executeShellCommand(fullCommand, context.currentDirectory)
    }
    
    private suspend fun executeShellCommand(command: String, workingDir: File): String = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(*command.split(" ").toTypedArray())
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            if (output.trim().isEmpty()) {
                "✅ Command executed successfully"
            } else {
                output
            }
        } catch (e: Exception) {
            "❌ Error executing command: ${e.message}"
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.1f %s", size, units[unitIndex])
    }
    
    private fun getPermissions(file: File): String {
        return buildString {
            append(if (file.canRead()) "r" else "-")
            append(if (file.canWrite()) "w" else "-")
            append(if (file.canExecute()) "x" else "-")
        }
    }
}