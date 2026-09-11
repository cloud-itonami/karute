(ns karute.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [karute.facts :as facts]))

(deftest an-uncatalogued-jurisdiction-is-unknown-not-permitted
  (testing "the single most important property of this table"
    (is (nil? (facts/basis-for "BRA")))
    (is (nil? (facts/basis-for "ZZZ")))
    (is (nil? (facts/basis-for nil)))
    (is (nil? (facts/basis-for "")))
    (is (some? (facts/basis-for "JPN")))))

(deftest lookup-is-case-and-whitespace-insensitive
  (is (= (facts/basis-for "JPN") (facts/basis-for "jpn")))
  (is (= (facts/basis-for "JPN") (facts/basis-for " JPN "))))

(deftest every-entry-carries-a-citation
  (testing "an entry without a legal basis and a provenance URL is an
            invented requirement wearing a jurisdiction's name"
    (doseq [[iso e] facts/catalog]
      (is (seq (:legal-basis e))     (str iso " has no :legal-basis"))
      (is (seq (:owner-authority e)) (str iso " has no :owner-authority"))
      (is (re-find #"^https://" (str (:provenance e))) (str iso " has no https provenance"))
      (is (pos-int? (:retention-years e)) (str iso " has no retention period")))))

(deftest adequacy-is-reflexive-for-a-single-jurisdiction
  (testing "a domestic disclosure is not a cross-border transfer at all"
    (is (true? (facts/adequate? "JPN" "JPN")))
    (is (true? (facts/adequate? "USA" "USA")))))

(deftest adequacy-is-not-assumed-symmetric-for-absent-pairs
  (testing "the real instruments are asymmetric, and USA is absent on
            purpose -- the EU-US framework is a per-organisation
            certification and cannot be answered from an iso3 code"
    (is (true?  (facts/adequate? "JPN" "DEU")))
    (is (true?  (facts/adequate? "DEU" "JPN")))
    (is (false? (facts/adequate? "JPN" "USA")))
    (is (false? (facts/adequate? "USA" "JPN")))
    (is (false? (facts/adequate? "USA" "DEU")))))

(deftest adequacy-of-an-unknown-jurisdiction-is-false
  (is (false? (facts/adequate? "JPN" "BRA")))
  (is (false? (facts/adequate? "BRA" "JPN")))
  (is (false? (facts/adequate? nil "JPN"))))

(deftest retention-of-an-unknown-jurisdiction-is-nil-not-zero
  (testing "a caller that defaulted this to 0 would delete records it
            has no basis to delete"
    (is (nil? (facts/retention-years "BRA")))
    (is (= 5 (facts/retention-years "JPN")))))

(deftest coverage-reports-what-it-knows-not-what-exists
  (let [c (facts/coverage)]
    (is (= (count facts/catalog) (:count c)))
    (is (= (vec (sort (keys facts/catalog))) (:jurisdictions c)))
    (is (seq (:note c)))))
