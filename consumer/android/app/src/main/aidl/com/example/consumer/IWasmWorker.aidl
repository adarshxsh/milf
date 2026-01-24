package com.example.consumer;

// Interface for the isolated WASM worker
interface IWasmWorker {
    /**
     * Executes a WASM binary.
     * @param binary The raw WASM bytes.
     * @param configJson Configuration for the job (memory limits, input string, etc).
     * @return A JSON string containing the execution result (stdout, usage stats).
     */
    String executeJob(in byte[] binary, String configJson);

    /**
     * Forces the job to stop.
     */
    void stopJob();
}
