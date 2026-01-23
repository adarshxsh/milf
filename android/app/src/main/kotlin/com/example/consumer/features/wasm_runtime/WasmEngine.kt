package com.example.consumer.features.wasm_runtime

/**
 * Responsible for the actual execution of WebAssembly binaries.
 * Only handles the execution context, not the process/slot lifecycle.
 */
class WasmEngine {

    /**
     * Executes the given WASM binary with the provided input.
     * 
     * @param info: Debug/Meta info about the function
     */
    fun execute(binary: ByteArray, input: ByteArray): String {
        val inputStr = String(input)
        
        // If file is "heavy", allocate 500MB. If "medium", allocate 200MB.
        val targetMB = when {
            inputStr.contains("heavy") -> 500
            inputStr.contains("medium") -> 200
            else -> (250..350).random() 
        }
        println("WASM Slot: Allocating $targetMB MB of simulated heap...")
        
        // Allocate and fill
        val simulatedHeap = ByteArray(targetMB * 1024 * 1024)
        simulatedHeap.fill(1)
        
        // Hold the memory for a while to simulate execution duration
        Thread.sleep(3000L) 
        
        // Log a value from the heap so the compiler doesn't optimize the allocation away
        val checkVal = simulatedHeap[targetMB * 1024]
        
        return """
            [WASM RUNTIME REPORT]
            Target: $inputStr
            Status: EXECUTION_SUCCESS
            
            Resource Usage:
              - Simulated Heap: ${targetMB} MB
              - Dirty Pages Check: $checkVal
            
            Telemetry:
            Timestamp: ${System.currentTimeMillis()}
        """.trimIndent()
        
        // simulatedHeap falls out of scope here and will be garbage collected later
    }
}
