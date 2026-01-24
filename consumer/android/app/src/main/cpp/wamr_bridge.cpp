#include <android/log.h>
#include <jni.h>
#include <string>
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

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_consumer_features_execution_WasmJNI_runWasm(
    JNIEnv *env, jobject /* this */, jbyteArray binary, jint memory_limit_mb) {

  ensure_init();

  // 1. Convert JNI byte array to C buffer
  jsize len = env->GetArrayLength(binary);
  jbyte *buffer = env->GetByteArrayElements(binary, NULL);
  uint8_t *wasm_buffer = (uint8_t *)buffer;

  // 2. Load Module
  char error_buf[128];
  wasm_module_t module =
      wasm_runtime_load(wasm_buffer, len, error_buf, sizeof(error_buf));
  if (!module) {
    LOGE("Load wasm module failed. error: %s", error_buf);
    env->ReleaseByteArrayElements(binary, buffer, JNI_ABORT);
    return env->NewStringUTF("Error: Load failed");
  }

  // 3. Instantiate Module
  uint32_t stack_size = 8 * 1024;
  uint32_t heap_size = memory_limit_mb * 1024 * 1024;

  // [FIX] Initialize WASI args (even empty ones) so modules using WASI don't
  // crash
  const char *dir_list[] = {};
  uint32_t dir_count = 0;
  const char *map_dir_list[] = {};
  uint32_t map_dir_count = 0;
  const char *env_list[] = {};
  uint32_t env_count = 0;
  char *argv_list[] = {}; // Typically argv[0] is the module name
  int argc = 0;

  wasm_runtime_set_wasi_args(module, dir_list, dir_count, map_dir_list,
                             map_dir_count, env_list, env_count, argv_list,
                             argc);

  wasm_module_inst_t module_inst = wasm_runtime_instantiate(
      module, stack_size, heap_size, error_buf, sizeof(error_buf));
  if (!module_inst) {
    LOGE("Instantiate wasm module failed. error: %s", error_buf);
    wasm_runtime_unload(module);
    env->ReleaseByteArrayElements(binary, buffer, JNI_ABORT);
    return env->NewStringUTF("Error: Instantiate failed");
  }

  // 4. Find Entry Function (main, run, or _start)
  wasm_function_inst_t func = wasm_runtime_lookup_function(module_inst, "main");
  if (!func)
    func = wasm_runtime_lookup_function(module_inst, "run");
  if (!func)
    func = wasm_runtime_lookup_function(module_inst, "_start");

  if (func) {
    LOGI("Found entry function. Proceeding to execution.");
  } else {
    LOGI("No explicit entry function (main/run/_start) found.");
  }

  // 5. Execute Job
  wasm_exec_env_t exec_env =
      wasm_runtime_create_exec_env(module_inst, stack_size);
  std::string execution_status = "Instantiated";
  int32_t wasm_return_code = -1;
  bool has_return = false;

  if (exec_env) {
    if (func) {
      // We expect a return value (i32) from our test WASM
      uint32_t argv[1];
      if (wasm_runtime_call_wasm(exec_env, func, 0, argv)) {
        execution_status = "Executed Successfully";
        wasm_return_code = (int32_t)argv[0];
        has_return = true;
      } else {
        const char *exception = wasm_runtime_get_exception(module_inst);
        execution_status =
            "Execution Failed: " +
            (exception ? std::string(exception) : "Unknown Error");
        LOGE("WASM Execution Error: %s", exception ? exception : "Unknown");
      }
    } else {
      execution_status = "Loaded (No Entry Point)";
    }
  } else {
    execution_status = "Failed to create exec env";
  }

  // Calculate Memory Usage
  uint32_t total_memory_size = 0;
  WASMModuleInstance *wasm_module_inst =
      (WASMModuleInstance *)wasm_runtime_get_module_inst(exec_env);

  if (wasm_module_inst != NULL && wasm_module_inst->memory_count > 0 &&
      wasm_module_inst->memories != NULL &&
      wasm_module_inst->memories[0] != NULL) {
    uint32_t num_pages = wasm_module_inst->memories[0]->num_bytes_per_page *
                         wasm_module_inst->memories[0]->cur_page_count;
    total_memory_size = num_pages;
  }

  LOGI("WASM Module Loaded. Memory Configured: %d MB. Current Usage: %d bytes",
       memory_limit_mb, total_memory_size);

  // Build a highly descriptive response for verification
  std::string response = "VERIFIED EXECUTION REPORT\n";
  response += "--------------------------\n";
  response += "Status: " + execution_status + "\n";
  if (has_return) {
    response += "WASM Return Code: " + std::to_string(wasm_return_code) + "\n";
  }
  response +=
      "Linear Memory: " + std::to_string(total_memory_size) + " bytes\n";
  response += "Runtime: WAMR (Native)";

  // Cleanup
  wasm_runtime_destroy_exec_env(exec_env);
  wasm_runtime_deinstantiate(module_inst);
  wasm_runtime_unload(module);
  env->ReleaseByteArrayElements(binary, buffer, JNI_ABORT);

  return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_consumer_features_execution_WasmJNI_helloNative(
    JNIEnv *env, jobject /* this */) {
  return env->NewStringUTF("Native Bridge Operational");
}
