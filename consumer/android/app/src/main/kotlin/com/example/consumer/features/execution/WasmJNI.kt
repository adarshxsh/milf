package com.example.consumer.features.execution

import android.os.ParcelFileDescriptor

object WasmJNI {
    init {
        try {
            System.loadLibrary("consumer_native")
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("Failed to load consumer_native library: ${e.message}")
        }
    }

    external fun helloNative(): String
    
    /**
     * Executes WASM using either a File Descriptor or SharedMemory object.
     */
    external fun runWasm(pfd: ParcelFileDescriptor?, sharedMem: android.os.SharedMemory?, fileSize: Long, memoryLimitMB: Int): String

    /**
     * Executes WASM directly from a Byte Array (Hybrid Transport for small files).
     */
    external fun runWasmDirect(binary: ByteArray, fileSize: Long, memoryLimitMB: Int): String

    /**
     * Applies OS-level resource limits (rlimit) to the current process.
     * This is critical for isolated processes.
     */
    external fun applyResourceLimits(memoryLimitBytes: Long, cpuTimeSeconds: Int): Boolean
}
