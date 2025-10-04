package com.example.termuxultra

import com.example.terminal.TerminalCore
import com.example.terminal.TerminalSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TerminalCoreTest {

    @Before
    fun setUp() {
        // Initialize test environment
    }

    @Test
    fun testCreateSession() {
        val session = TerminalCore.createSession("test-session", "/tmp")
        
        assertEquals("test-session", session.id)
        assertEquals("/tmp", session.workingDirectory)
        assertTrue(session.isActive)
        assertNotNull(session.environment)
    }

    @Test
    fun testSwitchSession() {
        val session1 = TerminalCore.createSession("session1")
        val session2 = TerminalCore.createSession("session2")
        
        assertTrue(TerminalCore.switchSession("session1"))
        assertEquals("session1", TerminalCore.currentSession.value?.id)
        
        assertTrue(TerminalCore.switchSession("session2"))
        assertEquals("session2", TerminalCore.currentSession.value?.id)
        
        assertFalse(TerminalCore.switchSession("nonexistent"))
    }

    @Test
    fun testRunCommand() {
        val session = TerminalCore.createSession("test")
        val result = TerminalCore.runCommand("echo hello")
        
        assertNotNull(result)
        assertTrue(result.contains("Command queued"))
        assertTrue(session.history.contains("echo hello"))
    }

    @Test
    fun testBuiltinCommands() {
        TerminalCore.createSession("test", "/")
        
        // Test pwd command
        TerminalCore.runCommand("pwd")
        // Test help command
        TerminalCore.runCommand("help")
        // Test echo command
        TerminalCore.runCommand("echo test message")
        
        // Verify commands were added to history
        val session = TerminalCore.currentSession.value
        assertNotNull(session)
        assertTrue(session!!.history.contains("pwd"))
        assertTrue(session.history.contains("help"))
        assertTrue(session.history.contains("echo test message"))
    }

    @Test
    fun testKillSession() {
        val session = TerminalCore.createSession("test-kill")
        assertTrue(session.isActive)
        
        assertTrue(TerminalCore.killSession("test-kill"))
        assertFalse(TerminalCore.killSession("nonexistent"))
    }

    @Test
    fun testGetActiveSessions() {
        TerminalCore.createSession("active1")
        TerminalCore.createSession("active2")
        
        val activeSessions = TerminalCore.getActiveSessions()
        assertTrue(activeSessions.size >= 2)
        assertTrue(activeSessions.any { it.id == "active1" })
        assertTrue(activeSessions.any { it.id == "active2" })
    }
}