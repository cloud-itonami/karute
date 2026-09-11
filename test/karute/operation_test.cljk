(ns karute.operation-test
  "End to end: the advisor proposes, the governor decides, the phase
  gate decides again, and the ledger records all three dispositions."
  (:require [clojure.test :refer [deftest is testing]]
            [karute.ledger :as ledger]
            [karute.operation :as op]
            [karute.store :as store]))

(def ctx {:actor-id "dr-a" :phase 3})
(def now "2026-06-01T00:00:00Z")

(def clean
  {:op :disclosure/second-opinion
   :subject "did:plc:patient-a"
   :recipient-did "did:web:clinic-b.example"
   :capability-uri "at://consent/cap-clean"
   :scope #{:soap-note :observation :condition}
   :now now :home-jurisdiction "JPN" :confidence 0.95})

(deftest a-hold-leaves-the-database-untouched
  (testing "a refused disclosure must not appear in the disclosure log,
            or the actor would have recorded doing what it refused to do"
    (let [s0 (op/initial-state)
          s1 (op/run s0 (assoc clean :capability-uri "at://consent/cap-other-patient") ctx)]
      (is (= :hold (get-in s1 [:result :disposition])))
      (is (= (:db s0) (:db s1)))
      (is (= 1 (count (:ledger s1))) "but the refusal itself IS recorded"))))

(deftest an-escalation-leaves-the-database-untouched-too
  (let [s0 (op/initial-state)
        s1 (op/run s0 clean ctx)]
    (is (= :escalate (get-in s1 [:result :disposition])))
    (is (= (:db s0) (:db s1)))))

(deftest approval-commits-and-a-second-attempt-is-refused
  (testing "the double-disclosure guard only works because the commit
            actually wrote to the disclosure log"
    (let [s0 (op/initial-state)
          s1 (op/run s0 clean ctx)
          s2 (op/resume s1 clean ctx {:status :approved :by "dr-a"})
          s3 (op/run s2 clean ctx)]
      (is (= :commit (get-in s2 [:result :disposition])))
      (is (store/disclosure-performed? (:db s2) (:op clean) (:subject clean)
                                       (:recipient-did clean) (:capability-uri clean)))
      (is (= :hold (get-in s3 [:result :disposition])))
      (is (= [:double-disclosure]
             (mapv :rule (get-in s3 [:result :verdict :violations])))))))

(deftest rejection-does-not-commit
  (let [s0 (op/initial-state)
        s1 (op/run s0 clean ctx)
        s2 (op/resume s1 clean ctx {:status :rejected :by "dr-a"})]
    (is (= :hold (get-in s2 [:result :disposition])))
    (is (= (:db s0) (:db s2)))
    (is (= :human-rejected (get-in s2 [:result :reason])))))

(deftest the-advisor-never-sees-the-consent-store
  (testing "`advise` takes the request only. If it could read the store
            it could be argued into 'checking' consent, and the
            independence of the governor would be a naming convention."
    (let [p (:proposal (op/advise clean))]
      (is (= (:op clean) (:op p)))
      (is (= 0.95 (:confidence p))))))

(deftest a-write-outside-the-allowlist-is-refused-end-to-end
  (let [s0 (op/initial-state)
        s1 (op/run s0 {:op :record/write :subject "did:plc:patient-a" :now now
                       :record-id "rec-9" :kind :soap-note :confidence 0.99
                       :public-meta {:patient-did "did:plc:patient-a" :name "山田 太郎"}}
                   ctx)]
    (is (= :hold (get-in s1 [:result :disposition])))
    (is (nil? (store/record (:db s1) "rec-9")) "and nothing was written")))

(deftest a-conforming-write-commits-without-a-human
  (testing "the other direction, so the test above is not passing
            because everything is refused"
    (let [s0 (op/initial-state)
          s1 (op/run s0 {:op :record/write :subject "did:plc:patient-a" :now now
                         :record-id "rec-9" :kind :soap-note :confidence 0.99
                         :public-meta {:patient-did "did:plc:patient-a" :occurred-at now}}
                     ctx)]
      (is (= :commit (get-in s1 [:result :disposition])))
      (is (some? (store/record (:db s1) "rec-9"))))))

(deftest every-disposition-lands-in-the-ledger
  (testing "including the refusals -- an actor whose refusals are
            invisible cannot be told from one that never refuses"
    (let [s0 (op/initial-state)
          s1 (op/run s0 (assoc clean :capability-uri "at://consent/cap-revoked"
                               :scope #{:soap-note}) ctx)
          s2 (op/run s1 clean ctx)
          s3 (op/resume s2 clean ctx {:status :approved :by "dr-a"})
          sm (ledger/summary (:ledger s3))]
      (is (= 3 (:total sm)) "one hold, one escalation, one approval")
      (is (= 1 (:refused sm)))
      (is (= 1 (:escalated sm)))
      (is (= 1 (:committed sm))
          "an escalated run writes ONE entry, not two -- the commit is
           the human's resume, and the escalation is not also a commit")
      (is (:ok? (:chain sm)) "and the chain verifies"))))

(deftest the-ledger-records-the-reason-by-name
  (let [s0 (op/initial-state)
        s1 (op/run s0 (assoc clean :capability-uri "at://consent/cap-revoked"
                             :scope #{:soap-note}) ctx)]
    (is (= [:consent-revoked] (:basis (first (:ledger s1)))))
    (is (= :hold (:disposition (first (:ledger s1)))))
    (is (= "did:web:clinic-b.example" (:recipient (first (:ledger s1)))))))

(deftest phase-zero-refuses-a-write-the-governor-cleared
  (testing "the phase gate is a second, independent reason to say no"
    (let [s1 (op/run (op/initial-state)
                     {:op :record/write :subject "did:plc:patient-a" :now now
                      :record-id "rec-9" :kind :soap-note :confidence 0.99
                      :public-meta {:patient-did "did:plc:patient-a"}}
                     (assoc ctx :phase 0))]
      (is (= :hold (get-in s1 [:result :disposition])))
      (is (= :phase-disabled (get-in s1 [:result :reason])))
      (is (empty? (get-in s1 [:result :verdict :violations]))
          "the governor had no complaint -- the phase did"))))
