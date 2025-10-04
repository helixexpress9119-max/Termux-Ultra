package com.example.termuxultra

import com.example.terminal.TerminalCore
import com.example.engines.llama.LlamaEngine
import com.example.engines.mlc4j.MLCEngine
import com.example.api.ApiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IntegrationTest {

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        
        // Initialize all components
        LlamaEngine.initializeEngine(context)
        MLCEngine.initializeEngine(context)
    }

    @Test
    fun testFullSystemWorkflow() = runBlocking {
        // 1. Create terminal session
        val session = TerminalCore.createSession("integration-test")
        assertNotNull(session)
        assertTrue(session.isActive)
        
        // 2. Run some terminal commands
        TerminalCore.runCommand("echo 'System integration test'")
        TerminalCore.runCommand("pwd")
        TerminalCore.runCommand("help")
        
        // Verify commands in history
        assertTrue(session.history.size >= 3)
        
        // 3. Test AI inference
        val llamaResponse = LlamaEngine.llamaInfer("Test prompt for integration")
        assertNotNull(llamaResponse)
        assertTrue(llamaResponse.isNotEmpty())
        
        val mlcResponse = MLCEngine.sendMessage("Integration test message")
        assertNotNull(mlcResponse)
        assertTrue(mlcResponse.isNotEmpty())
        
        // 4. Test API service
        val batteryStatus = ApiService.batteryStatus()
        assertNotNull(batteryStatus)
        assertTrue(batteryStatus.contains("battery"))
        
        // 5. Clean up
        TerminalCore.killSession("integration-test")
        assertFalse(TerminalCore.getActiveSessions().any { it.id == "integration-test" })
    }

    @Test
    fun testConcurrentOperations() = runBlocking {
        // Test multiple terminal sessions
        val session1 = TerminalCore.createSession("concurrent-1")
        val session2 = TerminalCore.createSession("concurrent-2")
        
        // Switch between sessions
        assertTrue(TerminalCore.switchSession("concurrent-1"))
        TerminalCore.runCommand("echo 'Session 1'")
        
        assertTrue(TerminalCore.switchSession("concurrent-2"))
        TerminalCore.runCommand("echo 'Session 2'")
        
        // Verify session isolation
        assertTrue(session1.history.contains("echo 'Session 1'"))
        assertTrue(session2.history.contains("echo 'Session 2'"))
        assertFalse(session1.history.contains("echo 'Session 2'"))
        assertFalse(session2.history.contains("echo 'Session 1'"))
        
        // Test concurrent AI operations
        val llamaResult = LlamaEngine.llamaInfer("Concurrent test 1")
        val mlcResult = MLCEngine.sendMessage("Concurrent test 2")
        
        assertNotNull(llamaResult)
        assertNotNull(mlcResult)
        assertTrue(llamaResult.isNotEmpty())
        assertTrue(mlcResult.isNotEmpty())
    }

    @Test
    fun testErrorHandling() = runBlocking {
        // Test invalid terminal commands
        val session = TerminalCore.createSession("error-test")
        TerminalCore.runCommand("invalid_command_that_does_not_exist")
        
        // Should not crash and command should be in history
        assertTrue(session.history.contains("invalid_command_that_does_not_exist"))
        
        // Test session operations on non-existent sessions
        assertFalse(TerminalCore.switchSession("non-existent-session"))
        assertFalse(TerminalCore.killSession("non-existent-session"))
        
        // Test empty/null inputs
        val emptyResult = TerminalCore.runCommand("")
        assertEquals("", emptyResult)
        
        // Test AI with empty prompts
        val emptyLlamaResult = LlamaEngine.llamaInfer("")
        assertNotNull(emptyLlamaResult)
        
        val emptyMLCResult = MLCEngine.sendMessage("")
        assertNotNull(emptyMLCResult)
    }

    @Test
    fun testMemoryAndResourceManagement() {
        // Create and destroy multiple sessions
        val sessionIds = mutableListOf<String>()
        
        for (i in 1..10) {
            val sessionId = "memory-test-$i"
            TerminalCore.createSession(sessionId)
            sessionIds.add(sessionId)
        }
        
        // Verify all sessions exist
        assertEquals(10, sessionIds.size)
        val activeSessions = TerminalCore.getActiveSessions()
        assertTrue(activeSessions.size >= 10)
        
        // Clean up sessions
        sessionIds.forEach { sessionId ->
            assertTrue(TerminalCore.killSession(sessionId))
        }
        
        // Verify cleanup
        val remainingSessions = TerminalCore.getActiveSessions()
        sessionIds.forEach { sessionId ->
            assertFalse(remainingSessions.any { it.id == sessionId })
        }
    }

    @Test
    fun testSystemIntegration() {
        // Test that all major components can work together
        val context = RuntimeEnvironment.getApplication()
        
        // Initialize engines
        assertTrue(LlamaEngine.initializeEngine(context))
        assertTrue(MLCEngine.initializeEngine(context))
        
        // Create terminal session
        val session = TerminalCore.createSession("system-integration")
        assertNotNull(session)
        
        // Test API service
        val apiResult = ApiService.batteryStatus()
        assertNotNull(apiResult)
        
        // Test engine states
        assertTrue(LlamaEngine.isInitialized.value)
        assertTrue(MLCEngine.isInitialized.value)
        
        // Test models availability
        assertTrue(LlamaEngine.availableModels.value.isNotEmpty())
        assertTrue(MLCEngine.availableModels.value.isNotEmpty())
        
        // Cleanup
        TerminalCore.killSession("system-integration")
    }
}