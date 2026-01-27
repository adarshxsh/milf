package com.example.consumer;

import android.os.Bundle;

// Interface for the isolated WASM worker
interface IWasmWorker {
    /**
     * Executes a WASM binary passed via a Bundle.
     * @param args Dictionary containing "shared_memory" (API 27+) or "pfd" (Legacy), "file_size", "config".
     * @return A JSON string containing the execution result (stdout, usage stats).
     */
    String executeJob(in Bundle args);

    /**
     * Forces the job to stop.
     */
    void stopJob();
}
