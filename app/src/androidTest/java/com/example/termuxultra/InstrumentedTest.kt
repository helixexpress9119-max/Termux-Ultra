package com.example.termuxultra

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import com.example.terminal.TerminalCore
import com.example.api.ApiService

/**
 * Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4::class)
class TerminalCoreTest {
    
    @Test
    fun testTerminalSessionCreation() {
        val sessionId = "test-session"
        val session = TerminalCore.createSession(sessionId)
        
        assertEquals(sessionId, session.id)
        assertTrue(session.isActive)
        assertNotNull(session.workingDirectory)
        assertTrue(session.environment.isNotEmpty())
    }
    
    @Test
    fun testBasicCommands() {
        val session = TerminalCore.createSession("cmd-test")
        
        // Test pwd command
        val result = TerminalCore.runCommand("pwd")
        assertNotNull(result)
        
        // Test echo command
        val echoResult = TerminalCore.runCommand("echo hello")
        assertNotNull(echoResult)
    }
    
    @Test
    fun testSessionManagement() {
        val session1 = TerminalCore.createSession("session1")
        val session2 = TerminalCore.createSession("session2")
        
        assertEquals(2, TerminalCore.getActiveSessions().size)
        
        assertTrue(TerminalCore.killSession("session1"))
        assertEquals(1, TerminalCore.getActiveSessions().size)
        
        assertTrue(TerminalCore.switchSession("session2"))
    }
}

@RunWith(AndroidJUnit4::class)
class ApiServiceTest {
    
    @Test
    fun testBatteryStatus() {
        val batteryStatus = ApiService.batteryStatus()
        assertNotNull(batteryStatus)
        assertTrue(batteryStatus.contains("battery"))
    }
    
    @Test
    fun testSystemInfoUpdate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ApiService.initialize(context)
        ApiService.updateSystemInfo(context)
        
        val systemInfoJson = ApiService.getSystemInfoJson()
        assertNotNull(systemInfoJson)
        assertTrue(systemInfoJson.contains("android_version"))
    }
    
    @Test
    fun testProcessListUpdate() = runBlocking {
        ApiService.updateProcessList()
        
        val processListJson = ApiService.getProcessListJson()
        assertNotNull(processListJson)
        assertTrue(processListJson.contains("processes"))
    }
}