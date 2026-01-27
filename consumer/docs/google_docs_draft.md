# Project: Secure WASM Consumer on Android

## 1. Runtime Environment & The Security Gap
We selected **WAMR (WebAssembly Micro Runtime)** as our execution engine.
*   **Capabilities**: Different isolation/sandboxing modes, instrumentation for health/heartbeat.
*   **The Risk**: By default, it has a security flaw—anyone can make the WASM access the files and folders of the user.

---

## 2. Solution: Advanced Isolation (Three Layer Security)

To solve the security risk, we implemented **Advanced Isolation** consisting of three distinct layers:

### Layer 1: The Sandbox Layer
*   **Mechanism**: Android `spawn` in restricted UID.
*   **Implementation**: We use `android:isolatedProcess="true"`. This ensures the process runs with no permissions, effectively locking it in a secure box managed by the OS.

### Layer 2: The Transport Layer
*   **Mechanism**: Zero-copy shared memory.
*   **The Constraint**: There is a limit in transporting files directly via Binder Transaction (approx 1 MB).
*   **Our Approach**:
    *   **Small Files**: Standard Binder transaction (Array-based, reserved memory for small processes).
    *   **Large Files**: **Shared Memory** over raw `mmap`. The host principle creates a shared memory region (or temporary file in private cache as fallback) to bypass the 1MB limit.

### Layer 3: The Enforcement Layer
*   **Mechanism**: Kernel level limits.
*   **The Logic**: The isolated process doesn’t stop the process from using RAM in a greedy way on its own.
*   **Implementation**: Before the start of any process, we set the limit of RAM usage (e.g., 1GB) using `setrlimit` and WAMR heap configurations.

---

## 3. The "Advanced IPC" Failure: Virtual Address Space Exhaustion

We originally tried a custom "Advanced IPC" using raw `mmap` and `setrlimit`, but it failed.

### Why it failed?
*   **Modern Android Process (ART)**: Android reserves a large amount of memory for its own processes (the Java Heap).
*   **Fragmentation**: Manual `mmap` fragmented the virtual RAM.
*   **The Result**: We could not test in **AVD** (Android Virtual Device) because of memory unavailability. The Isolated process cannot use `android:largeheap` and is treated as Low Priority by the Low Memory Killer (LMK).

### The Fix: Implicit Resource Management
Android Docs suggest "Implicit Resource Management".
*   If the system is under memory pressure, it should send signals (`onTrimMemory`) to the host.
*   **Alignment**: We moved away from raw Linux syscalls that fight ART, and instead use `SharedMemory` which is designed to cooperate with the Android VM. Garbage collectors never get confused about these virtual addresses.

---

## 4. Progress Report

1.  **WAMR Runtime**:
    1.  Selection of Runtime and using third-party GitHub CMake repo.
    2.  Multiple processes can run at the same time.
    3.  Runtime Instrumentation / Isolation.
    4.  Advanced IPC (Three layer checking).

---

## 5. Key Architecture Components

**Orchestrator (`IsolatedWorkerManager.kt`)**
*   Manages the "Three Layer" checks.
*   Decides between Binder Transaction (Small) vs Shared Memory (Large).

**Service (`WasmService.kt`)**
*   The Sandbox Boundary.
*   Runs in the restricted UID.

**Native Bridge (`wamr_bridge.cpp`)**
*   Implements the Runtime logic.
*   Handles Output Capture for verification.
