cmake_minimum_required(VERSION 3.18.1)

project("consumer_native")

# -----------------------------------------------------------------------
# 1. WAMR Configuration
# -----------------------------------------------------------------------
set(WAMR_ROOT "${CMAKE_CURRENT_LIST_DIR}/third_party/wamr")
set(WAMR_BUILD_PLATFORM "android")
set(WAMR_BUILD_TARGET "AARCH64") # Assuming arm64 for now, should be dynamic
set(WAMR_BUILD_INTERP 1)
set(WAMR_BUILD_FAST_INTERP 1)
set(WAMR_BUILD_AOT 1)
set(WAMR_BUILD_LIBC_BUILTIN 1)
set(WAMR_BUILD_MULTI_MODULE 1)

add_definitions(-DBH_MALLOC=wasm_runtime_malloc)
add_definitions(-DBH_FREE=wasm_runtime_free)
add_definitions(-DWASM_ENABLE_INTERP=1)
add_definitions(-DWASM_ENABLE_FAST_INTERP=1)
add_definitions(-DWASM_ENABLE_AOT=1)
add_definitions(-DWASM_ENABLE_LIBC_BUILTIN=1)
add_definitions(-DWASM_ENABLE_MULTI_MODULE=1)
add_definitions(-DWASM_ENABLE_MODULE_INST_CONTEXT=1)
add_definitions(-DBH_PLATFORM_ANDROID=1)
add_definitions(-DBUILD_TARGET="${WAMR_BUILD_TARGET}")

# -----------------------------------------------------------------------
# 2. Source Selection
# -----------------------------------------------------------------------

# Define include directories
set(WAMR_INCLUDE_DIRS
    ${WAMR_ROOT}/core/iwasm/include
    ${WAMR_ROOT}/core/iwasm/common
    ${WAMR_ROOT}/core/iwasm/interpreter
    ${WAMR_ROOT}/core/iwasm/aot
    ${WAMR_ROOT}/core/shared/platform/android
    ${WAMR_ROOT}/core/shared/platform/include
    ${WAMR_ROOT}/core/shared/platform/common/libc-util
    ${WAMR_ROOT}/core/shared/mem-alloc
    ${WAMR_ROOT}/core/shared/utils
    ${WAMR_ROOT}/core/iwasm/common/gc
    ${WAMR_ROOT}/core/iwasm/common/gc/stringref
)

# Apply include directories
include_directories(${WAMR_INCLUDE_DIRS})
include_directories(${CMAKE_CURRENT_LIST_DIR})

# [FIX] Use GLOB instead of GLOB_RECURSE to avoid picking up 'gc' folder which causes errors without flags
file(GLOB WAMR_COMMON_SRC "${WAMR_ROOT}/core/iwasm/common/*.c")
file(GLOB_RECURSE WAMR_ARCH_SRC "${WAMR_ROOT}/core/iwasm/common/arch/*.c") # Pick up arch specific files

file(GLOB WAMR_INTERP_SRC "${WAMR_ROOT}/core/iwasm/interpreter/*.c")
# [FIX] Exclude classic interpreter as we are using Fast Interpreter
list(FILTER WAMR_INTERP_SRC EXCLUDE REGEX "wasm_interp_classic.c")
list(FILTER WAMR_INTERP_SRC EXCLUDE REGEX "wasm_mini_loader.c") # Exclude mini loader unless enabled

file(GLOB WAMR_AOT_SRC "${WAMR_ROOT}/core/iwasm/aot/*.c")

# [FIX] Add AOT architecture-specific relocation (AARCH64 for Android arm64)
set(WAMR_AOT_ARCH_SRC "${WAMR_ROOT}/core/iwasm/aot/arch/aot_reloc_aarch64.c")

file(GLOB WAMR_PLATFORM_SRC "${WAMR_ROOT}/core/shared/platform/android/*.c")
file(GLOB WAMR_PLATFORM_POSIX_SRC "${WAMR_ROOT}/core/shared/platform/common/posix/*.c") # [FIX] Add POSIX implementation (threads, etc)

# [FIX] Add libc-util for errno conversion
file(GLOB WAMR_LIBC_UTIL_SRC "${WAMR_ROOT}/core/shared/platform/common/libc-util/*.c")

# [FIX] Add memory remapping support
set(WAMR_MREMAP_SRC "${WAMR_ROOT}/core/shared/platform/common/memory/mremap.c")

file(GLOB WAMR_UTILS_SRC "${WAMR_ROOT}/core/shared/utils/*.c")
file(GLOB WAMR_MEM_ALLOC_SRC "${WAMR_ROOT}/core/shared/mem-alloc/*.c") # [FIX] Added mem-alloc source

# [FIX] Add EMS GC allocator
file(GLOB WAMR_EMS_GC_SRC "${WAMR_ROOT}/core/shared/mem-alloc/ems/*.c")

file(GLOB WAMR_LIBC_BUILTIN_SRC "${WAMR_ROOT}/core/iwasm/libraries/libc-builtin/*.c") # [FIX] Add libc-builtin

# [FIX] Include Libc WASI via its own CMake script
include("${WAMR_ROOT}/core/iwasm/libraries/libc-wasi/libc_wasi.cmake")

# -----------------------------------------------------------------------
# 3. Build Target
# -----------------------------------------------------------------------
add_library(consumer_native SHARED
    wamr_bridge.cpp
    ${WAMR_COMMON_SRC}
    ${WAMR_ARCH_SRC}
    ${WAMR_INTERP_SRC}
    ${WAMR_AOT_SRC}
    ${WAMR_AOT_ARCH_SRC}
    ${WAMR_PLATFORM_SRC}
    ${WAMR_PLATFORM_POSIX_SRC}
    ${WAMR_LIBC_UTIL_SRC}
    ${WAMR_MREMAP_SRC}
    ${WAMR_UTILS_SRC}
    ${WAMR_MEM_ALLOC_SRC}
    ${WAMR_EMS_GC_SRC}
    ${WAMR_LIBC_BUILTIN_SRC}
    ${LIBC_WASI_SOURCE}
)

find_library(log-lib log)

target_link_libraries(consumer_native
    ${log-lib}
    -llog -ldl -lm # WAMR needs math and dynamic linking
)

target_include_directories(consumer_native PRIVATE ${WAMR_INCLUDE_DIRS})
