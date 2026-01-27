package com.example.consumer.features.orchestrator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.MemoryFile
import android.os.ParcelFileDescriptor
import android.os.SharedMemory
import android.util.Log
import com.example.consumer.IWasmWorker
import com.example.consumer.features.execution.WasmService
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class IsolatedWorkerManager(private val context: Context) : ServiceConnection {
    private var wasmWorker: IWasmWorker? = null
    private var isBound = false
    private var connectionFuture: java.util.concurrent.CompletableFuture<IWasmWorker>? = null

    fun bind() {
        if (!isBound) {
            connectionFuture = java.util.concurrent.CompletableFuture()
            val intent = Intent(context, WasmService::class.java)
            val success = context.bindService(intent, this, Context.BIND_AUTO_CREATE)
            if (success) {
                isBound = true
            } else {
                Log.e("IsolatedWorker", "Binding failed immediately (system rejected)")
                connectionFuture?.completeExceptionally(Exception("System rejected bind"))
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.i("IsolatedWorker", "Service Connected")
        val worker = IWasmWorker.Stub.asInterface(service)
        wasmWorker = worker
        isBound = true // Ensure flag is set
        connectionFuture?.complete(worker)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.w("IsolatedWorker", "Service Disconnected (Crash?)")
        wasmWorker = null
        isBound = false
        connectionFuture = null
    }

    fun unbind() {
        if (isBound) {
            try {
                context.unbindService(this)
            } catch (e: Exception) {
                Log.w("IsolatedWorker", "Error unbinding: ${e.message}")
            }
            isBound = false
            wasmWorker = null
            connectionFuture = null
        }
    }

    suspend fun runJob(binary: ByteArray, memoryLimitMB: Int = 64): String {
        // 1. Ensure Connection with Retries
        for (attempt in 1..3) {
            if (wasmWorker != null && wasmWorker!!.asBinder().isBinderAlive) {
                break
            }

            Log.i("IsolatedWorker", "Binding to service (Attempt $attempt/3)...")
            // Reset state before trying to bind
            unbind() 
            bind()

            try {
                // Wait up to 2 seconds for binding
                val worker = connectionFuture?.get(2, java.util.concurrent.TimeUnit.SECONDS)
                if (worker != null) {
                    wasmWorker = worker
                    break
                }
            } catch (e: Exception) {
                Log.w("IsolatedWorker", "Binding attempt $attempt failed: ${e.message}")
                if (attempt < 3) kotlinx.coroutines.delay(500)
            }
        }

        if (wasmWorker == null) {
            return "Error: Service failed to bind after 3 attempts. Check logs."
        }
        
        var pfd: ParcelFileDescriptor? = null
        var sharedMemory: SharedMemory? = null
        
        try {
            val fileSize = binary.size.toLong()

            if (fileSize < 500 * 1024) {
                // Strategy 1: Direct Transport (Small files < 500KB)
                // Most reliable for AVD and simple tests. Bypasses SharedMemory complexity.
                Log.i("IsolatedWorker", "Binary is small ($fileSize bytes). Using Direct Transport.")
                
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                // Strategy 2: SharedMemory (API 27+)
                sharedMemory = SharedMemory.create("wasm_ipc", binary.size)
                
                val buffer: ByteBuffer = sharedMemory!!.mapReadWrite()
                buffer.put(binary)
                // Unmap BEFORE setting protection to ensure write completes
                SharedMemory.unmap(buffer)

                // Set protection to Read-Only - seals the data for the other process
                sharedMemory!!.setProtect(android.system.OsConstants.PROT_READ)
                
            } else {
                // Legacy Fallback (< API 27): Temporary File
                val tempFile = File.createTempFile("wasm", ".bin", context.cacheDir)
                FileOutputStream(tempFile).use { it.write(binary) }
                pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                tempFile.delete()
            }

            // 2. Prepare Config
            val config = JSONObject().apply {
                put("memoryLimitMB", memoryLimitMB)
            }.toString()

            // 3. Pack into Bundle
            val args = android.os.Bundle().apply {
                putLong("file_size", fileSize)
                putString("config", config)
                
                if (fileSize < 500 * 1024) {
                     putByteArray("blob", binary)
                } else if (sharedMemory != null) {
                    putParcelable("shared_memory", sharedMemory)
                } else if (pfd != null) {
                    putParcelable("pfd", pfd)
                }
            }

            // 4. Call Worker
            return wasmWorker?.executeJob(args) ?: "Error: Worker connection lost"
            
        } catch (e: Exception) {
            Log.e("IsolatedWorker", "IPC Execution Failed", e)
            val msg = e.message ?: "Unknown error"
            if (e is NullPointerException) {
                return "Error: IPC Interrupted (Service Crash?). Try again."
            }
            return "Error: IPC failed - $msg"
        } finally {
            // Do NOT close SharedMemory here - it invalidates the FD before the isolated process reads it.
            // The service side handles cleanup in WasmService.kt finally block.
            try { pfd?.close() } catch (e: Exception) {}
            // try { sharedMemory?.close() } catch (e: Exception) {}
        }
    }
}
