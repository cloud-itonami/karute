#!/usr/bin/env bb
(require '[babashka.classpath :as cp]
         '[babashka.fs :as fs]
         '[clojure.test :as t])
(let [root (fs/parent (fs/absolutize *file*))]
  (cp/add-classpath (str root "/src"))
  (cp/add-classpath (str root "/test")))
;; Named explicitly, so adding a namespace under test/ without adding it
;; here means it does not run. That is a real hazard -- `cognitect.test-runner`
;; (`clojure -M:test`) discovers by the `-test` suffix and therefore does NOT
;; see `karute.methods.test-charter-gates`, while this list does. Neither
;; runner sees everything; both are kept in step by hand.
(def suites '[karute.murakumo-test
              karute.facts-test
              karute.store-test
              karute.ledger-test
              karute.governor-test
              karute.phase-test
              karute.operation-test
              karute.sim-test
              karute.methods.test-charter-gates])
(apply require suites)
(let [{:keys [fail error]} (apply t/run-tests suites)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
