package com.example.termuxultra.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

// Enhanced command parsing and execution system
data class ParsedCommand(
    val action: String,
    val target: String? = null,
    val options: Map<String, String> = emptyMap(),
    val isChained: Boolean = false,
    val chainedCommands: List<ParsedCommand> = emptyList()
)

data class CommandContext(
    val currentDirectory: File,
    val history: MutableList<String> = mutableListOf(),
    val variables: MutableMap<String, String> = mutableMapOf(),
    val lastResult: String = ""
)

class AdvancedCommandParser {
    
    fun parseInput(input: String): List<ParsedCommand> {
        val normalized = input.trim()
        
        // Handle chained commands (using && or ;)
        if (normalized.contains("&&") || normalized.contains(";")) {
            return parseChainedCommands(normalized)
        }
        
        // Handle piped commands
        if (normalized.contains("|")) {
            return parsePipedCommands(normalized)
        }
        
        // Single command parsing
        return listOf(parseSingleCommand(normalized))
    }
    
    private fun parseChainedCommands(input: String): List<ParsedCommand> {
        val commands = mutableListOf<ParsedCommand>()
        val parts = input.split(Regex("&&|;")).map { it.trim() }
        
        for (part in parts) {
            commands.add(parseSingleCommand(part))
        }
        
        return commands
    }
    
    private fun parsePipedCommands(input: String): List<ParsedCommand> {
        // For now, treat piped commands as a single complex command
        return listOf(parseSingleCommand(input))
    }
    
    private fun parseSingleCommand(input: String): ParsedCommand {
        val words = tokenize(input)
        if (words.isEmpty()) {
            return ParsedCommand("noop")
        }
        
        val action = words[0].lowercase()
        val options = mutableMapOf<String, String>()
        var target: String? = null
        
        // Parse options and target
        var i = 1
        while (i < words.size) {
            val word = words[i]
            when {
                word.startsWith("-") && i + 1 < words.size -> {
                    options[word] = words[i + 1]
                    i += 2
                }
                word.startsWith("-") -> {
                    options[word] = "true"
                    i++
                }
                target == null -> {
                    target = word
                    i++
                }
                else -> {
                    // Multiple targets - combine them
                    target += " $word"
                    i++
                }
            }
        }
        
        return ParsedCommand(action, target, options)
    }
    
    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '
        
        for (char in input) {
            when {
                char == '"' || char == '\'' -> {
                    if (!inQuotes) {
                        inQuotes = true
                        quoteChar = char
                    } else if (char == quoteChar) {
                        inQuotes = false
                        quoteChar = ' '
                    } else {
                        current.append(char)
                    }
                }
                char == ' ' && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> {
                    current.append(char)
                }
            }
        }
        
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        
        return tokens
    }
    
    fun interpretNaturalLanguage(input: String): ParsedCommand {
        val lowerInput = input.lowercase()
        
        return when {
            // File operations
            lowerInput.matches(Regex(".*list.*(file|dir|folder).*")) -> 
                ParsedCommand("ls", options = mapOf("-la" to "true"))
            
            lowerInput.matches(Regex(".*show.*(content|file).*")) -> {
                val target = extractTarget(input, listOf("content", "file", "show"))
                ParsedCommand("cat", target)
            }
            
            lowerInput.matches(Regex(".*create.*(dir|folder).*")) -> {
                val target = extractTarget(input, listOf("create", "dir", "folder"))
                ParsedCommand("mkdir", target)
            }
            
            lowerInput.matches(Regex(".*delete.*(file|dir|folder).*")) -> {
                val target = extractTarget(input, listOf("delete", "file", "dir", "folder"))
                ParsedCommand("rm", target, mapOf("-rf" to "true"))
            }
            
            // Navigation
            lowerInput.matches(Regex(".*go to.*|.*navigate to.*|.*change.*directory.*")) -> {
                val target = extractTarget(input, listOf("go", "to", "navigate", "change", "directory"))
                ParsedCommand("cd", target)
            }
            
            lowerInput.matches(Regex(".*where am i.*|.*current.*location.*|.*present.*directory.*")) ->
                ParsedCommand("pwd")
            
            lowerInput.matches(Regex(".*go back.*|.*parent.*directory.*")) ->
                ParsedCommand("cd", "..")
            
            // Development operations
            lowerInput.matches(Regex(".*build.*app.*|.*compile.*")) ->
                ParsedCommand("gradle", "assembleDebug")
            
            lowerInput.matches(Regex(".*test.*app.*|.*run.*test.*")) ->
                ParsedCommand("gradle", "test")
            
            lowerInput.matches(Regex(".*clean.*build.*|.*clean.*project.*")) ->
                ParsedCommand("gradle", "clean")
            
            // Git operations
            lowerInput.matches(Regex(".*git.*status.*|.*check.*status.*")) ->
                ParsedCommand("git", "status")
            
            lowerInput.matches(Regex(".*git.*commit.*")) -> {
                val message = extractQuotedText(input) ?: "Auto commit"
                ParsedCommand("git", "commit", mapOf("-m" to message))
            }
            
            // System operations
            lowerInput.matches(Regex(".*system.*info.*|.*device.*info.*")) ->
                ParsedCommand("system_info")
            
            lowerInput.matches(Regex(".*network.*info.*|.*wifi.*info.*")) ->
                ParsedCommand("network_info")
            
            // Help and information
            lowerInput.matches(Regex(".*help.*|.*what.*can.*do.*")) ->
                ParsedCommand("help")
            
            else -> {
                // Try to extract any shell command
                val possibleCommand = extractShellCommand(input)
                if (possibleCommand != null) {
                    parseSingleCommand(possibleCommand)
                } else {
                    ParsedCommand("unknown", input)
                }
            }
        }
    }
    
    private fun extractTarget(input: String, skipWords: List<String>): String? {
        val words = input.split(" ").map { it.trim().lowercase() }
        val filteredWords = words.filterNot { it in skipWords || it.length < 2 }
        return filteredWords.firstOrNull()
    }
    
    private fun extractQuotedText(input: String): String? {
        val pattern = Pattern.compile("\"([^\"]*)\"|'([^']*)'")
        val matcher = pattern.matcher(input)
        return if (matcher.find()) {
            matcher.group(1) ?: matcher.group(2)
        } else null
    }
    
    private fun extractShellCommand(input: String): String? {
        // Look for shell command patterns
        val commandPattern = Pattern.compile("(?:run|execute|do)\\s+(.+)")
        val matcher = commandPattern.matcher(input.lowercase())
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
}