(ns karute.facts
  "Per-jurisdiction PHI-disclosure legal-basis catalog -- the citation
  table `karute.governor` checks every disclosure proposal against
  ('did the advisor cite an OFFICIAL public source for this
  jurisdiction's health-data disclosure rules, or did it invent one?').

  Structurally the same discipline `commitledger.facts`
  (`cloud-itonami-commitment-ledger`) and `cerealops.facts`
  (`cloud-itonami-isic-0111`) already established in this fleet --
  re-implemented locally here, not required from either sibling, because
  this repo is standalone by its own `repository-contracts.edn`
  (`:classification :actor`, no `:local/root` dependencies).

  Coverage is reported HONESTLY (see `coverage`): a jurisdiction not in
  this table has NO legal basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries. A jurisdiction
  being ABSENT is not the same as a jurisdiction being FORBIDDEN; it is
  `unknown`, and this actor refuses to disclose into an unknown.

  Seed values are drawn from each jurisdiction's official statute or
  regulator (see `:provenance`); a STARTING catalog, not a from-scratch
  survey of all ~194 jurisdictions. Extending coverage is additive: add
  one map to `catalog`, cite a real source, done -- never invent a
  jurisdiction's requirements to make coverage look bigger.

  `JPN` is this actor's own primary reference jurisdiction. The three
  rules the governor actually enforces from it are:

    - 個人情報保護法 第27条 -- 第三者提供の制限. Disclosure to a third
      party requires the data subject's consent. `karute.governor`'s
      consent-capability checks are the machine form of this.
    - 個人情報保護法 第18条 -- 利用目的による制限. Personal data may not
      be handled beyond the purpose for which consent was obtained.
      `purpose-not-bound-violations` is the machine form of this.
    - 個人情報保護法 第28条 -- 外国にある第三者への提供の制限. Transfer
      to a third party in a foreign country needs its own basis.
      `cross-border-without-basis-violations` is the machine form, and
      `adequacy` below is the (small, honest) allowlist it reads.

  医療情報 is 要配慮個人情報 (special-care-required personal information,
  同法 第2条第3項), which is why this actor has no 'implied consent'
  path at all: opt-out disclosure is unavailable for this category."
  (:require [kotoba.lang.text :as str]))

(def catalog
  "iso3 -> legal basis map. `:legal-basis` / `:owner-authority` /
  `:provenance` are the citation the governor requires before any
  disclosure proposal can commit."
  {"JPN"
   {:name            "Japan"
    :owner-authority "個人情報保護委員会 (Personal Information Protection Commission) / 厚生労働省 (Ministry of Health, Labour and Welfare)"
    :legal-basis     "個人情報保護法 (Act on the Protection of Personal Information, Act No. 57 of 2003) 第18条・第27条・第28条 / 次世代医療基盤法 (Act No. 28 of 2018) / 医師法 (Act No. 201 of 1948) 第24条"
    :special-category "要配慮個人情報 (個人情報保護法 第2条第3項)"
    :retention-years 5      ; 医師法 第24条第2項 -- 診療録は5年間保存
    :retention-basis "医師法 第24条第2項 (診療録の5年保存義務)"
    :provenance      "https://www.ppc.go.jp/personalinfo/legal/"}

   "USA"
   {:name            "United States"
    :owner-authority "U.S. Department of Health and Human Services, Office for Civil Rights"
    :legal-basis     "HIPAA Privacy Rule, 45 CFR §164.502(b) (minimum necessary) / §164.508 (authorization required for disclosure)"
    :special-category "Protected Health Information (45 CFR §160.103)"
    :retention-years 6      ; 45 CFR §164.530(j)(2) -- documentation retained 6 years
    :retention-basis "45 CFR §164.530(j)(2)"
    :provenance      "https://www.ecfr.gov/current/title-45/subtitle-A/subchapter-C/part-164"}

   "DEU"
   {:name            "Germany"
    :owner-authority "Bundesbeauftragte für den Datenschutz und die Informationsfreiheit"
    :legal-basis     "GDPR (Regulation (EU) 2016/679) Article 9(2)(h) (health care) / Articles 44-49 (third-country transfers)"
    :special-category "Data concerning health (GDPR Article 9(1))"
    :retention-years 10     ; §630f(3) BGB -- Patientenakte 10 Jahre
    :retention-basis "§630f(3) BGB (Bürgerliches Gesetzbuch)"
    :provenance      "https://eur-lex.europa.eu/eli/reg/2016/679/oj"}

   "FRA"
   {:name            "France"
    :owner-authority "Commission Nationale de l'Informatique et des Libertés (CNIL)"
    :legal-basis     "GDPR (Regulation (EU) 2016/679) Article 9(2)(h) / Code de la santé publique Article L1110-4"
    :special-category "Data concerning health (GDPR Article 9(1))"
    :retention-years 20     ; CSP R1112-7 -- dossier médical 20 ans
    :retention-basis "Code de la santé publique Article R1112-7"
    :provenance      "https://eur-lex.europa.eu/eli/reg/2016/679/oj"}})

(def adequacy
  "Ordered pairs {from-iso3 #{to-iso3 ..}} for which a cross-border
  disclosure has a standing legal basis WITHOUT a per-transfer
  safeguard. This is deliberately TINY and asymmetric, because the real
  instruments are.

    JPN -> EEA member : the EU adequacy decision of 23 January 2019 for
                        Japan, and Japan's own PPC designation of the EEA
                        under 個人情報保護法 第28条.
    EEA member -> JPN : the same mutual finding, other direction.

  A pair NOT listed here is not necessarily unlawful in the real world
  -- it means this actor has no machine-checkable standing basis for it,
  so the disclosure must carry its own `:transfer-safeguard` (SCCs,
  explicit consent under GDPR Article 49(1)(a) / 個人情報保護法 第28条第1項)
  or the governor holds. USA is deliberately absent: the EU-US Data
  Privacy Framework is a per-organisation certification, not a blanket
  country pairing, so it cannot be answered from an iso3 code alone."
  {"JPN" #{"DEU" "FRA"}
   "DEU" #{"JPN"}
   "FRA" #{"JPN"}})

(defn basis-for
  "The legal-basis map for an iso3 code, or nil. nil means UNKNOWN, and
  the governor treats unknown as a hold -- never as permission."
  [iso3]
  (when (and (string? iso3) (not (str/blank? iso3)))
    (get catalog (str/upper (str/trim iso3)))))

(defn adequate?
  "True when `from` -> `to` has a standing cross-border basis in
  `adequacy`. A same-jurisdiction disclosure is not a cross-border
  transfer at all and is trivially true."
  [from to]
  (boolean
   (and (string? from) (string? to)
        (let [f (str/upper (str/trim from))
              t (str/upper (str/trim to))]
          (or (= f t)
              (contains? (get adequacy f #{}) t))))))

(defn retention-years
  "Statutory retention floor in years for a jurisdiction, or nil when
  unknown. Callers MUST NOT default a nil to zero -- an unknown
  retention period is a reason to escalate, not a reason to delete."
  [iso3]
  (:retention-years (basis-for iso3)))

(defn coverage
  "Honest coverage report. `:jurisdictions` is what this table actually
  knows, NOT a claim about the world."
  []
  {:jurisdictions   (vec (sort (keys catalog)))
   :count           (count catalog)
   :adequacy-pairs  (reduce + 0 (map count (vals adequacy)))
   :note            "A jurisdiction absent from this catalog has no legal basis here and is treated as UNKNOWN (hold), never as permitted."})
