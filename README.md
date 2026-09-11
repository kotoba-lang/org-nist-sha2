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

**No SHA-3**: a different construction entirely, sharing nothing with this one.

This section used to say that SHA-384 and SHA-512 were absent. They were, and
then they were added — see below — and the sentence saying they were missing
stayed where it was for the whole time in between. A reader who greps for
"Not implemented" got a confident wrong answer while the section three
headings down documented the functions. Recording it here rather than
deleting it quietly, because the failure is the interesting part: a "what is
missing" list is a claim about the present tense, and nothing makes it get
re-read when the present tense changes.

## `@noble/hashes` subpath oracle (ADR-2608301100)

`kotoba/hash/subpath_dispatch` maps each `@noble/hashes/<subpath>` import to a
first-party target (org-nist-sha2, org-ietf-blake2, crypto, hash, argon2).
Portable reference: `hash.subpath-dispatch`. Parity:
`kbb -M:test -n hash.subpath-dispatch-kotoba-parity-test`.

## Test

```sh
kbb -M:test      # JVM: vectors + sweeps against MessageDigest and shasum
kbb --backend sci run-tests.cljk   # ClojureScript: the portable suite
kbb -M:lint
```

## SHA-512 and SHA-384

```clojure
(require '[sha2.sha512 :as sha512])

(sha512/sha512 bytes)        ; => 64 unsigned bytes
(sha512/sha512-hex bytes)
(sha512/sha384 bytes)        ; => 48
(sha512/hmac-sha512 key message)
(sha512/hmac-sha384 key message)   ; => 48
```

A separate namespace, because SHA-224/256 are 32-bit and SHA-384/512 are
64-bit and neither runtime here has a 64-bit integer both can name: the JVM
does and ClojureScript does not. A word is `[hi lo]`, two unsigned 32-bit
halves, and every operation is a different one from its 32-bit twin.

Added because Ed25519 (RFC 8032) hashes with SHA-512 and this workspace had
SHA-224/256 only. `hmac-sha384` came later, for HKDF-SHA384 — RFC 9180 names
it as a KDF, and `org-ietf-hpke` could not offer that suite without it.

### The constants were derived, not transcribed

FIPS 180-4 defines the initial hash value as the first 64 bits of the
fractional parts of the **square roots of the first eight primes**, the
SHA-384 one as the same for the ninth through sixteenth, and the eighty round
constants as the **cube roots of the first eighty**. All three tables were
computed to 80 significant digits and checked against the published values
before being written into the source, so **no digit passed through anyone's
memory** — the failure mode a table of 88 hand-copied 64-bit words invites.

### Two things that would otherwise be silent

**The length field is sixteen bytes, not eight.** SHA-512 counts message bits
in 128 bits, so the last block that still holds its own length ends at 111
rather than SHA-256's 55. Reusing the 32-bit widths gives a digest that is
right for every input anyone would hash and wrong past 2^61 bytes.

**HMAC's block is 128, not 64.** A 64-byte key must *not* be pre-hashed here,
and one that is produces a MAC that is self-consistent and disagrees with
every other implementation.

**HMAC-SHA-384 hashes an over-long key with SHA-384, not SHA-512.** The two
MACs share a block size, so the substitution changes nothing until a key
exceeds 128 bytes — and then it changes everything, silently. RFC 4231's test
case 6 is that key. Measured: making the substitution turns **exactly one
assertion** red, and it is that one.

Both are checked. The JVM suite sweeps **every length from 0 to 384 bytes**
against `java.security.MessageDigest` for both digests, and HMAC against
`javax.crypto` across eight key lengths and five message lengths, for
**both** `HmacSHA512` and `HmacSHA384` — 1,670 assertions in all.

Measured: changing **one nibble of one round constant** turns 812 assertions
red; narrowing the length field to eight bytes turns 49 red.
