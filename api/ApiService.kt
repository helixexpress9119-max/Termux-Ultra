package com.example.api

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*

data class SystemInfo(
    val deviceModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val architecture: String,
    val kernelVersion: String,
    val totalMemory: Long,
    val availableMemory: Long,
    val totalStorage: Long,
    val availableStorage: Long,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkInterfaces: List<NetworkInterfaceInfo>,
    val uptime: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class NetworkInterfaceInfo(
    val name: String,
    val displayName: String,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val addresses: List<String>
)

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val cpu: Float,
    val memory: Long,
    val user: String,
    val command: String
)

data class PackageInfo(
    val name: String,
    val version: String,
    val size: Long,
    val installDate: Long,
    val isSystemApp: Boolean
)

object ApiService {
    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo.asStateFlow()
    
    private val _processes = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val processes: StateFlow<List<ProcessInfo>> = _processes.asStateFlow()
    
    private val _packages = MutableStateFlow<List<PackageInfo>>(emptyList())
    val packages: StateFlow<List<PackageInfo>> = _packages.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun initialize(context: Context) {
        coroutineScope.launch {
            updateSystemInfo(context)
            updateProcessList()
            updatePackageList(context)
        }
    }
    
    // Legacy function for backward compatibility
    fun batteryStatus(): String {
        val systemInfo = _systemInfo.value
        return if (systemInfo != null) {
            JSONObject().apply {
                put("battery", "${systemInfo.batteryLevel}%")
                put("charging", systemInfo.isCharging)
            }.toString()
        } else {
            "{\"battery\":\"Unknown\"}"
        }
    }
    
    suspend fun updateSystemInfo(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val batteryInfo = getBatteryInfo(context)
                val memoryInfo = getMemoryInfo()
                val storageInfo = getStorageInfo()
                val networkInfo = getNetworkInterfaces()
                val kernelVersion = getKernelVersion()
                val uptime = getSystemUptime()
                
                val systemInfo = SystemInfo(
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    androidVersion = Build.VERSION.RELEASE,
                    apiLevel = Build.VERSION.SDK_INT,
                    architecture = System.getProperty("os.arch") ?: "unknown",
                    kernelVersion = kernelVersion,
                    totalMemory = memoryInfo.first,
                    availableMemory = memoryInfo.second,
                    totalStorage = storageInfo.first,
                    availableStorage = storageInfo.second,
                    batteryLevel = batteryInfo.first,
                    isCharging = batteryInfo.second,
                    networkInterfaces = networkInfo,
                    uptime = uptime
                )
                
                _systemInfo.value = systemInfo
            } catch (e: Exception) {
                // Handle error gracefully
            }
        }
    }
    
    private fun getBatteryInfo(context: Context): Pair<Int, Boolean> {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                            status == BatteryManager.BATTERY_STATUS_FULL
            
            val batteryPct = if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                0
            }
            
            Pair(batteryPct, isCharging)
        } else {
            Pair(0, false)
        }
    }
    
    private fun getMemoryInfo(): Pair<Long, Long> {
        return try {
            val memInfo = File("/proc/meminfo").readText()
            val lines = memInfo.split("\n")
            
            var totalMemory = 0L
            var availableMemory = 0L
            
            for (line in lines) {
                when {
                    line.startsWith("MemTotal:") -> {
                        totalMemory = line.split("\\s+".toRegex())[1].toLong() * 1024
                    }
                    line.startsWith("MemAvailable:") -> {
                        availableMemory = line.split("\\s+".toRegex())[1].toLong() * 1024
                    }
                }
            }
            
            Pair(totalMemory, availableMemory)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
    
    private fun getStorageInfo(): Pair<Long, Long> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            Pair(totalBytes, availableBytes)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
    
    private fun getNetworkInterfaces(): List<NetworkInterfaceInfo> {
        return try {
            NetworkInterface.getNetworkInterfaces().toList().map { netInterface ->
                val addresses = netInterface.inetAddresses.toList()
                    .map { it.hostAddress ?: "" }
                    .filter { it.isNotEmpty() }
                
                NetworkInterfaceInfo(
                    name = netInterface.name,
                    displayName = netInterface.displayName ?: netInterface.name,
                    isUp = netInterface.isUp,
                    isLoopback = netInterface.isLoopback,
                    addresses = addresses
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getKernelVersion(): String {
        return try {
            File("/proc/version").readText().trim()
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getSystemUptime(): Long {
        return try {
            val uptimeStr = File("/proc/uptime").readText().trim()
            uptimeStr.split(" ")[0].toDouble().toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    suspend fun updateProcessList() {
        withContext(Dispatchers.IO) {
            try {
                val processes = mutableListOf<ProcessInfo>()
                val procDir = File("/proc")
                
                procDir.listFiles()?.forEach { pidDir ->
                    if (pidDir.isDirectory && pidDir.name.all { it.isDigit() }) {
                        try {
                            val pid = pidDir.name.toInt()
                            val statFile = File(pidDir, "stat")
                            val cmdlineFile = File(pidDir, "cmdline")
                            
                            if (statFile.exists()) {
                                val stat = statFile.readText().trim()
                                val parts = stat.split(" ")
                                
                                if (parts.size > 13) {
                                    val name = parts[1].removeSurrounding("(", ")")
                                    val utime = parts[13].toLongOrNull() ?: 0L
                                    val stime = parts[14].toLongOrNull() ?: 0L
                                    val cpu = (utime + stime) / 100.0f // Simplified CPU calculation
                                    
                                    val cmdline = if (cmdlineFile.exists()) {
                                        cmdlineFile.readText().replace("\u0000", " ").trim()
                                    } else {
                                        name
                                    }
                                    
                                    processes.add(
                                        ProcessInfo(
                                            pid = pid,
                                            name = name,
                                            cpu = cpu,
                                            memory = 0L, // Would need to parse from /proc/pid/status
                                            user = "unknown", // Would need to get from /proc/pid/status
                                            command = cmdline.ifEmpty { name }
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // Skip this process
                        }
                    }
                }
                
                _processes.value = processes.sortedByDescending { it.cpu }
            } catch (e: Exception) {
                // Handle error gracefully
            }
        }
    }
    
    suspend fun updatePackageList(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                val packages = packageManager.getInstalledPackages(0).map { packageInfo ->
                    val applicationInfo = packageInfo.applicationInfo
                    
                    PackageInfo(
                        name = packageInfo.packageName,
                        version = packageInfo.versionName ?: "unknown",
                        size = 0L, // Would need additional API calls to get actual size
                        installDate = packageInfo.firstInstallTime,
                        isSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                
                _packages.value = packages.sortedBy { it.name }
            } catch (e: Exception) {
                // Handle error gracefully
            }
        }
    }
    
    fun getSystemInfoJson(): String {
        val systemInfo = _systemInfo.value
        return if (systemInfo != null) {
            JSONObject().apply {
                put("device_model", systemInfo.deviceModel)
                put("android_version", systemInfo.androidVersion)
                put("api_level", systemInfo.apiLevel)
                put("architecture", systemInfo.architecture)
                put("kernel_version", systemInfo.kernelVersion)
                put("total_memory", systemInfo.totalMemory)
                put("available_memory", systemInfo.availableMemory)
                put("total_storage", systemInfo.totalStorage)
                put("available_storage", systemInfo.availableStorage)
                put("battery_level", systemInfo.batteryLevel)
                put("is_charging", systemInfo.isCharging)
                put("uptime", systemInfo.uptime)
                put("timestamp", systemInfo.timestamp)
                
                val networkArray = org.json.JSONArray()
                systemInfo.networkInterfaces.forEach { netInterface ->
                    val netObj = JSONObject().apply {
                        put("name", netInterface.name)
                        put("display_name", netInterface.displayName)
                        put("is_up", netInterface.isUp)
                        put("is_loopback", netInterface.isLoopback)
                        put("addresses", org.json.JSONArray(netInterface.addresses))
                    }
                    networkArray.put(netObj)
                }
                put("network_interfaces", networkArray)
            }.toString()
        } else {
            "{\"error\":\"System info not available\"}"
        }
    }
    
    fun getProcessListJson(): String {
        val processes = _processes.value
        val jsonArray = org.json.JSONArray()
        
        processes.forEach { process ->
            val processObj = JSONObject().apply {
                put("pid", process.pid)
                put("name", process.name)
                put("cpu", process.cpu)
                put("memory", process.memory)
                put("user", process.user)
                put("command", process.command)
            }
            jsonArray.put(processObj)
        }
        
        return JSONObject().apply {
            put("processes", jsonArray)
            put("count", processes.size)
        }.toString()
    }
    
    fun getPackageListJson(): String {
        val packages = _packages.value
        val jsonArray = org.json.JSONArray()
        
        packages.forEach { pkg ->
            val packageObj = JSONObject().apply {
                put("name", pkg.name)
                put("version", pkg.version)
                put("size", pkg.size)
                put("install_date", pkg.installDate)
                put("is_system_app", pkg.isSystemApp)
            }
            jsonArray.put(packageObj)
        }
        
        return JSONObject().apply {
            put("packages", jsonArray)
            put("count", packages.size)
        }.toString()
    }
    
    suspend fun executeCommand(command: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(command.split(" "))
                    .redirectErrorStream(true)
                    .start()
                
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                
                JSONObject().apply {
                    put("command", command)
                    put("output", output)
                    put("exit_code", exitCode)
                    put("success", exitCode == 0)
                }.toString()
            } catch (e: Exception) {
                JSONObject().apply {
                    put("command", command)
                    put("error", e.message)
                    put("success", false)
                }.toString()
            }
        }
    }
    
    fun cleanup() {
        coroutineScope.cancel()
    }
}
