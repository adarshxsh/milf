package com.example.consumer.features.execution

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.consumer.IWasmWorker

class WasmService : Service() {

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private val binder = object : IWasmWorker.Stub() {
        override fun executeJob(binary: ByteArray, configJson: String): String {
            // TODO: Parse configJson to get limits
            // TODO: Call WasmJNI.runWasm(binary, limit)
            
            // For now, just return a dummy success from the isolated process
            val nativeMsg = try {
                WasmJNI.runWasm(binary, 30) // Hardcoded 30MB for test, parse configJson in real logic
            } catch (e: Throwable) {
                "Native Error: ${e.message}"
            }
            
            return """
                {
                    "status": "SUCCESS",
                    "output": "$nativeMsg",
                    "pid": ${android.os.Process.myPid()}
                }
            """.trimIndent()
        }

        override fun stopJob() {
            // TODO: Implement cancellation
        }
    }
}
