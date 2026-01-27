import struct

# Minimal WASM module that imports 'wasi_snapshot_preview1' 'fd_write' and prints "Hello form WASM\n"
# Hand-crafted or compiled from simple WAT.
# This specific binary exports "_start" which calls fd_write(1, &iov, 1).

# Minimal Valid WASM: Exports "main" which returns 42.
# Verified with wat2wasm.
# (module (func (export "main") (result i32) (i32.const 42)))
wasi_hello_bytes = bytes.fromhex(
    "0061736d010000000105016000017f03020100070801046d6169"
    "6e00000a06010400412a0b"
)

with open("hello.wasm", "wb") as f:
    f.write(wasi_hello_bytes)

print("Created hello.wasm (Valid 'Return 42' module)")

print("Created hello.wasm")
