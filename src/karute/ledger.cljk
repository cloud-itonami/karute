(ns karute.ledger
  "The append-only audit ledger. `manifest.edn` already routes audit
  events to `did:web:audit.etzhayyim.com` (`emitAuditEvent`) and exposes
  `listAuditEvents`; this namespace is the local, verifiable half of
  that -- the structure those events have before they leave, and the
  reason a later reader can tell whether they were edited afterwards.

  ## Why a chain and not a vector

  An audit log that can be quietly rewritten answers the same way
  whether or not it was rewritten. Each entry therefore carries
  `:prev` (the previous entry's `:digest`) and `:digest` (a digest over
  its own content AND `:prev`), so removing or editing any entry breaks
  every digest after it and `verify` can say WHERE.

  `digest` is a small, dependency-free polynomial rolling hash over the
  entry's canonically printed form (`canonical` sorts every nested map,
  so two equal entries always print identically). It is a TAMPER-EVIDENCE device
  for a local, single-writer log -- NOT a cryptographic commitment. It
  is not collision resistant and an adversary who can write to this log
  can also recompute the chain. Do not cite it as integrity protection
  against such an adversary; the cryptographic form is the signed
  envelope the substrate already provides (ADR-2605181100). What it
  buys is that an accident -- a truncated file, a dropped entry, a
  hand-edited timestamp -- cannot pass silently.

  ## What is recorded

  BOTH dispositions. A ledger that records only what was disclosed
  cannot answer 'was anything refused?', and an actor whose refusals are
  invisible is indistinguishable from an actor that never refuses.")

(def genesis-digest "0000000000")

(def ^:private modulus
  "A prime just under 2^31. Every intermediate in `rolling-hash` stays
  below `modulus * 131 + 65535` ~= 2.8e11, which is exactly
  representable as a double, so the JVM (long) and JS (double) agree
  digit for digit. That is the whole reason this is a plain polynomial
  hash and not FNV-1a: FNV's 16777619 multiply exceeds 2^53 for a
  32-bit accumulator and would silently diverge between the two
  runtimes -- a chain that verifies on one host and not the other."
  2147483647)

(defn- char-code [c]
  #?(:clj (int c) :cljs (.charCodeAt c 0)))

(defn- pad10 [n]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- 10 (count s))) "0")) s)))

(defn- rolling-hash
  "Non-cryptographic polynomial rolling hash, rendered as ten decimal
  digits so the width is stable and two digests are comparable by eye."
  [s]
  (pad10 (reduce (fn [h c] (mod (+ (* h 131) (char-code c)) modulus))
                 2166136261
                 (seq s))))

(defn canonical
  "Recursively sort every map so that two equal values always print the
  same way. Clojure's print order for a map is an implementation detail
  once it grows past an array-map, so hashing `pr-str` of a raw nested
  value would make the digest depend on how the map happened to be
  built."
  [x]
  (cond
    (map? x)        (into (sorted-map) (map (fn [[k v]] [k (canonical v)])) x)
    (set? x)        (vec (sort-by pr-str (map canonical x)))
    (sequential? x) (mapv canonical x)
    :else           x))

(defn entry-digest
  "Digest over an entry's content and its `:prev`. `:digest` itself is
  excluded, or the value would have to contain its own hash."
  [entry]
  (rolling-hash (pr-str (canonical (dissoc entry :digest)))))

(defn append
  "Append `fact` to `ledger`, chaining it to the previous entry.
  Pure: returns the next ledger."
  [ledger fact]
  (let [prev (or (:digest (peek ledger)) genesis-digest)
        e    (assoc fact :seq (count ledger) :prev prev)]
    (conj (vec ledger) (assoc e :digest (entry-digest e)))))

(defn verify
  "Walk the chain. Returns {:ok? true :length n} or
  {:ok? false :length n :broken-at i :reason kw}. `:broken-at` is the
  index of the FIRST entry that does not agree with the chain, which is
  where an edit or a removal happened."
  [ledger]
  (loop [i 0, prev genesis-digest]
    (if (= i (count ledger))
      {:ok? true :length (count ledger)}
      (let [e (nth ledger i)]
        (cond
          (not= (:prev e) prev)
          {:ok? false :length (count ledger) :broken-at i :reason :prev-mismatch}

          (not= (:seq e) i)
          {:ok? false :length (count ledger) :broken-at i :reason :seq-mismatch}

          (not= (:digest e) (entry-digest (dissoc e :digest)))
          {:ok? false :length (count ledger) :broken-at i :reason :digest-mismatch}

          :else (recur (inc i) (:digest e)))))))

(defn holds    [ledger] (filterv #(= :hold (:disposition %)) ledger))
(defn commits  [ledger] (filterv #(= :commit (:disposition %)) ledger))

(defn summary
  "Counts by disposition, plus the chain verdict. `:refused` is reported
  as its own number so that 'this actor never refused anything' is a
  visible fact rather than an absence."
  [ledger]
  {:total      (count ledger)
   :refused    (count (holds ledger))
   :committed  (count (commits ledger))
   :escalated  (count (filterv #(= :escalate (:disposition %)) ledger))
   :chain      (verify ledger)})
