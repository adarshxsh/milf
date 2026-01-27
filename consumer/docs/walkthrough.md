# Android WASM Runtime Architecture

## 1. Problem Statement
We aimed to run arbitrary WebAssembly (WASM) code on Android. This presents two critical challenges:
1.  **Security**: Malicious WASM must not access the host app's data or files.
2.  **Performance & Stability**: WASM binaries can be large, and execution must not crash the main application, especially on constrained devices or Emulators (AVD).

## 2. Evolution of the Architecture

### Phase 1: The "Naive" Advanced Approach (FAILED)
Initially, we attempted a custom "Advanced IPC" layer using raw Linux system calls (`mmap`, `setrlimit`) to forcefully manage memory.

*   **Mechanism**: We tried to manually map large memory regions in the worker process and enforce strict limits.
*   **The Failure**: **Virtual Address Space Fragmentation**.
    *   Modern Android Runtime (ART) reserves huge chunks of virtual memory for its own heap and stack.
    *   Our manual `mmap` calls conflicted with ART's memory layout, leading to crashes (especially on Emulators which have stricter memory layouts).
    *   *Lesson*: Don't fight the Android OS memory manager; cooperation is required.

### Phase 2: The "Implicit Resource Management" Approach (SUCCESS)
We shifted to using Android's native mechanisms (`SharedMemory`, `Binder`, `isolatedProcess`) which are designed to cooperate with ART.

#### A. Isolation Layer (The Sandbox)
Instead of building a custom sandbox, we leverage the OS kernel:
*   **`android:isolatedProcess="true"`**: The WASM service runs in a special PID with **no permissions**. It cannot read files, access the network, or see other apps.
*   **Restricted UID**: Checkable via kernel tools.

#### B. The Hybrid Transport Layer
To solve the "AVD vs Physical Device" transport issue, we implemented a dual-strategy transport:

| Strategy | Mechanism | Why? |
| :--- | :--- | :--- |
| **1. Direct Transport** | **Binder Copy** | **Reliability**. Simple Java `byte[]` copy. Bypasses all Virtual Memory complexity. Used for small files (< 500KB) and guarantees `simple.wasm` works on AVD. |
| **2. Shared Transport** | **SharedMemory (Ashmem)** | **Performance**. Zero-copy memory sharing using File Descriptors. Used for production workloads (Large Models) to avoid Out-Of-Memory errors on the Java Heap. |

#### C. Enforcement Layer
We prevent greedy RAM/CPU usage using a multi-layer approach:
1.  **Memory**: Enforced by WAMR's internal heap limit (controlled via `wasm_runtime_instantiate`).
2.  **CPU**: Enforced by `setrlimit(RLIMIT_CPU)` in the native layer.
3.  **System**: If the system is under pressure, it sends `onTrimMemory` signals (standard Android lifecycle).

## 3. Technical Implementation

### System Diagram
```mermaid
graph TD
    Host[Main App (UI)] -->|Binder IPC (AIDL)| Service[WasmService (Isolated)]
    
    subgraph "Main Process"
        Host -- Direct Copy (<500KB) --> Bundle
        Host -- SharedMemory (>500KB) --> Ashmem[Ashmem Region]
    end
    
    subgraph "Isolated Process (:wasm_worker)"
        Service --> JNI[Native Bridge (C++)]
        JNI --> WAMR[WebAssembly Micro Runtime]
        Ashmem -.->|mmap| JNI
    end
```

### Key Components

#### 1. Orchestrator (`IsolatedWorkerManager.kt`)
The "Brain" that decides the transport strategy.
```kotlin
if (fileSize < 500kb) {
    useDirectTransport(binary) // Safe, Fast enough
} else {
    useSharedMemory(binary)    // Zero-copy, High Perf
}
```

#### 2. Native Bridge (`wamr_bridge.cpp`)
The "Muscle" that interfaces with the lower-level runtime.
*   **Safety**: Uses `dlsym` to dynamically load `ASharedMemory_dupFromJava`, ensuring we don't crash on older Android versions that lack specific symbols.
*   **Output Capture**: Uses `pipe()` and `dup2()` to redirect the WASM `stdout` into a buffer, so we can see "Hello World" logs in the Kotlin app.

## 4. How to Verify
1.  **Test Stability**: Run on Android Emulator. It uses *Direct Transport* and succeeds immediately.
2.  **Test specific output**: Push the generated `hello.wasm` (returns 42).
    *   Command: `adb push hello.wasm /sdcard/Download/`
    *   Result: App reports "Status: Executed Successfully", "Return Code: 42".

### 4. Output Capture
Standard Output (`stdout`) is captured from the WASM runtime via pipe redirection, allowing the app to display print statements from the executed module.
