package com.example.consumer.features.execution

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.consumer.IWasmWorker
import org.json.JSONObject

class WasmService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.i("WasmService", "Isolated WASM Service Created (PID: ${android.os.Process.myPid()})")
        
        // Apply resource limits immediately on creation.
        // We use a very high 4GB limit for RLIMIT_AS on 64-bit systems 
        // to prevent the Android Runtime (ART) from crashing during GC/compaction.
        WasmJNI.applyResourceLimits(4096L * 1024 * 1024, 30)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private val binder = object : IWasmWorker.Stub() {
        override fun executeJob(args: android.os.Bundle): String {
            var pfd: ParcelFileDescriptor? = null
            var sharedMemory: android.os.SharedMemory? = null
            
            return try {
                // Unpack arguments
                args.classLoader = WasmService::class.java.classLoader
                val fileSize = args.getLong("file_size")
                val configJson = args.getString("config") ?: "{}"
                
                pfd = args.getParcelable("pfd")
                sharedMemory = args.getParcelable("shared_memory")
                val blob = args.getByteArray("blob")

                val config = JSONObject(configJson)
                val memLimit = config.optInt("memoryLimitMB", 32)
                
                // Call JNI with appropriate inputs
                val result = if (blob != null) {
                    WasmJNI.runWasmDirect(blob, fileSize, memLimit)
                } else {
                    WasmJNI.runWasm(pfd, sharedMemory, fileSize, memLimit)
                }
                
                JSONObject().apply {
                    put("status", "SUCCESS")
                    put("output", result)
                    put("pid", android.os.Process.myPid())
                }.toString()
            } catch (e: Exception) {
                Log.e("WasmService", "Error executing job", e)
                JSONObject().apply {
                    put("status", "ERROR")
                    put("message", e.message)
                }.toString()
            } finally {
                try { pfd?.close() } catch (e: Exception) {}
                try { sharedMemory?.close() } catch (e: Exception) {}
            }
        }

        override fun stopJob() {
            // In an isolated process, the most effective way to stop a runaway job
            // is often to let the host kill the process, but we can flag for WAMR termination.
            Log.w("WasmService", "Stop job requested - isolated process will exit")
            System.exit(0)
        }
    }
}
