(ns karute.phase-test
  "The rollout gate, and the one invariant two layers are supposed to
  agree on independently."
  (:require [clojure.test :refer [deftest is testing]]
            [karute.governor :as gov]
            [karute.phase :as phase]))

(def req {:op :disclosure/second-opinion})
(def clean-verdict {:hard? false :escalate? true :violations []})

(deftest no-disclosure-op-auto-commits-at-any-phase
  (testing "including the last one. This is a structural fact, not a
            milestone still to come."
    (doseq [p (keys phase/phases)
            op gov/disclosure-ops]
      (is (not (contains? (:auto (get phase/phases p)) op))
          (str op " must not be auto-eligible at phase " p)))))

(def all-mutating-ops (into gov/disclosure-ops phase/write-ops))

(defn- never-auto
  "Ops that no phase makes auto-eligible, computed from `phase/phases`."
  []
  (into #{} (remove (fn [op] (some #(contains? (:auto %) op) (vals phase/phases))))
        all-mutating-ops))

(defn- high-stakes-ops
  "Ops the GOVERNOR marks high-stakes, read from its behaviour rather
  than from `gov/disclosure-ops`. Asking the governor what it does keeps
  this a cross-layer check: if the var and the check ever disagree, this
  test follows the check."
  []
  (into #{} (filter #(:high-stakes? (gov/check {:op %} {} {:confidence 1.0} {})))
        all-mutating-ops))

(deftest every-high-stakes-op-is-never-auto-eligible
  (testing "the two layers are compared TO EACH OTHER, not to a literal
            copied into this test -- a copied list would go on passing
            after the real sets drifted apart.

            Note this is containment, not equality: `:consent/grant` and
            `:consent/revoke` are also never auto-eligible without being
            high-stakes disclosures. Changing a patient's consent is the
            patient's act and is not something this actor automates, but
            it moves no PHI, so the governor does not flag it. Asserting
            equality here would have been asserting a coincidence."
    (let [hs (high-stakes-ops)
          na (never-auto)]
      (is (= gov/disclosure-ops hs)
          "the governor's behaviour must match its own declared op set")
      (is (every? na hs)
          (str "high-stakes but auto-eligible somewhere: "
               (vec (sort (remove na hs)))))
      (is (seq hs) "and the set is non-empty, or `every?` proves nothing"))))

(deftest a-governor-hold-survives-every-phase
  (testing "the phase gate can only ever be more restrictive"
    (doseq [p (keys phase/phases)]
      (is (= :hold (:disposition (phase/gate {:hard? true :violations [{:rule :consent-revoked}]}
                                             req p)))))))

(deftest phase-zero-disables-writes
  (is (= {:disposition :hold :reason :phase-disabled}
         (phase/gate clean-verdict {:op :record/write} 0))))

(deftest a-clean-disclosure-escalates-at-the-highest-phase
  (is (= {:disposition :escalate :reason :phase-approval}
         (phase/gate clean-verdict req 3))))

(deftest a-clean-write-commits-at-the-highest-phase
  (testing "the other direction -- a gate that holds everything would
            pass every assertion above"
    (is (= {:disposition :commit :reason nil}
           (phase/gate {:hard? false :escalate? false :violations []}
                       {:op :record/write} 3)))))

(deftest an-unknown-phase-falls-back-to-read-only
  (testing "an unrecognised phase must not be read as permissive"
    (is (= :hold (:disposition (phase/gate clean-verdict {:op :record/write} 99))))))
