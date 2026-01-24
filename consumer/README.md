# MILF: Consumer Node 🚀
**Mobile Infra for Lambdas and Files**

The **Consumer Node** is the execution edge of the MILF ecosystem, specialized in high-performance, isolated WebAssembly (WASM) execution on Android.

## 🛠 Progress & Current State

We have successfully established the core foundation for native WASM execution:

### 1. Kotlin & Native Integration (Core Progress)
- **WAMR Runtime Integration**: Successfully integrated the **WebAssembly Micro Runtime (WAMR)** into the Android NDK layer.
- **High-Performance Native Bridge**: Implemented a JNI-based bridge (`wamr_bridge.cpp`) that handles:
    - WASM Module loading and instantiation.
    - Linear memory allocation management.
    - Entry-point discovery (main/run/_start).
    - Detailed execution telemetry (Return codes, Memory usage reports).
- **C-Make Foundation**: Optimized `CMakeLists.txt` for AARCH64 (Android arm64) builds with multi-module and fast-interpreter support.

### 2. Execution Environment
- **Process Isolation**: Execution involves restricted memory heaps and stacks, preventing WASM processes from impacting node stability.
- **Verification Ready**: Built a custom 37-byte `simple.wasm` (returning code `56`) to verify the end-to-end native execution pipeline.

## 📦 Kotlin Strategy
The architecture emphasizes the **Kotlin layer** as the primary orchestrator:
- **`WasmJNI`**: The gateway between Flutter/Dart and the C++ runtime.
- **Resource Guarding**: Kotlin manages the lifecycle of the native engine, ensuring threads are dispatched correctly (`Dispatchers.Default`) and memory limits are strictly enforced.

## 🚀 Native Build Status
- [x] NDK Environment setup.
- [x] WAMR Header/Source resolution.
- [x] JNI Bridge Implementation.
- [x] Functional `simple.wasm` test battery.
- [ ] Advanced IPC Isolation (Next Step).

---
*Status: Native Engine Operational. Proceeding to Cloud Orchestration.*
