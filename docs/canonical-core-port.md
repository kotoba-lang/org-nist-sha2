# Complete `sha2.core` source candidate

## Scope and status

`src/sha2/core.kotoba` contains all eight existing public exports and their
complete local helper closure: 25 functions, 17 private, one eight-i64 state
schema, no imports, no capability calls and no host interop.

| Export | Typed parameters | Typed result |
| --- | --- | --- |
| `hex` | `:vector-i64` | `:string` |
| `hmac-sha256` | `:vector-i64`, `:vector-i64` | `:vector-i64` |
| `hmac-sha256-hex` | `:vector-i64`, `:vector-i64` | `:string` |
| `sha224`, `sha256` | `:vector-i64` | `:vector-i64` |
| `sha224-hex`, `sha256-hex` | `:vector-i64` | `:string` |
| `u32` | `:i64` | `:i64` |

Byte vectors use values 0–255, including NUL and non-UTF-8 values. `hex`
emits two lowercase ASCII digits per byte. `u32` preserves the historical
signed-int32 conversion: add 2^32 to a negative input, otherwise return it.
Out-of-domain hosted numeric/formatting behavior is not part of the portable
unsigned-byte API.

This is source implementation, not a native admission or cryptographic
validation receipt. The owning main has no native gate. No project compiler,
digest implementation or translated copy was executed for this port; no
tests were added or run.

## Implementation shape

- SHA-256 and SHA-224 use distinct eight-word initial values; SHA-224 emits
  seven words. Every working word is masked to 32 bits after sums.
- The complete 64-entry round table is literal data. Rotations and length
  serialization use literal shift counts with Kotoba i64/u64 operations.
- Each block starts with 16 big-endian words. The schedule is a 16-word ring:
  W[t−16], W[t−15], W[t−7], W[t−2] are read before the current slot is replaced.
  All 64 rounds are represented and state addition wraps to 32 bits.
- Padding reads a virtual prefix/message/0x80/zero/64-bit-length stream.
  The outer loop advances by 64 through the entire padded span. There is no
  single-block shortcut or fixed message-size cap in the algorithm source.
- HMAC hashes keys longer than 64 bytes, zero-pads shorter keys (including an
  empty key), and XORs all 64 bytes with 0x36 / 0x5c. Inner and outer digests
  use virtual prefix concatenation, avoiding an oversized joined vector.
- Vector updates use persistent `vector-assoc`. Correctness does not assume
  affine in-place rewriting. Real allocation, arena lifetime and resource
  budgets are unqualified. `hex` also remains subject to string budgets.

Algorithm references are FIPS 180-4 §§4.1.2, 4.2.2, 5.1.1, 5.3.2–3 and
6.2.2, and the HMAC procedure in FIPS 198-1. Links are in the README.

## Source inspection and provenance

The source reader inspected forms without evaluating them:

- All eight export names and public arities match the historical core.
- All 64 round constants and all 16 SHA-224/256 IV words match as integer data.
- Local calls, builtin argument counts, literal shifts, record fields and
  constructors were read. Six loop/recur pairs match their binding counts;
  the maximum loop arity is 10, within the current language limit of 64.
- No imports, hosted digest calls, dynamic shifts, capability calls or stubs
  are used to replace the digest implementation.

Baseline: `944466b05ffbb210e375baa4b9bcd0b6abab06da`.
The following Git blobs remain unchanged:

| Input | Git blob |
| --- | --- |
| `src/sha2/core.cljk` | `9d2efb9af49e880946613ad96ddf99430388c9be` |
| `test/sha2/sha2_test.cljk` | `afc986e296c0d24e53b5ceb0ea63ded2f9ffcb03` |
| `test/sha2/sha2_oracle_test.cljk` | `4966211543826abf1dc66708e650ee87d68eb5a6` |
| `src/sha2/sha512.cljk` | `020fcee350e2d965b25894a978fa92d0b41cfd44` |
| `test/sha2/sha512_test.cljk` | `e3d49ca479ec5a58ae730cc7d5e6c91fa8d2f27d` |
| `test/sha2/sha512_oracle_test.cljk` | `eaf14f1b4a6c2051651ab2c3f42f582b6de9c4ad` |
| `kotoba/hash/subpath_dispatch.kotoba` | `cfa3488186a9668a6628eb4cc4d40ecd541816bd` |
| `kotoba/sha512/core.kotoba` | `8469d0cd3db4af99ba92196b2f8cd76db99b0ca5` |

## Remaining dependency order

1. Admit complete runtime slot implementations and the i64/vector/string/
   record lowering closure on both native ISAs. The current native runtime
   and language-contract source drafts are not activated evidence.
2. Compile and execute the entire core through Amu selfhost / Kotoba native,
   preserving the full published-vector, padding/length, all-byte and HMAC
   corpus. Record exact resource bounds, immutable revisions and artifacts.
3. Admit the complete component and update owning main / immutable downstream
   locks with matching receipts. No lock, CID or self-rebuild result changes
   in this patch.
4. Integrate complete compiler-cache and file-digest callers. File operations
   separately require raw-byte capability kinds, both ISA lowering/encoding,
   typed provider/result handling and the full FS2 request/result path.

SHA-384/512 and subpath-dispatch components remain separate whole-component
work. This core does not qualify them. It also does not complete raw file I/O,
native hashing of artifacts or the full selfhost fixed point.
