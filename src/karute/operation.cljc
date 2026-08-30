(ns karute.operation
  "OperationActor -- one karute request = one supervised actor run.
  The advisor (KaruteAdvisor, an LLM) is sealed into a single stage
  (`:advise`); its proposal is ALWAYS routed through the KaruteGovernor
  (`:govern`) and the rollout phase gate (`:decide`) before anything
  commits, and every disposition -- commit, escalate AND hold -- lands
  in the append-only ledger (`:record`).

  ## Why this is a pure reduction and not a langgraph StateGraph

  The sibling actors in this fleet (`cloud-itonami-commitment-ledger`,
  `cloud-itonami-isic-0111`, ~190 others) express this same five-stage
  shape as a `langgraph.graph` StateGraph with
  `interrupt-before #{:request-approval}`. This actor deliberately does
  not, for one reason: `repository-contracts.edn` declares karute
  standalone, its `deps.edn` has no runtime dependency at all, and the
  langgraph seam in those repos is wired with `:local/root
  \"../../kotoba-lang/langgraph\"`, a path that only resolves inside
  this monorepo checkout. Adding it would trade a property this repo
  currently has -- it builds anywhere -- for a framework it does not yet
  need, since nothing here has an unbounded inner loop or a resumable
  checkpoint.

  The shape is identical and the seam is named: `run` is
  `(reduce stage state stages)`, `:escalate` is the interrupt, and
  `resume` is what a human resuming a checkpoint would call. When this
  actor grows a genuinely resumable run, `run`/`resume` are the two
  functions a StateGraph replaces.

  ## The disposition is not the advisor's to choose

  `advise` may return any `:confidence` it likes and it never sees the
  consent store. The governor reads the store; the phase gate reads the
  governor. An advisor that returns `:confidence 1.0` on a proposal to
  export patient A's chart under patient B's consent gets exactly the
  same HOLD as one that returns 0.0 -- see `karute.governor-test`'s
  `confidence-cannot-buy-a-hard-violation`."
  (:require [karute.governor :as gov]
            [karute.ledger :as ledger]
            [karute.phase :as phase]
            [karute.store :as store]))

(defn advise
  "The sealed advisor stage. A deterministic stand-in for the
  Karute-LLM: it proposes, it does not decide. The real LLM is a swap
  here and nowhere else -- note that it is handed the REQUEST only, not
  the store, so it cannot 'check' consent and cannot be trusted to."
  [request]
  {:proposal {:op         (:op request)
              :subject    (:subject request)
              :confidence (:confidence request 0.9)
              :rationale  (:rationale request "advisor proposal")}})

(defn- commit-effect
  "Apply the state change an approved operation performs. Disclosures
  record themselves in the disclosure log, which is what makes
  `double-disclosure` detectable on the next attempt."
  [st request]
  (cond
    (gov/disclosure-ops (:op request))
    (store/record-disclosure st (:op request) (:subject request)
                             (:recipient-did request) (:capability-uri request))

    (= :record/write (:op request))
    (store/put-record st {:record-id  (:record-id request)
                          :kind       (:kind request)
                          :seq        (:seq request 0)
                          :encrypted-cid (:encrypted-cid request)
                          :public-meta (:public-meta request)})

    (= :consent/revoke (:op request))
    (store/revoke-capability st (:capability-uri request)
                             (:now request) (:reason request))

    :else st))

(defn- fact [request context verdict disposition reason]
  {:t           :decision
   :op          (:op request)
   :actor       (:actor-id context)
   :subject     (:subject request)
   :recipient   (:recipient-did request)
   :capability  (:capability-uri request)
   :disposition disposition
   :reason      reason
   :basis       (mapv :rule (:violations verdict))
   :confidence  (:confidence verdict)
   :at          (:now request)})

(defn run
  "Execute one operation against `state` = {:db .. :ledger ..}.
  Returns the next state plus `:result`.

  A `:hold` and an `:escalate` BOTH leave `:db` untouched. Only an
  approved commit changes it, and `:escalate` leaves the operation
  pending for `resume`."
  [state request context]
  (let [{:keys [proposal]} (advise request)
        verdict  (gov/check request context proposal (:db state))
        ph       (:phase context phase/default-phase)
        {:keys [disposition reason]} (phase/gate verdict request ph)
        entry    (fact request context verdict disposition reason)
        state'   (update state :ledger ledger/append entry)]
    (case disposition
      :commit   (assoc (update state' :db commit-effect request)
                       :result {:disposition :commit :verdict verdict})
      :escalate (assoc state' :result {:disposition :escalate :reason reason
                                       :verdict verdict :pending request})
      (assoc state' :result {:disposition :hold :reason reason :verdict verdict}))))

(defn resume
  "A human's decision on an escalated operation. This is the interrupt
  boundary: the approval NEVER re-opens the governor's verdict, because
  a HARD violation never reaches here -- `run` returns `:hold` for those
  and there is nothing to resume. An approver can only decide about
  proposals the governor already cleared."
  [state request context approval]
  (let [entry (assoc (fact request context {:violations [] :confidence nil}
                           (if (= :approved (:status approval)) :commit :hold)
                           (if (= :approved (:status approval))
                             :human-approved :human-rejected))
                     :approver (:by approval))
        state' (update state :ledger ledger/append entry)]
    (if (= :approved (:status approval))
      (assoc (update state' :db commit-effect request)
             :result {:disposition :commit :reason :human-approved})
      (assoc state' :result {:disposition :hold :reason :human-rejected}))))

(defn initial-state
  ([] (initial-state (store/seed-db)))
  ([db] {:db db :ledger []}))
