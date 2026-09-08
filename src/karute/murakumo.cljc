(ns karute.murakumo
  "Pure cljc actor boundary generated from manifest migration scaffold."
  (:require [kotoba.lang.text :as str]))

(def actor-did
  "did:web:karute.etzhayyim.com")

(def common-gates
  [:council-charter-attestation
   :no-platform-held-key-baseline
   :no-probing-baseline
   :murakumo-only-inference-baseline
   :did-primary-baseline
   :append-only-gate-baseline
   :kotoba-only-substrate-baseline])

(defn collection
  [name]
  (str "com.etzhayyim.karute." name))

(def cell-specs {
  :listpatients {:legacy-cell "com-etzhayyim-apps-karute-listPatients"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "listpatients")]
     :required-gates common-gates
     :trigger "manifest cell listpatients"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :getpatient {:legacy-cell "com-etzhayyim-apps-karute-getPatient"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "getpatient")]
     :required-gates common-gates
     :trigger "manifest cell getpatient"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :createsoapnote {:legacy-cell "com-etzhayyim-apps-karute-createSoapNote"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "createsoapnote")]
     :required-gates common-gates
     :trigger "manifest cell createsoapnote"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :createmedicationrequest {:legacy-cell "com-etzhayyim-apps-karute-createMedicationRequest"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "createmedicationrequest")]
     :required-gates common-gates
     :trigger "manifest cell createmedicationrequest"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :createobservation {:legacy-cell "com-etzhayyim-apps-karute-createObservation"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "createobservation")]
     :required-gates common-gates
     :trigger "manifest cell createobservation"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :createdispense {:legacy-cell "com-etzhayyim-apps-karute-createDispense"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "createdispense")]
     :required-gates common-gates
     :trigger "manifest cell createdispense"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :listdispenses {:legacy-cell "com-etzhayyim-apps-karute-listDispenses"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "listdispenses")]
     :required-gates common-gates
     :trigger "manifest cell listdispenses"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :getchartsummary {:legacy-cell "com-etzhayyim-apps-karute-getChartSummary"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "getchartsummary")]
     :required-gates common-gates
     :trigger "manifest cell getchartsummary"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :exportfhirbundle {:legacy-cell "com-etzhayyim-apps-karute-exportFhirBundle"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "exportfhirbundle")]
     :required-gates common-gates
     :trigger "manifest cell exportfhirbundle"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :grantconsent {:legacy-cell "com-etzhayyim-apps-karute-grantConsent"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "grantconsent")]
     :required-gates common-gates
     :trigger "manifest cell grantconsent"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :revokeconsent {:legacy-cell "com-etzhayyim-apps-karute-revokeConsent"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "revokeconsent")]
     :required-gates common-gates
     :trigger "manifest cell revokeconsent"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :listconsent {:legacy-cell "com-etzhayyim-apps-karute-listConsent"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "listconsent")]
     :required-gates common-gates
     :trigger "manifest cell listconsent"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :rekeyrecord {:legacy-cell "com-etzhayyim-apps-karute-rekeyRecord"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "rekeyrecord")]
     :required-gates common-gates
     :trigger "manifest cell rekeyrecord"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :redactrecord {:legacy-cell "com-etzhayyim-apps-karute-redactRecord"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "redactrecord")]
     :required-gates common-gates
     :trigger "manifest cell redactrecord"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :listtombstones {:legacy-cell "com-etzhayyim-apps-karute-listTombstones"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "listtombstones")]
     :required-gates common-gates
     :trigger "manifest cell listtombstones"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :listauditevents {:legacy-cell "com-etzhayyim-apps-karute-listAuditEvents"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "listauditevents")]
     :required-gates common-gates
     :trigger "manifest cell listauditevents"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :requestiryobilling {:legacy-cell "com-etzhayyim-apps-karute-requestIryoBilling"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "requestiryobilling")]
     :required-gates common-gates
     :trigger "manifest cell requestiryobilling"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :shinkaevolution {:legacy-cell "com-etzhayyim-apps-standard-shinkaEvolution"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "shinkaevolution")]
     :required-gates common-gates
     :trigger "manifest cell shinkaevolution"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :shinkaknowledge {:legacy-cell "com-etzhayyim-apps-standard-shinkaKnowledge"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "shinkaknowledge")]
     :required-gates common-gates
     :trigger "manifest cell shinkaknowledge"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :shinka {:legacy-cell "shinka"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "shinka")]
     :required-gates common-gates
     :trigger "manifest cell shinka"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :koji {:legacy-cell "koji"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "koji")]
     :required-gates common-gates
     :trigger "manifest cell koji"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :kyumei {:legacy-cell "kyumei"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "kyumei")]
     :required-gates common-gates
     :trigger "manifest cell kyumei"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :domain-knowledge {:legacy-cell "domain-knowledge"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "domain-knowledge")]
     :required-gates common-gates
     :trigger "manifest cell domain-knowledge"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :record {:legacy-cell "com-etzhayyim-encrypted-record"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "record")]
     :required-gates common-gates
     :trigger "manifest cell record"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :terminologybinding {:legacy-cell "com-etzhayyim-apps-karute-terminologyBinding"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "terminologybinding")]
     :required-gates common-gates
     :trigger "manifest cell terminologybinding"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :referral {:legacy-cell "com-etzhayyim-apps-karute-referral"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "referral")]
     :required-gates common-gates
     :trigger "manifest cell referral"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
})

(defn safe-rkey
  [s]
  (let [clean (-> (str s)
                  (str/replace #"^did:web:" "")
                  (str/replace #"[^A-Za-z0-9._~-]" "-"))]
    (if (str/blank? clean) "unknown" clean)))

(defn gate-value
  [attestations gate]
  (or (get attestations gate)
      (get attestations (name gate))
      (when (set? attestations) (attestations gate))
      (when (set? attestations) (attestations (name gate)))))

(defn missing-gates
  [spec attestations]
  (->> (:required-gates spec)
       (remove #(boolean (gate-value attestations %)))
       vec))

(defn put-record-effect
  [collection rkey record]
  {:op :mst/put-record
   :actor actor-did
   :collection collection
   :rkey rkey
   :record record})

(defn records-for
  [spec {:keys [records record computed-at request-id]
         :as input}]
  (let [input-records (cond
                        (map? records) records
                        (some? record) {0 record}
                        :else {})
        base {:actorDid actor-did
              :computedAt computed-at
              :legacyCell (:legacy-cell spec)
              :phase (:phase spec)
              :requestId request-id
              :actorBoundary "cljc-migration-scaffold"
              :scaffold true
              :constitutionalStatus "attested-plan"}]
    (map-indexed
     (fn [idx coll]
       (let [record* (merge {:$type coll}
                            base
                            (or (get input-records coll)
                                (get input-records idx)
                                {}))
             rkey (safe-rkey (or (:rkey record*)
                                 (get record* "rkey")
                                 (:tid record*)
                                 request-id
                                 (str (:legacy-cell spec) "-" idx)))]
         {:collection coll
          :record record*
          :rkey rkey}))
     (:collections spec))))

(defn cell-plan
  [cell-key {:keys [attestations] :as input}]
  (let [spec (get cell-specs cell-key)]
    (when-not spec
      (throw (ex-info "unknown cell" {:cell cell-key})))
    (let [missing (missing-gates spec attestations)]
      (merge
       {:cell cell-key
        :legacy-cell (:legacy-cell spec)
        :actor actor-did
        :phase (:phase spec)
        :murakumo-node (:murakumo-node spec)
        :trigger (:trigger spec)
        :ceiling (:ceiling spec)
        :required-gates (:required-gates spec)
        :missing-gates missing}
       (if (seq missing)
         {:status :blocked
          :effects []}
         (let [planned-records (records-for spec input)]
           {:status :ready
            :records (vec planned-records)
            :effects (mapv (fn [{:keys [collection record rkey]}]
                             (put-record-effect collection rkey record))
                           planned-records)}))))))

(defn all-cell-plans
  [input]
  (into {}
        (map (fn [cell-key] [cell-key (cell-plan cell-key input)]))
        (keys cell-specs)))
