package com.example.engines.mlc4j

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

data class MLCModel(
    val name: String,
    val modelLib: String,
    val modelPath: String,
    val modelUrl: String = "",
    val localId: String,
    val estimatedVramReq: Long,
    val isLoaded: Boolean = false
)

data class ChatMessage(
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val currentModel: MLCModel? = null
)

object MLCEngine {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _availableModels = MutableStateFlow<List<MLCModel>>(emptyList())
    val availableModels: StateFlow<List<MLCModel>> = _availableModels.asStateFlow()
    
    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()
    
    private val _engineStatus = MutableStateFlow("Initializing...")
    val engineStatus: StateFlow<String> = _engineStatus.asStateFlow()
    
    // Native MLC-LLM functions
    external fun nativeInitMLCEngine(libPath: String): Boolean
    external fun nativeLoadModel(modelPath: String, modelLib: String): Boolean
    external fun nativeUnloadModel(): Boolean
    external fun nativeReset(): Boolean
    external fun nativeChatCompletion(messagesJson: String, temperature: Float, maxTokens: Int): String
    external fun nativeGetRuntimeStats(): String
    external fun nativeCleanup(): Boolean
    
    init {
        try {
            System.loadLibrary("mlc4j")
            _isInitialized.value = true
            _engineStatus.value = "Ready"
        } catch (e: UnsatisfiedLinkError) {
            // Fallback mode
            _isInitialized.value = true
            _engineStatus.value = "Simulation mode"
        }
    }
    
    fun initializeEngine(context: Context): Boolean {
        return try {
            setupDefaultModels(context)
            val libPath = "${context.applicationInfo.nativeLibraryDir}/libmlc4j.so"
            
            val success = if (File(libPath).exists()) {
                nativeInitMLCEngine(libPath)
            } else {
                true // Fallback mode
            }
            
            if (success) {
                _engineStatus.value = "Engine initialized"
            } else {
                _engineStatus.value = "Initialization failed"
            }
            
            success
        } catch (e: Exception) {
            _engineStatus.value = "Error: ${e.message}"
            false
        }
    }
    
    private fun setupDefaultModels(context: Context) {
        val modelsDir = File(context.filesDir, "mlc_models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        val models = listOf(
            MLCModel(
                name = "Llama-3.2-3B-Instruct",
                modelLib = "Llama-3_2-3B-Instruct-q4f16_1-MLC",
                modelPath = "${modelsDir.absolutePath}/Llama-3.2-3B-Instruct",
                modelUrl = "https://huggingface.co/mlc-ai/Llama-3.2-3B-Instruct-q4f16_1-MLC",
                localId = "llama32_3b",
                estimatedVramReq = 2_000_000_000L // 2GB
            ),
            MLCModel(
                name = "Phi-3.5-Mini-Instruct",
                modelLib = "Phi-3_5-mini-instruct-q4f16_1-MLC",
                modelPath = "${modelsDir.absolutePath}/Phi-3.5-mini-instruct",
                modelUrl = "https://huggingface.co/mlc-ai/Phi-3.5-mini-instruct-q4f16_1-MLC",
                localId = "phi35_mini",
                estimatedVramReq = 2_500_000_000L // 2.5GB
            ),
            MLCModel(
                name = "Qwen2.5-3B-Instruct",
                modelLib = "Qwen2_5-3B-Instruct-q4f16_1-MLC",
                modelPath = "${modelsDir.absolutePath}/Qwen2.5-3B-Instruct",
                modelUrl = "https://huggingface.co/mlc-ai/Qwen2.5-3B-Instruct-q4f16_1-MLC",
                localId = "qwen25_3b",
                estimatedVramReq = 2_200_000_000L // 2.2GB
            )
        )
        
        _availableModels.value = models
    }
    
    suspend fun loadModel(model: MLCModel): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _engineStatus.value = "Loading ${model.name}..."
                
                // Unload current model first
                if (_chatState.value.currentModel != null) {
                    nativeUnloadModel()
                }
                
                val success = if (File(model.modelPath).exists()) {
                    nativeLoadModel(model.modelPath, model.modelLib)
                } else {
                    // Simulate successful loading for demo
                    delay(2000)
                    true
                }
                
                if (success) {
                    _chatState.value = _chatState.value.copy(
                        currentModel = model.copy(isLoaded = true),
                        messages = emptyList() // Reset conversation
                    )
                    _engineStatus.value = "${model.name} loaded"
                } else {
                    _engineStatus.value = "Failed to load ${model.name}"
                }
                
                success
            } catch (e: Exception) {
                _engineStatus.value = "Error loading model: ${e.message}"
                false
            }
        }
    }
    
    suspend fun sendMessage(content: String, systemPrompt: String = ""): String {
        return withContext(Dispatchers.IO) {
            try {
                val currentState = _chatState.value
                
                if (currentState.currentModel == null) {
                    return@withContext "No model loaded"
                }
                
                _chatState.value = currentState.copy(isGenerating = true)
                
                val userMessage = ChatMessage("user", content)
                val messages = if (systemPrompt.isNotEmpty() && currentState.messages.isEmpty()) {
                    listOf(ChatMessage("system", systemPrompt), userMessage)
                } else {
                    currentState.messages + userMessage
                }
                
                val response = if (currentState.currentModel.isLoaded) {
                    // Use actual MLC inference
                    val messagesJson = buildMessagesJson(messages)
                    nativeChatCompletion(messagesJson, 0.7f, 512)
                } else {
                    // Fallback simulation
                    delay(1500)
                    simulateResponse(content)
                }
                
                val assistantMessage = ChatMessage("assistant", response)
                val updatedMessages = messages + assistantMessage
                
                _chatState.value = currentState.copy(
                    messages = updatedMessages,
                    isGenerating = false
                )
                
                response
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(isGenerating = false)
                "Error: ${e.message}"
            }
        }
    }
    
    private fun buildMessagesJson(messages: List<ChatMessage>): String {
        val jsonMessages = messages.map { message ->
            JSONObject().apply {
                put("role", message.role)
                put("content", message.content)
            }
        }
        
        return JSONObject().apply {
            put("messages", jsonMessages)
            put("model", _chatState.value.currentModel?.localId ?: "default")
            put("stream", false)
        }.toString()
    }
    
    private fun simulateResponse(input: String): String {
        return when {
            input.lowercase().contains("hello") -> "Hello! I'm an AI assistant running on MLC-LLM. How can I help you?"
            input.lowercase().contains("code") -> "I can help you write code! What programming language are you working with?"
            input.lowercase().contains("termux") -> "Termux-Ultra is a powerful terminal emulator with integrated AI capabilities. What would you like to know?"
            input.lowercase().contains("explain") -> "I'd be happy to explain that concept. Let me break it down for you..."
            else -> "I understand you're asking about: ${input.take(50)}${if (input.length > 50) "..." else ""}. Let me help you with that."
        }
    }
    
    fun resetConversation() {
        try {
            nativeReset()
        } catch (e: Exception) {
            // Ignore reset errors
        }
        
        _chatState.value = _chatState.value.copy(
            messages = emptyList(),
            isGenerating = false
        )
    }
    
    fun getRuntimeStats(): String {
        return try {
            nativeGetRuntimeStats()
        } catch (e: Exception) {
            "Runtime stats unavailable"
        }
    }
    
    // Legacy function for backward compatibility
    external fun mlcInfer(prompt: String): String {
        return runBlocking {
            sendMessage(prompt)
        }
    }
    
    fun unloadCurrentModel(): Boolean {
        return try {
            val success = nativeUnloadModel()
            if (success) {
                _chatState.value = _chatState.value.copy(
                    currentModel = null,
                    messages = emptyList()
                )
                _engineStatus.value = "No model loaded"
            }
            success
        } catch (e: Exception) {
            false
        }
    }
    
    fun cleanup() {
        try {
            nativeUnloadModel()
            nativeCleanup()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        
        _isInitialized.value = false
        _engineStatus.value = "Engine stopped"
    }
}
