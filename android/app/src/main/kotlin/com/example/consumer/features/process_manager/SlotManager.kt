package com.example.consumer.features.process_manager

import com.example.consumer.features.wasm_runtime.WasmEngine
import kotlinx.coroutines.*
import java.io.File

class SlotManager(
    private val wasmEngine: WasmEngine = WasmEngine(),
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val filesDir: File? = null
) {

    fun checkAvailableProcesses(): List<String> {
        // TODO: Implement check for available/sleeping processes
        return emptyList()
    }

    // Moved to async execution to prevent UI throttling
    fun startProcessSlot(
        wasmBinary: ByteArray, 
        input: Map<String, Any>, 
        metadata: Map<String, Any>,
        onComplete: (String, String) -> Unit // Callback to return path and content
    ) {
        ioScope.launch {
            try {
                // Security: Enforce strict timeout
                withTimeout(5000L) { // 5 second max execution time
                    val inputBytes = input.toString().toByteArray() 
                    
                    println("Starting WASM execution in background thread...")
                    val resultString = wasmEngine.execute(wasmBinary, inputBytes)
                    
                    // Storing the result in a file for testing purposes
                    val outputFile = File(filesDir, "wasm_output_${System.currentTimeMillis()}.txt")
                    outputFile.writeText(resultString)
                    
                    println("WASM execution completed. Result stored at: ${outputFile.absolutePath}")
                    onComplete(outputFile.absolutePath, resultString)
                }
            }
 catch (e: TimeoutCancellationException) {
                println("Security Alert: WASM Execution timed out. Terminating slot.")
            } catch (e: Exception) {
                println("Error during WASM execution: ${e.message}")
            }
        }
    }

    fun stopProcessSlot(pid: String) {
        // TODO: Implement stopping a process slot
    }
}
