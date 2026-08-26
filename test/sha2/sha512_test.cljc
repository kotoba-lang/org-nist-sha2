(ns sha2.sha512-test
  "Runtime-agnostic SHA-512/384 vectors. The JVM suite adds an oracle sweep
  against `java.security.MessageDigest`; this file is what ClojureScript can
  also run."
  (:require [clojure.test :refer [deftest is testing]]
            [sha2.sha512 :as sha512]))

(defn- utf8 [s]
  #?(:clj (mapv #(bit-and % 0xFF) (.getBytes ^String s "UTF-8"))
     :cljs (vec (array-seq (.encode (js/TextEncoder.) s)))))

;; ── FIPS 180-4 / NIST examples ───────────────────────────────────────────────

(deftest sha512-known-answers
  (is (= (str "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce"
              "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e")
         (sha512/sha512-hex []))
      "the empty string")
  (is (= (str "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
              "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f")
         (sha512/sha512-hex (utf8 "abc"))))
  (is (= (str "8e959b75dae313da8cf4f72814fc143f8f7779c6eb9f7fa17299aeadb6889018"
              "501d289e4900f7e4331b99dec4b5433ac7d329eeb6dd26545e96e55b874be909")
         (sha512/sha512-hex (utf8 (str "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmn"
                                       "hijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu"))))
      "the two-block NIST example"))

(deftest sha384-known-answers
  (is (= (str "38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da"
              "274edebfe76f65fbd51ad2f14898b95b")
         (sha512/sha384-hex []))
      "the empty string")
  (is (= (str "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed"
              "8086072ba1e7cc2358baeca134c825a7")
         (sha512/sha384-hex (utf8 "abc")))))

;; ── the padding boundary ─────────────────────────────────────────────────────

(deftest lengths-across-the-block-boundary
  ;; SHA-512's block is 128 bytes and its length field is SIXTEEN, so the
  ;; last block that still fits its own length ends at 111 rather than at
  ;; SHA-256's 55. An implementation that reused the 32-bit widths is right
  ;; for short inputs and wrong from 112 on -- which is why this sweeps the
  ;; boundary rather than sampling near it.
  (doseq [n [0 1 110 111 112 113 127 128 129 200 239 240 241]]
    (let [d (sha512/sha512 (vec (repeat n 0x61)))]
      (is (= 64 (count d)) (str n " bytes"))
      (is (every? #(<= 0 % 255) d)))))

;; ── HMAC ─────────────────────────────────────────────────────────────────────

(deftest hmac-sha512-rfc-4231
  ;; RFC 4231 §4.2 -- test case 1.
  (is (= (str "87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cde"
              "daa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854")
         (sha512/hmac-sha512-hex (vec (repeat 20 0x0b)) (utf8 "Hi There"))))
  ;; §4.3 -- test case 2, a short key.
  (is (= (str "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea250554"
              "9758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737")
         (sha512/hmac-sha512-hex (utf8 "Jefe") (utf8 "what do ya want for nothing?"))))
  (testing "a key longer than the 128-byte block is hashed first"
    (is (= 128 (count (sha512/hmac-sha512-hex (vec (repeat 200 0xaa)) (utf8 "x"))))))
  (testing "and the block is 128, not 64 -- a 64-byte key must NOT be hashed"
    (let [k (vec (repeat 64 0xaa))]
      (is (not= (sha512/hmac-sha512-hex k (utf8 "x"))
                (sha512/hmac-sha512-hex (sha512/sha512 k) (utf8 "x")))))))
