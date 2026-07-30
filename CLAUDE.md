# CLAUDE.md — org-nist-sha2

SHA-224/256 and HMAC-SHA-256, portable `.cljc`, zero dependencies.

## Invariants

- **No host hash in `src/`.** `MessageDigest`, `javax.crypto.Mac` and `shasum`
  appear in `test/sha2/sha2_oracle_test.clj` only, as oracles.
- **Digests are pinned by published vectors and by a length sweep against two
  independent implementations.** There is no checksum for the constant table, so
  this is what stands in for one — do not weaken the sweep.
- **Both runtimes are gated** (`clojure -M:test`, `nbb run-tests.cljs`).

## Traps

- **Padding boundaries are where hash code breaks**: at 56 bytes in a block the
  length field no longer fits and a whole extra block is needed. The oracle sweep
  covers every length 0-199 for that reason; a suite that only tests "abc" and a
  long string passes with a wrong boundary.
- **The length field is 64-bit** and is written as two 32-bit halves. Do not
  "simplify" it into a shift — cljs bitwise ops are int32.
- **`bit-not` needs masking.** `(m32 (bit-not e))` — an unmasked complement is
  negative on cljs and wrong on the JVM.
- **HMAC: hash a long key, zero-pad a short one.** Truncating a long key is the
  classic bug and produces a MAC that looks fine until it has to interoperate.
- **The JVM cannot be the oracle for an empty HMAC key** (`SecretKeySpec` throws
  "Empty key") even though FIPS 198-1 allows it. That case is pinned against
  python's `hmac` instead; the comment in the test says so.
- **`(int "a")` is 0 in ClojureScript.** Test helpers use `.charCodeAt`.

## Layout

Single namespace, `sha2.core`: `sha256`, `sha224`, `hmac-sha256`, the `-hex`
variants, and `hex`.

## Next consumer

`org-tukaani-xz` refuses SHA-256-checked `.xz` files. Wiring this in closes that
refusal — the check is a plain SHA-256 over the uncompressed block data.
