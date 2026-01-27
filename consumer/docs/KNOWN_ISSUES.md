# Known Issue: SharedMemory Transport - "Magic Header Not Detected"

**Status**: 🔴 Open Issue  
**Affects**: Large WASM files (>500KB) using SharedMemory transport  
**Severity**: High - Blocks production use of large models  

---

## Problem Description

When executing WASM binaries larger than 500KB (which triggers the SharedMemory transport path), the native layer fails to load the module with error:

```
Error: Load failed - magic header not detected
```

Small files (<500KB) using Direct Transport work perfectly.

---

## Reproduction Steps

1. Generate a large WASM file:
   ```bash
   python3 generate_large_wasm.py  # Creates ~1.5MB file
   adb push large.wasm /sdcard/Download/
   ```

2. Run the app and select `large.wasm`

3. **Expected**: Status: Executed Successfully, Return Code: 42  
   **Actual**: Error: Load failed - magic header not detected

---

## Environment

- **Device**: Android Emulator (SDK gphone16k arm64)
- **API Level**: 27+ (SharedMemory available)
- **Transport**: SharedMemory (Ashmem)
- **Runtime**: WAMR (WebAssembly Micro Runtime)

---

## What We've Tried

### ✅ Verified Working
- Direct Transport (byte[] via Binder) - Works flawlessly for small files
- Native fallback (malloc + read) - Implementation added but issue persists

### ❌ Attempted Fixes
1. **Buffer Sync**: Tried adding `buffer.force()` (doesn't exist in Android API)
2. **Premature Close**: Removed `sharedMemory?.close()` from orchestrator
3. **Protection Timing**: Ensured `setProtect(PROT_READ)` happens after unmap

### 🔍 Current Investigation
- **Hypothesis**: Race condition where SharedMemory FD gets invalidated before `mmap()` completes in native layer
- **Evidence**: Small files work → Issue is specific to SharedMemory lifecycle
- **Next Steps**: Add hex logging to verify first 4 bytes at each stage

---

## Code Locations

**Key Files**:
- `IsolatedWorkerManager.kt:107-119` - SharedMemory creation and mapping
- `WasmService.kt:65` - SharedMemory cleanup (second close)
- `wamr_bridge.cpp:248-295` - Native mmap with malloc fallback

**Critical Section** (IsolatedWorkerManager.kt):
```kotlin
val buffer: ByteBuffer = sharedMemory!!.mapReadWrite()
buffer.put(binary)  // ← Data write
SharedMemory.unmap(buffer)
sharedMemory!!.setProtect(PROT_READ)  // ← Seal
```

---

## Potential Solutions

### Option A: Delay Service Close
Keep `SharedMemory` open until AFTER native execution completes, not in the `finally` block.

### Option B: Duplicate FD Earlier
Use `dup()` immediately in JNI to create an independent FD that won't be invalidated by parent close.

### Option C: Force Fallback for Large Files
Bypass `mmap` entirely for large files, always use `malloc + read` as fallback.

### Option D: Debug Memory Contents
Add logging in Kotlin and C++ to dump first 4 bytes (`00 61 73 6D`) and verify where corruption occurs.

---

## Community Input Wanted

If you've encountered similar issues with Android SharedMemory + JNI, or have insights on:
- Proper SharedMemory lifecycle management across processes
- Binder transaction vs SharedMemory for large payloads
- WAMR-specific initialization quirks

Please contribute findings or suggestions!

---

## References

- [Android SharedMemory API](https://developer.android.com/reference/android/os/SharedMemory)
- [WAMR Documentation](https://github.com/bytecodealliance/wasm-micro-runtime)
- Project Docs: `docs/kotlin_architecture.md` - Full workflow explanation
