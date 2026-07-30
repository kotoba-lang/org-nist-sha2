# kotoba-lang/org-nist-sha2

Zero-dependency portable `.cljc` **SHA-224 / SHA-256** (NIST FIPS 180-4) and
**HMAC** (FIPS 198-1).

```clojure
(require '[sha2.core :as sha2])

(sha2/sha256 bytes)          ; => 32 unsigned bytes
(sha2/sha256-hex bytes)      ; => lowercase hex
(sha2/sha224 bytes)
(sha2/hmac-sha256 key msg)
(sha2/hmac-sha256-hex key msg)
```

## Why this repo exists

`org-tukaani-xz` **refuses `.xz` files whose block check is SHA-256**, because
the workspace had no portable SHA-256 to verify them with — the choice was
injecting a host hash or refusing, and refusing was the honest option. The
workspace had `sha256d` (Bitcoin's double hash), several
`capability-hash-sha256` *names*, and no library. This is the library.

## How the constants are pinned

FIPS 180-4 publishes no checksum for the 64 round constants, so there is nothing
to verify the table against directly. Instead the *digests* are pinned:

- NIST's own vectors for SHA-256 and SHA-224, and RFC 4231's for HMAC.
- A sweep against `java.security.MessageDigest` over **every length from 0 to
  199** — which crosses the 55/56, 63/64 and 119/120 padding boundaries — plus
  ten input shapes up to a megabyte.
- The same shapes against the `shasum` binary, a completely independent
  implementation.

One wrong constant, one wrong rotation amount or one wrong padding boundary
changes the digest, so the sweep constrains all 64 constants at once.

## Notes

- **Everything stays in the unsigned 32-bit domain.** ClojureScript's bitwise
  operators return signed int32, so every intermediate is normalised.
- **The 64-bit length field is written as two 32-bit halves**, not by shifting: a
  64-bit shift does not exist on ClojureScript, and a message long enough to need
  the high half is exactly the case a shift gets wrong.
- **HMAC hashes an over-long key rather than truncating it**, and zero-pads a
  short one. Those are the two ways HMAC is usually written wrong, and both are
  tested against the JVM.
- **An empty HMAC key is legal** under FIPS 198-1 even though the JVM's
  `SecretKeySpec` refuses one, so that case is pinned against an independent
  implementation instead of the JVM.

## Not implemented

**SHA-384 and SHA-512** are absent. They are the 64-bit variant of the same
construction, which on ClojureScript means every word becomes a `[hi lo]` pair
and all 80 round constants become pairs — a separate piece of work, not a small
extension. Nothing in the workspace needs them today; `.xz`'s SHA-256 check does.
No SHA-3 either: different construction entirely.

## Test

```sh
clojure -M:test      # JVM: vectors + sweeps against MessageDigest and shasum
nbb run-tests.cljs   # ClojureScript: the portable suite
clojure -M:lint
```
