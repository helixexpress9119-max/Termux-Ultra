package com.example.terminal

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.util.concurrent.ConcurrentHashMap

data class TerminalSession(
    val id: String,
    val workingDirectory: String = "/",
    val environment: MutableMap<String, String> = mutableMapOf(),
    var isActive: Boolean = true,
    val history: MutableList<String> = mutableListOf(),
    var currentCommand: String = ""
)

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String,
    val executionTimeMs: Long
)

object TerminalCore {
    private val sessions = ConcurrentHashMap<String, TerminalSession>()
    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()
    
    private val _currentSession = MutableStateFlow<TerminalSession?>(null)
    val currentSession: StateFlow<TerminalSession?> = _currentSession.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Built-in commands
    private val builtinCommands = mapOf(
        "cd" to ::changeDirectory,
        "pwd" to ::printWorkingDirectory,
        "ls" to ::listFiles,
        "clear" to ::clearTerminal,
        "exit" to ::exitSession,
        "help" to ::showHelp,
        "echo" to ::echoCommand,
        "cat" to ::catCommand,
        "mkdir" to ::makeDirectory,
        "rm" to ::removeFile,
        "cp" to ::copyFile,
        "mv" to ::moveFile,
        "ps" to ::listProcesses,
        "env" to ::showEnvironment,
        "export" to ::exportVariable,
        "history" to ::showHistory
    )
    
    fun createSession(id: String, workingDir: String = "/"): TerminalSession {
        val session = TerminalSession(
            id = id,
            workingDirectory = workingDir,
            environment = getDefaultEnvironment().toMutableMap()
        )
        sessions[id] = session
        _currentSession.value = session
        appendOutput("Terminal session $id created\n")
        return session
    }
    
    fun switchSession(id: String): Boolean {
        val session = sessions[id]
        return if (session != null && session.isActive) {
            _currentSession.value = session
            appendOutput("Switched to session $id\n")
            true
        } else {
            appendOutput("Session $id not found or inactive\n")
            false
        }
    }
    
    fun runCommand(command: String): String {
        val session = _currentSession.value ?: run {
            val defaultSession = createSession("default")
            defaultSession
        }
        
        if (command.isBlank()) return ""
        
        session.history.add(command)
        session.currentCommand = command
        
        coroutineScope.launch {
            executeCommand(session, command.trim())
        }
        
        return "Command queued: $command"
    }
    
    private suspend fun executeCommand(session: TerminalSession, command: String) {
        val startTime = System.currentTimeMillis()
        val parts = parseCommand(command)
        val cmd = parts.firstOrNull() ?: return
        val args = parts.drop(1)
        
        appendOutput("${session.workingDirectory}$ $command\n")
        
        try {
            val result = if (builtinCommands.containsKey(cmd)) {
                // Execute built-in command
                builtinCommands[cmd]?.invoke(session, args) ?: CommandResult(1, "", "Unknown builtin", 0)
            } else {
                // Execute external command via Bifrost agent system
                executeExternalCommand(session, cmd, args)
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            if (result.output.isNotEmpty()) {
                appendOutput(result.output)
            }
            
            if (result.error.isNotEmpty()) {
                appendOutput("Error: ${result.error}\n")
            }
            
            if (result.exitCode != 0) {
                appendOutput("Exit code: ${result.exitCode}\n")
            }
            
        } catch (e: Exception) {
            appendOutput("Command failed: ${e.message}\n")
        }
        
        session.currentCommand = ""
    }
    
    private suspend fun executeExternalCommand(
        session: TerminalSession, 
        command: String, 
        args: List<String>
    ): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                val fullCommand = if (args.isNotEmpty()) {
                    "$command ${args.joinToString(" ")}"
                } else {
                    command
                }
                
                // Create agent task for system command execution
                val taskJson = """
                {
                    "id": "${java.util.UUID.randomUUID()}",
                    "agent_type": "system",
                    "command": "$fullCommand",
                    "args": [],
                    "environment": ${session.environment.toJson()}
                }
                """.trimIndent()
                
                // This would call into Rust Bifrost system
                // For now, simulate with basic process execution
                val processBuilder = ProcessBuilder(command, *args.toTypedArray())
                processBuilder.directory(File(session.workingDirectory))
                processBuilder.environment().putAll(session.environment)
                
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                
                CommandResult(exitCode, output, error, 0)
            } catch (e: Exception) {
                CommandResult(1, "", e.message ?: "Unknown error", 0)
            }
        }
    }
    
    // Built-in command implementations
    private fun changeDirectory(session: TerminalSession, args: List<String>): CommandResult {
        val targetDir = args.firstOrNull() ?: session.environment["HOME"] ?: "/"
        val newDir = if (targetDir.startsWith("/")) {
            targetDir
        } else {
            "${session.workingDirectory}/$targetDir"
        }
        
        val file = File(newDir)
        return if (file.exists() && file.isDirectory) {
            session.workingDirectory = file.canonicalPath
            session.environment["PWD"] = session.workingDirectory
            CommandResult(0, "", "", 0)
        } else {
            CommandResult(1, "", "Directory not found: $newDir", 0)
        }
    }
    
    private fun printWorkingDirectory(session: TerminalSession, args: List<String>): CommandResult {
        return CommandResult(0, "${session.workingDirectory}\n", "", 0)
    }
    
    private fun listFiles(session: TerminalSession, args: List<String>): CommandResult {
        val dir = args.firstOrNull() ?: session.workingDirectory
        val file = File(dir)
        
        return if (file.exists() && file.isDirectory) {
            val files = file.listFiles()?.sortedBy { it.name }?.joinToString("\n") { file ->
                val type = if (file.isDirectory) "d" else "-"
                val permissions = "rwxr-xr-x" // Simplified
                val size = if (file.isFile) file.length().toString() else "0"
                "$type$permissions  $size  ${file.name}"
            } ?: ""
            CommandResult(0, "$files\n", "", 0)
        } else {
            CommandResult(1, "", "Directory not found: $dir", 0)
        }
    }
    
    private fun clearTerminal(session: TerminalSession, args: List<String>): CommandResult {
        _terminalOutput.value = ""
        return CommandResult(0, "", "", 0)
    }
    
    private fun exitSession(session: TerminalSession, args: List<String>): CommandResult {
        session.isActive = false
        sessions.remove(session.id)
        return CommandResult(0, "Session terminated\n", "", 0)
    }
    
    private fun showHelp(session: TerminalSession, args: List<String>): CommandResult {
        val help = """
        Termux-Ultra Terminal Commands:
        
        Built-in commands:
        cd [dir]        - Change directory
        pwd             - Print working directory  
        ls [dir]        - List files
        clear           - Clear terminal
        exit            - Exit session
        help            - Show this help
        echo [text]     - Echo text
        cat [file]      - Display file contents
        mkdir [dir]     - Create directory
        rm [file]       - Remove file
        cp [src] [dst]  - Copy file
        mv [src] [dst]  - Move file
        ps              - List processes
        env             - Show environment variables
        export [var]    - Export environment variable
        history         - Show command history
        
        External commands are executed via the Bifrost agent system.
        """.trimIndent()
        
        return CommandResult(0, "$help\n", "", 0)
    }
    
    private fun echoCommand(session: TerminalSession, args: List<String>): CommandResult {
        val text = args.joinToString(" ")
        return CommandResult(0, "$text\n", "", 0)
    }
    
    private fun catCommand(session: TerminalSession, args: List<String>): CommandResult {
        val filename = args.firstOrNull() ?: return CommandResult(1, "", "No file specified", 0)
        val file = File(if (filename.startsWith("/")) filename else "${session.workingDirectory}/$filename")
        
        return try {
            if (file.exists() && file.isFile) {
                val content = file.readText()
                CommandResult(0, content, "", 0)
            } else {
                CommandResult(1, "", "File not found: $filename", 0)
            }
        } catch (e: Exception) {
            CommandResult(1, "", "Error reading file: ${e.message}", 0)
        }
    }
    
    private fun makeDirectory(session: TerminalSession, args: List<String>): CommandResult {
        val dirname = args.firstOrNull() ?: return CommandResult(1, "", "No directory specified", 0)
        val dir = File(if (dirname.startsWith("/")) dirname else "${session.workingDirectory}/$dirname")
        
        return if (dir.mkdirs()) {
            CommandResult(0, "", "", 0)
        } else {
            CommandResult(1, "", "Failed to create directory: $dirname", 0)
        }
    }
    
    private fun removeFile(session: TerminalSession, args: List<String>): CommandResult {
        val filename = args.firstOrNull() ?: return CommandResult(1, "", "No file specified", 0)
        val file = File(if (filename.startsWith("/")) filename else "${session.workingDirectory}/$filename")
        
        return if (file.delete()) {
            CommandResult(0, "", "", 0)
        } else {
            CommandResult(1, "", "Failed to remove: $filename", 0)
        }
    }
    
    private fun copyFile(session: TerminalSession, args: List<String>): CommandResult {
        if (args.size < 2) return CommandResult(1, "", "Usage: cp [source] [destination]", 0)
        
        val src = File(if (args[0].startsWith("/")) args[0] else "${session.workingDirectory}/${args[0]}")
        val dst = File(if (args[1].startsWith("/")) args[1] else "${session.workingDirectory}/${args[1]}")
        
        return try {
            src.copyTo(dst, overwrite = true)
            CommandResult(0, "", "", 0)
        } catch (e: Exception) {
            CommandResult(1, "", "Copy failed: ${e.message}", 0)
        }
    }
    
    private fun moveFile(session: TerminalSession, args: List<String>): CommandResult {
        if (args.size < 2) return CommandResult(1, "", "Usage: mv [source] [destination]", 0)
        
        val src = File(if (args[0].startsWith("/")) args[0] else "${session.workingDirectory}/${args[0]}")
        val dst = File(if (args[1].startsWith("/")) args[1] else "${session.workingDirectory}/${args[1]}")
        
        return if (src.renameTo(dst)) {
            CommandResult(0, "", "", 0)
        } else {
            CommandResult(1, "", "Move failed", 0)
        }
    }
    
    private fun listProcesses(session: TerminalSession, args: List<String>): CommandResult {
        // Simplified process listing
        val processes = """
        PID   PPID  CMD
        1     0     init
        100   1     termux-ultra
        """.trimIndent()
        
        return CommandResult(0, "$processes\n", "", 0)
    }
    
    private fun showEnvironment(session: TerminalSession, args: List<String>): CommandResult {
        val env = session.environment.entries.joinToString("\n") { "${it.key}=${it.value}" }
        return CommandResult(0, "$env\n", "", 0)
    }
    
    private fun exportVariable(session: TerminalSession, args: List<String>): CommandResult {
        val arg = args.firstOrNull() ?: return CommandResult(1, "", "Usage: export VAR=value", 0)
        
        val parts = arg.split("=", limit = 2)
        return if (parts.size == 2) {
            session.environment[parts[0]] = parts[1]
            CommandResult(0, "", "", 0)
        } else {
            CommandResult(1, "", "Usage: export VAR=value", 0)
        }
    }
    
    private fun showHistory(session: TerminalSession, args: List<String>): CommandResult {
        val history = session.history.mapIndexed { index, cmd ->
            "${index + 1}  $cmd"
        }.joinToString("\n")
        
        return CommandResult(0, "$history\n", "", 0)
    }
    
    // Utility functions
    private fun parseCommand(command: String): List<String> {
        return command.trim().split("\\s+".toRegex())
    }
    
    private fun getDefaultEnvironment(): Map<String, String> {
        return mapOf(
            "HOME" to "/data/data/com.example.termuxultra/files/home",
            "PATH" to "/data/data/com.example.termuxultra/files/usr/bin:/system/bin",
            "SHELL" to "/data/data/com.example.termuxultra/files/usr/bin/bash",
            "TERM" to "xterm-256color",
            "LANG" to "en_US.UTF-8",
            "PWD" to "/",
            "USER" to "termux",
            "TMPDIR" to "/data/data/com.example.termuxultra/files/usr/tmp"
        )
    }
    
    private fun appendOutput(text: String) {
        _terminalOutput.value += text
    }
    
    private fun Map<String, String>.toJson(): String {
        return entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }
    }
    
    fun getActiveSessions(): List<TerminalSession> {
        return sessions.values.filter { it.isActive }
    }
    
    fun killSession(id: String): Boolean {
        val session = sessions[id]
        return if (session != null) {
            session.isActive = false
            sessions.remove(id)
            if (_currentSession.value?.id == id) {
                _currentSession.value = sessions.values.firstOrNull { it.isActive }
            }
            true
        } else {
            false
        }
    }
}
