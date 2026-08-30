(ns karute.phase
  "Phase 0->3 staged rollout -- the karute analog of
  `commitledger.phase` (`cloud-itonami-commitment-ledger`) and
  `cerealops.phase` (`cloud-itonami-isic-0111`).

    Phase 0  read-only    -- no writes, still governor-gated.
    Phase 1  assisted     -- clinical record writes allowed, each needs
                             a human approval.
    Phase 2  assisted     -- same write set as phase 1. This domain has
                             no intermediate 'no-PHI-risk' write that
                             phase 2 could unlock: every clinical write
                             lands in a patient's chart.
    Phase 3  supervised   -- a governor-clean, high-confidence
                             `:record/write` may auto-commit. NO
                             `:disclosure/*` op ever auto-commits, at
                             any phase.

  Every member of `karute.governor/disclosure-ops` is deliberately
  ABSENT from every phase's `:auto` set, including phase 3 -- a
  permanent structural fact, not a rollout milestone still to come.
  PHI leaving this actor is always a clinician's call.
  `karute.governor`'s `:high-stakes?` flag enforces the same invariant
  independently: every op it flags is one no phase makes auto-eligible.
  The relation is CONTAINMENT, not equality -- `:consent/grant` and
  `:consent/revoke` are never auto-eligible either, because changing a
  patient's consent is the patient's act, but they move no PHI so the
  governor does not flag them. `karute.phase-test` asserts the two
  layers cannot drift apart by comparing them to each other rather than
  to a literal list copied into the test."
  (:require [karute.governor :as gov]))

(def read-ops  #{:chart/summary :consent/list})
(def write-ops (into #{:consent/grant :consent/revoke} gov/write-ops))

(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed
  to auto-commit when governor-clean>}."
  {0 {:label "read-only"      :writes #{}                     :auto #{}}
   1 {:label "assisted"       :writes #{:record/write}        :auto #{}}
   2 {:label "assisted"       :writes #{:record/write}        :auto #{}}
   3 {:label "supervised-auto"
      :writes (into write-ops gov/disclosure-ops)
      :auto   #{:record/write}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins; the phase can
    only ever be more restrictive, never less).
  - a write/disclosure op not yet enabled in this phase -> HOLD
    (`:phase-disabled`).
  - an enabled op that is not auto-eligible -> ESCALATE
    (`:phase-approval`), even when the governor was clean.
  - every `:disclosure/*` op is non-auto at every phase, so a clean
    disclosure always escalates and never commits by itself."
  [verdict request phase]
  (let [op       (:op request)
        {:keys [writes auto]} (get phases phase (get phases 0))
        mutating? (or (contains? write-ops op) (contains? gov/disclosure-ops op))]
    (cond
      (:hard? verdict)                {:disposition :hold     :reason :governor-hold}
      (and mutating? (not (contains? writes op)))
      {:disposition :hold     :reason :phase-disabled}
      (and mutating? (not (contains? auto op)))
      {:disposition :escalate :reason :phase-approval}
      (:escalate? verdict)            {:disposition :escalate :reason :governor-escalate}
      :else                           {:disposition :commit   :reason nil})))
