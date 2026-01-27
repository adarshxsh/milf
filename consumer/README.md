# MILF: Consumer Node 🚀
**Mobile Infra for Lambdas and Files**

The **Consumer Node** is the execution edge of the MILF ecosystem, specialized in high-performance, isolated WebAssembly (WASM) execution on Android.

---

## � Security & Architecture
We implement a **Three-Layer Security Model** to safely run untrusted code:
1.  **Sandbox Layer**: `android:isolatedProcess="true"`
    *   WASM runs in a disposable, restricted-UID process with **no permissions** (No Network, No Storage).
2.  **Transport Layer**: **Hybrid Transport Strategy**
    *   **Small Files (<500KB)**: Transferred via Binder (`byte[]`). Guaranteeing stability on Emulators (AVD).
    *   **Large Files (>500KB)**: Transferred via `SharedMemory` (Ashmem). Guaranteeing Zero-Copy performance on physical devices.
3.  **Enforcement Layer**: Kernel-level limits (`setrlimit`) and WAMR Heap limits prevent resource exhaustion.

---

## 🛠 Features

### 1. Robust Execution Engine
*   **Runtime**: Integrated **WebAssembly Micro Runtime (WAMR)** via NDK.
*   **Stability**: Fixes "Virtual Address Space Fragmentation" crashes on AVD by intelligent transport selection.
*   **Performance**: Native C++ bridge (`wamr_bridge.cpp`) capable of running complex AI/Compute workloads.

### 2. Output Verification (Debug Mode)
*   **Stdout Implemented**: The engine captures standard output from the WASM module using pipe redirection.
*   **Verification**: You can verify execution not just by return code, but by the unique text output (e.g., "Hello World").

---

## � Quick Start

### Prerequisites
*   Android SDK / Flutter SDK
*   Ninja Build System (for C++)

### Run the App
```bash
flutter run
```

### Verify E2E
Use the included generator to create a reliable verification binary:
```bash
# 1. Generate hello.wasm (Returns 42, Prints 'Hello World')
python3 generate_wasm.py

# 2. Push to device or you can do it manually 
adb push hello.wasm /sdcard/Download/


# 3. Run in App
# Expected Output:
# Status: Executed Successfully
# Return Code: 42
# Standard Output: Hello World
```

---

## � Documentation
Detailed documentation is available in the `docs/` directory:
*   [**System Overview**](docs/system_overview.md): High-level flow and user journey.
*   [**Architecture Walkthrough**](docs/walkthrough.md): Deep dive into memory management and transport logic.
*   [**Executive Summary**](docs/executive_summary.md): Project status report.

---
*Status: Stable / Feature Complete.*
