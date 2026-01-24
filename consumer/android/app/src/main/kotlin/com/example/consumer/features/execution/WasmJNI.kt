package com.example.consumer.features.execution

object WasmJNI {
    init {
        try {
            System.loadLibrary("consumer_native")
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("Failed to load consumer_native library: ${e.message}")
        }
    }

    external fun helloNative(): String
    
    external fun runWasm(binary: ByteArray, memoryLimitMB: Int): String
}
