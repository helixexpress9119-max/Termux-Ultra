package com.example.termuxultra

import com.example.engines.llama.LlamaEngine
import com.example.engines.llama.LlamaModel
import com.example.engines.llama.InferenceRequest
import com.example.engines.mlc4j.MLCEngine
import com.example.engines.mlc4j.MLCModel
import com.example.engines.mlc4j.ChatMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EngineTest {

    @Test
    fun testLlamaEngineInitialization() {
        val context = RuntimeEnvironment.getApplication()
        val result = LlamaEngine.initializeEngine(context)
        
        assertTrue(result)
        assertTrue(LlamaEngine.isInitialized.value)
        assertNotNull(LlamaEngine.availableModels.value)
        assertTrue(LlamaEngine.availableModels.value.isNotEmpty())
    }

    @Test
    fun testLlamaInference() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        LlamaEngine.initializeEngine(context)
        
        val request = InferenceRequest(
            prompt = "Hello, how are you?",
            maxTokens = 100,
            temperature = 0.7f
        )
        
        val result = LlamaEngine.infer(request)
        
        assertNotNull(result)
        assertNotNull(result.text)
        assertTrue(result.text.isNotEmpty())
        assertTrue(result.tokensGenerated > 0)
        assertTrue(result.inferenceTimeMs >= 0)
    }

    @Test
    fun testLlamaModelLoading() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        LlamaEngine.initializeEngine(context)
        
        val models = LlamaEngine.availableModels.value
        if (models.isNotEmpty()) {
            val model = models.first()
            val result = LlamaEngine.loadModel(model)
            
            // Should succeed in simulation mode
            assertTrue(result)
            assertEquals(model.name, LlamaEngine.currentModel.value?.name)
        }
    }

    @Test
    fun testMLCEngineInitialization() {
        val context = RuntimeEnvironment.getApplication()
        val result = MLCEngine.initializeEngine(context)
        
        assertTrue(result)
        assertTrue(MLCEngine.isInitialized.value)
        assertNotNull(MLCEngine.availableModels.value)
        assertTrue(MLCEngine.availableModels.value.isNotEmpty())
    }

    @Test
    fun testMLCChatCompletion() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        MLCEngine.initializeEngine(context)
        
        val response = MLCEngine.sendMessage(
            "What is artificial intelligence?",
            "You are a helpful AI assistant."
        )
        
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
        
        val chatState = MLCEngine.chatState.value
        assertTrue(chatState.messages.isNotEmpty())
        assertTrue(chatState.messages.any { it.role == "user" })
        assertTrue(chatState.messages.any { it.role == "assistant" })
    }

    @Test
    fun testMLCModelLoading() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        MLCEngine.initializeEngine(context)
        
        val models = MLCEngine.availableModels.value
        if (models.isNotEmpty()) {
            val model = models.first()
            val result = MLCEngine.loadModel(model)
            
            // Should succeed in simulation mode
            assertTrue(result)
            assertEquals(model.name, MLCEngine.chatState.value.currentModel?.name)
        }
    }

    @Test
    fun testMLCConversationReset() {
        val context = RuntimeEnvironment.getApplication()
        MLCEngine.initializeEngine(context)
        
        runBlocking {
            MLCEngine.sendMessage("Hello")
            assertTrue(MLCEngine.chatState.value.messages.isNotEmpty())
            
            MLCEngine.resetConversation()
            assertTrue(MLCEngine.chatState.value.messages.isEmpty())
        }
    }
}