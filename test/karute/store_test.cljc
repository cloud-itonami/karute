(ns karute.store-test
  (:require [clojure.test :refer [deftest is testing]]
            [karute.store :as store]))

(deftest the-public-meta-allowlist-is-closed
  (testing "a key nobody thought about is refused by default. This is
            the property a blocklist cannot have, and the reason the
            actor's declared PHI policy is enforceable at all."
    (is (= [] (store/public-meta-violations {:patient-did "did:plc:a" :occurred-at "t"})))
    (is (= [:name] (store/public-meta-violations {:patient-did "did:plc:a" :name "山田"})))
    (is (= [:chief-complaint :diagnosis]
           (store/public-meta-violations {:diagnosis "J18.9" :chief-complaint "cough"}))
        "reported sorted, so an operator sees a stable list")
    (is (= [:a-field-invented-in-2027]
           (store/public-meta-violations {:a-field-invented-in-2027 "x"}))
        "the point: a field nobody has heard of is refused, not allowed")))

(deftest public-meta-violations-on-a-non-map-is-empty-not-an-error
  (testing "a write with no public-meta at all has nothing to leak.
            Note this is NOT a pass for a missing map -- the governor
            only consults this for :record/write, and a write with no
            public half writes no public half."
    (is (= [] (store/public-meta-violations nil)))
    (is (= [] (store/public-meta-violations "not a map")))))

(deftest the-disclosure-guard-is-membership-not-status
  (testing "recording a disclosure is what makes the next identical one
            detectable -- never a :status field that any node may set"
    (let [db (store/seed-db)]
      (is (false? (store/disclosure-performed?
                   db :disclosure/second-opinion "did:plc:patient-a"
                   "did:web:clinic-b.example" "at://consent/cap-clean")))
      (let [db' (store/record-disclosure db :disclosure/second-opinion "did:plc:patient-a"
                                         "did:web:clinic-b.example" "at://consent/cap-clean")]
        (is (true? (store/disclosure-performed?
                    db' :disclosure/second-opinion "did:plc:patient-a"
                    "did:web:clinic-b.example" "at://consent/cap-clean")))
        (is (false? (store/disclosure-performed?
                     db' :disclosure/second-opinion "did:plc:patient-a"
                     "did:web:clinic-b.example" "at://consent/cap-clean-2"))
            "a different capability is a different disclosure")
        (is (false? (store/disclosure-performed?
                     db' :disclosure/export-bundle "did:plc:patient-a"
                     "did:web:clinic-b.example" "at://consent/cap-clean"))
            "a different op is a different disclosure")))))

(deftest a-missing-capability-is-nil-not-a-default
  (let [db (store/seed-db)]
    (is (nil? (store/capability db "at://consent/nope")))
    (is (nil? (store/capability db nil)))
    (is (nil? (store/capability db "")))
    (is (some? (store/capability db "at://consent/cap-clean")))))

(deftest revocation-preserves-the-capability
  (testing "an audit that cannot see a capability that once existed
            cannot explain a disclosure made under it"
    (let [db  (store/revoke-capability (store/seed-db) "at://consent/cap-clean"
                                       "2026-06-01T00:00:00Z" "patient withdrew")
          cap (store/capability db "at://consent/cap-clean")]
      (is (some? cap))
      (is (= :revoked (:status cap)))
      (is (= "patient withdrew" (:revocation-reason cap))))))

(deftest revoking-an-unknown-capability-changes-nothing
  (let [db (store/seed-db)]
    (is (= db (store/revoke-capability db "at://consent/nope" "t" "r")))))

(deftest records-are-indexed-by-patient
  (let [db (store/seed-db)]
    (is (= 3 (count (store/records-for-patient db "did:plc:patient-a"))))
    (is (= [] (store/records-for-patient db "did:plc:patient-b")))
    (is (= #{:soap-note :observation :medication-request}
           (store/record-kinds-for-patient db "did:plc:patient-a")))))

(deftest every-seeded-record-has-a-conforming-public-meta
  (testing "the fixture must not itself violate the rule the governor
            enforces, or every test built on it starts from a leak"
    (doseq [r (vals (:records (store/seed-db)))]
      (is (= [] (store/public-meta-violations (:public-meta r)))
          (str (:record-id r) " のpublic-meta が allowlist の外に出ている")))))

(deftest every-seeded-capability-scope-names-real-kinds
  (testing "a fixture scope naming a kind that does not exist would make
            :scope-unknown-kind fire in cases that are not about it"
    (doseq [c (vals (:capabilities (store/seed-db)))]
      (is (empty? (remove store/record-kinds (:scope c)))
          (str (:capability-uri c) " の scope に未知の kind がある")))))
