package com.example.consumer.features.orchestrator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.example.consumer.IWasmWorker
import com.example.consumer.features.execution.WasmService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IsolatedWorkerManager(private val context: Context) {
    private var wasmWorker: IWasmWorker? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i("IsolatedWorker", "Service Connected")
            wasmWorker = IWasmWorker.Stub.asInterface(service)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w("IsolatedWorker", "Service Disconnected (Crash?)")
            wasmWorker = null
            isBound = false
        }
    }

    fun bind() {
        if (!isBound) {
            val intent = Intent(context, WasmService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbind() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    suspend fun runJob(binary: ByteArray, memoryLimit: Int = 300): String {
        // Wait for connection if needed (simple retry mechanism could be added here)
        if (wasmWorker == null) {
            bind()
            // In a real app, use a proper suspendable connection waiter
            // For now, fail fast so we don't block
             if (wasmWorker == null) return "Error: Worker Service not ready. Try again in 1s."
        }
        
        return try {
            val config = "{ \"memory_limit\": $memoryLimit }"
            wasmWorker?.executeJob(binary, config) ?: "Error: Worker is null"
        } catch (e: Exception) {
            "Error: IPC failed - ${e.message}"
        }
    }
}
