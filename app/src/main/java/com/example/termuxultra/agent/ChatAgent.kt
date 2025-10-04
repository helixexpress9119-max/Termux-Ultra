package com.example.termuxultra.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Import the AI engines
import com.example.engines.llama.LlamaEngine
import com.example.engines.llama.InferenceRequest
import com.example.engines.mlc4j.MLCEngine
import com.example.engines.mlc4j.ChatMessage

class ChatAgent(private val context: Context) {
    private val ctxFile = File(context.filesDir, "ultra_context.json")
    private var cwd: File = context.filesDir
    private var useAI: Boolean = true // Toggle for AI vs simple mapping
    
    // Enhanced parsing and execution
    private val parser = AdvancedCommandParser()
    private val executor = AdvancedCommandExecutor(context)
    private val commandContext = CommandContext(cwd)

    init {
        if (ctxFile.exists()) {
            cwd = File(ctxFile.readText())
            commandContext.currentDirectory = cwd
        }
        
        // Initialize AI engines
        initializeAIEngines()
    }

    private fun initializeAIEngines() {
        try {
            LlamaEngine.initializeEngine(context)
            // MLCEngine would also be initialized here if available
        } catch (e: Exception) {
            useAI = false // Fallback to simple mapping
        }
    }

    suspend fun handleInput(user: String): String = withContext(Dispatchers.IO) {
        if (user.lowercase() == "exit") return@withContext "Goodbye! 👋"

        // Add to command history
        commandContext.history.add(user)
        if (commandContext.history.size > 100) {
            commandContext.history.removeAt(0)
        }

        // Try AI-powered command interpretation first
        val result = if (useAI && LlamaEngine.isInitialized.value) {
            handleWithAI(user)
        } else {
            // Use enhanced parsing instead of simple mapping
            handleWithAdvancedParsing(user)
        }
        
        // Update context
        commandContext.lastResult = result
        updateCurrentDirectory()
        
        return@withContext result
    }

    private suspend fun handleWithAI(userInput: String): String {
        return try {
            val prompt = buildCommandPrompt(userInput)
            
            val request = InferenceRequest(
                prompt = prompt,
                maxTokens = 150,
                temperature = 0.3f, // Lower temperature for more deterministic commands
                topP = 0.9f
            )
            
            val result = LlamaEngine.infer(request)
            val aiResponse = result.text.trim()
            
            // Parse AI response to extract command
            val commands = extractCommandsFromAI(aiResponse)
            
            if (commands.isNotEmpty()) {
                executeCommands(commands)
            } else {
                "🤖 AI Response: $aiResponse"
            }
            
        } catch (e: Exception) {
            "❌ AI Error: ${e.message}\nFalling back to advanced parsing..."
                .also { 
                    useAI = false
                    handleWithAdvancedParsing(userInput)
                }
        }
    }

    private suspend fun handleWithAdvancedParsing(userInput: String): String {
        return try {
            // First try natural language interpretation
            val naturalCommand = parser.interpretNaturalLanguage(userInput)
            
            if (naturalCommand.action != "unknown") {
                executor.executeCommand(naturalCommand, commandContext)
            } else {
                // Fall back to direct command parsing
                val parsedCommands = parser.parseInput(userInput)
                executeCommands(parsedCommands)
            }
        } catch (e: Exception) {
            "❌ Parsing Error: ${e.message}"
        }
    }

    private suspend fun executeCommands(commands: List<ParsedCommand>): String {
        val results = mutableListOf<String>()
        
        for (command in commands) {
            val result = executor.executeCommand(command, commandContext)
            results.add(result)
            
            // If this was a directory change, update our context
            if (command.action == "cd" && result.startsWith("✅")) {
                updateCurrentDirectory()
            }
        }
        
        return results.joinToString("\n\n")
    }

    private fun extractCommandsFromAI(aiResponse: String): List<ParsedCommand> {
        val commands = mutableListOf<ParsedCommand>()
        val lines = aiResponse.lines()
        
        for (line in lines) {
            val cleaned = line.trim()
            
            // Skip empty lines and explanatory text
            if (cleaned.isEmpty() || 
                cleaned.startsWith("#") || 
                cleaned.startsWith("//") ||
                cleaned.contains("explanation", true) ||
                cleaned.contains("response", true)) {
                continue
            }
            
            // Check if it looks like a command
            if (isLikelyCommand(cleaned)) {
                try {
                    val parsed = parser.parseInput(cleaned)
                    commands.addAll(parsed)
                } catch (e: Exception) {
                    // If parsing fails, treat as unknown command
                    commands.add(ParsedCommand("unknown", cleaned))
                }
            }
        }
        
        // If no commands found, try to interpret the whole response as natural language
        if (commands.isEmpty()) {
            val naturalCommand = parser.interpretNaturalLanguage(aiResponse)
            if (naturalCommand.action != "unknown") {
                commands.add(naturalCommand)
            }
        }
        
        return commands
    }

    private fun isLikelyCommand(text: String): Boolean {
        val commonCommands = listOf(
            "ls", "cd", "pwd", "cat", "mkdir", "rm", "cp", "mv", 
            "grep", "find", "chmod", "git", "./gradlew", "gradle"
        )
        
        return commonCommands.any { text.startsWith(it) } ||
               text.startsWith("./") ||
               text.contains("&&") ||
               text.contains("|")
    }

    private fun updateCurrentDirectory() {
        try {
            val contextFile = File(context.filesDir, "ultra_context.json")
            if (contextFile.exists()) {
                val newPath = contextFile.readText()
                val newDir = File(newPath)
                if (newDir.exists() && newDir.isDirectory) {
                    cwd = newDir
                    commandContext.currentDirectory = cwd
                }
            }
        } catch (e: Exception) {
            // Ignore update errors
        }
    }

    private fun buildCommandPrompt(userInput: String): String {
        val currentDir = cwd.name
        val recentHistory = commandContext.history.takeLast(3).joinToString(", ")
        
        return """
You are a terminal assistant. Convert natural language requests to shell commands.

Context:
- Current directory: $currentDir
- Recent commands: $recentHistory
- Last result: ${commandContext.lastResult.take(100)}

User request: "$userInput"

Respond with ONLY the shell command(s), one per line. No explanations.

Examples:
- "list files" → ls -la
- "build the app" → ./gradlew assembleDebug
- "go to documents folder" → cd Documents
- "show file content" → cat filename.txt
- "create test folder" → mkdir test

Commands:""".trimIndent()
    }

    // Public method to toggle AI mode
    fun toggleAIMode(): String {
        useAI = !useAI && LlamaEngine.isInitialized.value
        return if (useAI) {
            "🤖 AI mode enabled - Commands interpreted by LLM"
        } else {
            "🔧 Advanced parsing mode - Enhanced command interpretation"
        }
    }

    // Get command history
    fun getHistory(): String {
        val recentCommands = commandContext.history.takeLast(10)
        return if (recentCommands.isEmpty()) {
            "📜 No command history yet"
        } else {
            buildString {
                appendLine("📜 Recent Command History:")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━")
                recentCommands.forEachIndexed { index, cmd ->
                    appendLine("${index + 1}. $cmd")
                }
            }
        }
    }

    // Clear command history
    fun clearHistory(): String {
        commandContext.history.clear()
        return "🧹 Command history cleared"
    }

    // Get current status with enhanced information
    fun getStatus(): String {
        val aiStatus = if (LlamaEngine.isInitialized.value) "✅ Available" else "❌ Not Available"
        val currentMode = if (useAI) "🤖 AI Mode" else "🔧 Advanced Parsing Mode"
        val currentModel = LlamaEngine.currentModel.value?.name ?: "None"
        val historyCount = commandContext.history.size
        val variables = commandContext.variables.size
        
        return buildString {
            appendLine("📊 Termux-Ultra Chat Agent Status")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📍 Current Directory: ${cwd.absolutePath}")
            appendLine("🧠 AI Engine: $aiStatus")
            appendLine("🎯 Mode: $currentMode")
            appendLine("📱 Model: $currentModel")
            appendLine("📜 Command History: $historyCount commands")
            appendLine("🔧 Variables: $variables set")
            appendLine("💾 Last Result: ${commandContext.lastResult.take(50)}...")
        }
    }

    // Set environment variable
    fun setVariable(name: String, value: String): String {
        commandContext.variables[name] = value
        return "✅ Set variable $name = $value"
    }

    // Get environment variable
    fun getVariable(name: String): String {
        return commandContext.variables[name] ?: "❌ Variable $name not set"
    }

    // List all variables
    fun listVariables(): String {
        return if (commandContext.variables.isEmpty()) {
            "📦 No variables set"
        } else {
            buildString {
                appendLine("📦 Environment Variables:")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━")
                commandContext.variables.forEach { (name, value) ->
                    appendLine("$name = $value")
                }
            }
        }
    }
}