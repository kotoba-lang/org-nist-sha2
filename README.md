# kotoba-lang/org-nist-sha2

SHA-224 / SHA-256 and HMAC-SHA-256, with a separate SHA-384 / SHA-512
namespace and hash subpath dispatch.

## Current migration contract

The canonical target is `.kotoba` compiled and run through Amu selfhost /
Kotoba native. CLJK, CLJC, Clojure, JVM/Java, NBB, Node and KBB are historical
port inputs, not final runtime dependencies or required CI gates.

The former workflow ran JDK 17/21 tests/lint and Node/NBB tests. It has been
retired. No native CI gate is registered by this change, and no native
qualification follows from its removal. Native compilation, full corpus
execution, artifact admission and caller integration remain open.

## Canonical core source candidate

`src/sha2/core.kotoba` implements all eight `sha2.core` exports with typed
`:vector-i64` byte inputs/results and no imports or host digest. It is a
source candidate pending native qualification. Its 16-word schedule, virtual
padding and virtual HMAC prefix/message avoid allocating a whole padded or
joined input. All message blocks are processed; resource limits still apply.

See [the complete port scope and evidence](docs/canonical-core-port.md).
The retained `.cljk` file is provenance, not a supported fallback. Publication
of this source does not switch downstream locks or qualify any artifact.

## Complete `sha2.core` surface

| Export | Inputs | Result |
| --- | --- | --- |
| `sha256` | unsigned byte vector | 32 unsigned bytes |
| `sha224` | unsigned byte vector | 28 unsigned bytes |
| `sha256-hex`, `sha224-hex` | unsigned byte vector | lowercase hexadecimal |
| `hmac-sha256` | key byte vector, message byte vector | 32 unsigned bytes |
| `hmac-sha256-hex` | key byte vector, message byte vector | lowercase hexadecimal |
| `hex` | unsigned byte vector | two lowercase hexadecimal digits per byte |
| `u32` | signed 32-bit word | historical unsigned-word conversion |

The port must preserve the full message, distinct SHA-224 initial state,
64 rounds, all padding boundaries, 64-bit bit length, and HMAC with empty,
short, 64-byte and longer keys. A single-block fixture is insufficient.
No host digest or UTF-8 conversion of arbitrary bytes is an implementation
of this API. Native resource budgets must be recorded when qualified.

## Retained source and corpus

`src/sha2/core.cljk` is the historical implementation input. The existing
`test/sha2/sha2_test.cljk` contains published SHA-224/256 vectors and RFC 4231
HMAC cases. `test/sha2/sha2_oracle_test.cljk` retains length 0–199 sweeps,
multiple input shapes including all byte values and larger messages, and
historical MessageDigest / shasum comparisons. These files are preserved;
old hosted comparison results do not establish native execution parity.

The SHA-384/512 implementation and corpus remain under `src/sha2/sha512.cljk`
and `test/sha2/sha512*`. Their full migration, including the 128-byte HMAC
block, distinct SHA-384 initial state and 128-bit length field, is separate
from `sha2.core`. The existing `kotoba/sha512` fixture does not establish
whole-namespace completion.

Hash subpath dispatch (`src/hash/subpath_dispatch.cljk`,
`kotoba/hash/subpath_dispatch.kotoba`) has its own public surface and corpus.
It remains a separate dependency until its full native closure is admitted.

Historical hosted runners and dependency descriptors remain porting records;
they do not define an accepted fallback or a future gate.

## Downstream integration

The original consumer was SHA-256 block checking for `org-tukaani-xz`.
Compiler caches and file hashing also need byte-preserving callers. Source
migration here does not implement raw file capability kinds, their provider
and result lowering, caller conversion, or artifact publication.

## Specifications

- [NIST FIPS 180-4](https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.180-4.pdf)
- [NIST FIPS 198-1](https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.198-1.pdf)
- [RFC 4231](https://www.rfc-editor.org/rfc/rfc4231)

These references define the algorithms; this repository does not claim a
validated implementation or completed native qualification.
