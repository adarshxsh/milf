#include <android/log.h>
#include <dlfcn.h> // For dlsym
#include <errno.h>
#include <iostream>
#include <jni.h>
#include <string.h> // For memset, strerror
#include <string>
#include <sys/mman.h>
#include <sys/resource.h>
#include <unistd.h>
#include <vector>
#include <wasm_export.h>  // WAMR Public Header
#include <wasm_runtime.h> // WAMR Internal Header for struct definitions

#define LOG_TAG "ConsumerNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool wasm_initialized = false;

// Initialize WAMR only once
void ensure_init() {
  if (wasm_initialized)
    return;

  RuntimeInitArgs init_args;
  memset(&init_args, 0, sizeof(RuntimeInitArgs));

  init_args.mem_alloc_type = Alloc_With_Allocator;
  init_args.mem_alloc_option.allocator.malloc_func = (void *)malloc;
  init_args.mem_alloc_option.allocator.realloc_func = (void *)realloc;
  init_args.mem_alloc_option.allocator.free_func = (void *)free;

  if (!wasm_runtime_full_init(&init_args)) {
    LOGE("Init runtime environment failed.");
    return;
  }
  wasm_initialized = true;
  LOGI("WAMR Runtime Initialized.");
}

// Helper to execute WASM from a pointer (avoids duplication)
std::string execute_wasm_payload(uint8_t *wasm_buffer, uint32_t file_size,
                                 jint memory_limit_mb) {
  // Debug: Print first 8 bytes
  if (file_size >= 8) {
    LOGI("WASM Header: %02X %02X %02X %02X %02X %02X %02X %02X", wasm_buffer[0],
         wasm_buffer[1], wasm_buffer[2], wasm_buffer[3], wasm_buffer[4],
         wasm_buffer[5], wasm_buffer[6], wasm_buffer[7]);
  }

  char error_buf[128] = {0};
  wasm_module_t module =
      wasm_runtime_load(wasm_buffer, file_size, error_buf, sizeof(error_buf));

  if (!module) {
    return "Error: Load failed - " +
           std::string(error_buf[0] ? error_buf : "No error message");
  }

  uint32_t stack_size = 8 * 1024;
  uint32_t heap_size = memory_limit_mb * 1024 * 1024;

  wasm_module_inst_t module_inst = wasm_runtime_instantiate(
      module, stack_size, heap_size, error_buf, sizeof(error_buf));

  if (!module_inst) {
    wasm_runtime_unload(module);
    return "Error: Instantiate failed - " + std::string(error_buf);
  }

  wasm_function_inst_t func = wasm_runtime_lookup_function(module_inst, "main");
  if (!func)
    func = wasm_runtime_lookup_function(module_inst, "run");
  if (!func)
    func = wasm_runtime_lookup_function(module_inst, "_start");

  wasm_exec_env_t exec_env =
      wasm_runtime_create_exec_env(module_inst, stack_size);
  std::string result = "Execution Failed";
  std::string captured_stdout = "";
  int32_t wasm_return_code = -1;
  bool has_return = false;

  // Output Capture Mechanism
  int pipefd[2];
  bool capture_enabled = (pipe(pipefd) != -1);
  int old_stdout = -1, old_stderr = -1;

  if (capture_enabled) {
    old_stdout = dup(STDOUT_FILENO);
    // old_stderr = dup(STDERR_FILENO); // Optional: also capture stderr?

    fflush(stdout);
    // fflush(stderr);

    dup2(pipefd[1], STDOUT_FILENO);
    // dup2(pipefd[1], STDERR_FILENO);
    close(pipefd[1]);
  }

  if (exec_env) {
    if (func) {
      uint32_t argv[1];
      if (wasm_runtime_call_wasm(exec_env, func, 0, argv)) {
        result = "Executed Successfully";
        wasm_return_code = (int32_t)argv[0];
        has_return = true;
      } else {
        const char *exception = wasm_runtime_get_exception(module_inst);
        result = "Execution Failed: " +
                 (exception ? std::string(exception) : "Unknown");
      }
    } else {
      result = "Loaded (No Entry Point)";
    }
  }

  if (capture_enabled) {
    fflush(stdout);
    // fflush(stderr);

    // Restore
    dup2(old_stdout, STDOUT_FILENO);
    // dup2(old_stderr, STDERR_FILENO);
    close(old_stdout);
    // close(old_stderr);

    // Read content
    // Note: This is a blocking read if pipe not closed on write end?
    // dup2 restoration closes the write end copy attached to STDOUT.
    // So pipefd[1] is closed.

    char buffer[1024];
    ssize_t n;
    // We use non-blocking read or just ensure we read until EOF
    // Setting O_NONBLOCK might be safer but for small outputs loop is fine
    while ((n = read(pipefd[0], buffer, sizeof(buffer) - 1)) > 0) {
      buffer[n] = '\0';
      captured_stdout += buffer;
    }
    close(pipefd[0]);
  }

  // Calculate Memory Usage
  uint32_t total_memory_size = 0;
  WASMModuleInstance *wasm_module_inst =
      (WASMModuleInstance *)wasm_runtime_get_module_inst(exec_env);

  if (wasm_module_inst != NULL && wasm_module_inst->memory_count > 0 &&
      wasm_module_inst->memories != NULL &&
      wasm_module_inst->memories[0] != NULL) {
    total_memory_size = wasm_module_inst->memories[0]->num_bytes_per_page *
                        wasm_module_inst->memories[0]->cur_page_count;
  }

  std::string response = "ADVANCED ISOLATED EXECUTION REPORT\n";
  response += "------------------------------------\n";
  response += "Status: " + result + "\n";
  if (has_return)
    response += "WASM Return Code: " + std::to_string(wasm_return_code) + "\n";
  response +=
      "Linear Memory: " + std::to_string(total_memory_size) + " bytes\n";

  wasm_runtime_destroy_exec_env(exec_env);
  wasm_runtime_deinstantiate(module_inst);
  wasm_runtime_unload(module);

  return response;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_consumer_features_execution_WasmJNI_runWasmDirect(
    JNIEnv *env, jobject /* this */, jbyteArray binary, jlong file_size,
    jint memory_limit_mb) {

  ensure_init();

  jbyte *buffer = env->GetByteArrayElements(binary, NULL);
  if (!buffer)
    return env->NewStringUTF("Error: Out of Memory accessing byte[]");

  std::string result = execute_wasm_payload(
      (uint8_t *)buffer, (uint32_t)file_size, memory_limit_mb);

  // Append Transport Info
  result += "\nTransport: Direct Byte Array (Hybrid)\nStrategy: Copy";

  env->ReleaseByteArrayElements(binary, buffer, JNI_ABORT);
  return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_consumer_features_execution_WasmJNI_applyResourceLimits(
    JNIEnv *env, jobject /* this */, jlong memoryLimitBytes,
    jint cpuTimeSeconds) {

  // We have removed RLIMIT_AS because the Android Runtime (ART) reserves
  // significant virtual address space.
  struct rlimit cpu_limit;
  cpu_limit.rlim_cur = (rlim_t)cpuTimeSeconds;
  cpu_limit.rlim_max = (rlim_t)cpuTimeSeconds;

  if (setrlimit(RLIMIT_CPU, &cpu_limit) != 0) {
    LOGE("Failed to set CPU limit: %s", strerror(errno));
    return JNI_FALSE;
  }
  LOGI("Resource limits applied: CPU=%d seconds", cpuTimeSeconds);
  return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_consumer_features_execution_WasmJNI_runWasm(
    JNIEnv *env, jobject /* this */, jobject pfd, jobject shared_mem,
    jlong file_size, jint memory_limit_mb) {

  ensure_init();
  int fd = -1;

  // Strategy A: Use SharedMemory object (API 27+) via NDK
  if (shared_mem != NULL) {
    typedef int (*ASharedMemory_dupFromJava_Func)(JNIEnv *, jobject);
    void *libandroid = dlopen("libandroid.so", RTLD_NOW);
    if (libandroid) {
      ASharedMemory_dupFromJava_Func dupFunc =
          (ASharedMemory_dupFromJava_Func)dlsym(libandroid,
                                                "ASharedMemory_dupFromJava");
      if (dupFunc) {
        fd = dupFunc(env, shared_mem);
        if (fd < 0)
          LOGE("NDK FD extraction failed: %d", fd);
      }
      dlclose(libandroid);
    }
  }

  // Strategy B: Use ParcelFileDescriptor
  if (fd < 0 && pfd != NULL) {
    jclass pfdClass = env->GetObjectClass(pfd);
    jmethodID getFdMethod = env->GetMethodID(pfdClass, "getFd", "()I");
    fd = env->CallIntMethod(pfd, getFdMethod);
  }

  if (fd < 0) {
    return env->NewStringUTF("Error: Transport Failure (No FD)");
  }

  // 2. Mmap the WASM binary (Shared Memory Access)
  void *mapped_ptr =
      mmap(NULL, (size_t)file_size, PROT_READ, MAP_PRIVATE, fd, 0);

  uint8_t *wasm_buffer = NULL;
  bool use_malloc_fallback = false;

  if (mapped_ptr == MAP_FAILED) {
    LOGE("mmap failed: %s (errno: %d). Attempting fallback to malloc/read...",
         strerror(errno), errno);

    // Fallback: Allocate on Heap and Read
    wasm_buffer = (uint8_t *)malloc((size_t)file_size);
    if (!wasm_buffer) {
      LOGE("Fallback malloc failed. Out of Memory.");
      return env->NewStringUTF(
          "Error: Transport Failure (mmap & malloc both failed)");
    }

    // Reset FD position just in case
    lseek(fd, 0, SEEK_SET);

    // Read loop
    size_t total_read = 0;
    ssize_t bytes_read = 0;
    while (total_read < (size_t)file_size) {
      bytes_read =
          read(fd, wasm_buffer + total_read, (size_t)file_size - total_read);
      if (bytes_read <= 0) {
        LOGE("Read failed during fallback: %s", strerror(errno));
        free(wasm_buffer);
        return env->NewStringUTF("Error: Transport Fallback Read Failed");
      }
      total_read += bytes_read;
    }

    use_malloc_fallback = true;
    LOGI("Fallback successful. Loaded %zu bytes via malloc/read.", total_read);

  } else {
    // Shared Memory Success
    LOGI("mmap successful at address: %p", mapped_ptr);
    wasm_buffer = (uint8_t *)mapped_ptr;
  }

  // DEBUG INFO
  if (wasm_buffer && file_size >= 4) {
    LOGI("Native Received Header: %02X %02X %02X %02X", wasm_buffer[0],
         wasm_buffer[1], wasm_buffer[2], wasm_buffer[3]);
  }

  std::string result =
      execute_wasm_payload(wasm_buffer, (uint32_t)file_size, memory_limit_mb);

  if (use_malloc_fallback) {
    result += "\nTransport: Shared Memory (Fallback)\nStrategy: Heap Copy";
    free(wasm_buffer);
  } else {
    result += "\nTransport: Shared Memory (mmap FD)\nStrategy: NDK/Ashmem";
    munmap(mapped_ptr, (size_t)file_size);
  }

  return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_consumer_features_execution_WasmJNI_helloNative(
    JNIEnv *env, jobject /* this */) {
  return env->NewStringUTF("Advanced Native Bridge Operational");
}
