import struct

def encode_unsigned_leb128(n):
    """Encodes an integer to an unsigned LEB128 bytearray."""
    result = bytearray()
    while True:
        byte = n & 0x7f
        n >>= 7
        if n == 0:
            result.append(byte)
            break
        else:
            result.append(byte | 0x80)
    return result

# 1. Base Valid WASM (Return 42)
# (module (func (export "main") (result i32) (i32.const 42)))
base_wasm = bytes.fromhex(
    "0061736d010000000105016000017f03020100070801046d6169"
    "6e00000a06010400412a0b"
)

# 2. Create a "Junk" Custom Section to bloat the file
# Section ID 0 (Custom)
# Payload: Name + Data
custom_name = b"padding"
name_len = encode_unsigned_leb128(len(custom_name))

# Target size: 1.5 MB (well over 500KB threshold)
target_padding = (1024 * 1024 * 1) + (512 * 1024) 
padding_data = b'\x00' * target_padding

payload = name_len + custom_name + padding_data
section_len = encode_unsigned_leb128(len(payload))

# Construct final binary
final_wasm = base_wasm + b'\x00' + section_len + payload

with open("large.wasm", "wb") as f:
    f.write(final_wasm)

print(f"Created large.wasm (Size: {len(final_wasm)} bytes)")
print("This file contains a valid module but is padded to force Shared Memory transport.")
