package com.example.consumer.features.orchestrator

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.example.consumer.features.os_stats.OsReader
import com.example.consumer.features.process_manager.SlotManager
import kotlinx.coroutines.launch

class FlutterBridge(
    private val osReader: OsReader,
    private val slotManager: SlotManager,
    private val workerManager: IsolatedWorkerManager
) : MethodChannel.MethodCallHandler {
    
    // Create a CoroutineScope to launch suspend functions
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "reqProcessAllocation" -> {
                // TODO: Delegate to SlotManager
                // val metadata = call.argument<Map<String, Any>>("metadata")
                result.success(null)
            }
            "sendFuncJob" -> {
                // TODO: Delegate to SlotManager/Manager
                 result.success(null)
            }
            "getOSResources" -> {
               val appMemory = osReader.getAppMemory()
               val deviceMem = osReader.getDeviceMemory()
               result.success(mapOf(
                   "cpu" to 0.0, 
                   "mem" to appMemory,
                   "deviceAvail" to deviceMem["avail"],
                   "deviceTotal" to deviceMem["total"],
                   "lowMemory" to (deviceMem["lowMemory"] == 1L)
               ))
            }
            "runWasmTest" -> {
                val wasmBinary = call.argument<ByteArray>("wasmBinary") ?: ByteArray(0)
                val input = call.argument<Map<String, Any>>("input") ?: emptyMap()
                val memoryLimitMB = call.argument<Int>("memoryLimitMB") ?: 300
                
                // Launch coroutine to call suspend function
                scope.launch {
                    val output = workerManager.runJob(wasmBinary, memoryLimitMB)
                    result.success(mapOf(
                        "path" to "isolated_memory",
                        "content" to output,
                        "memDelta" to 0L // Delta tracking is now internal to the service report
                    ))
                }
            }
            else -> result.notImplemented()
        }
    }
}
