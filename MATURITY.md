# karute (カルテ) — Maturity Ledger

`/loop` 進捗台帳。各イテレーションで **1項目** だけ成熟度を上げ、ここに記録する。
honest framing: できていないことは「未」と明記する。

- Actor: `did:web:karute.etzhayyim.com` · ADR-2605231100 (EMR Phase 1) · DID-worker LIVE
- **二層構造**: (1) この this repository = kotoba-native **charter surface** — 11 FHIR
  Lexicons + 憲法ゲートテスト; (2) EMR の実装 (Svelte SuperApp + lg-karute pod + did-worker) は
  `60-apps/etzhayyim-project-karute/` + `50-infra/karute-did-web/` 側(`actor.edn` の deploy stages)。
  この台帳は **(1) の charter surface** の成熟度のみを追う(実 EMR は別レイヤ)。
- 不変条件(厳守): 全 PHI は `com.etzhayyim.encrypted.record` envelope のみ(平文 PHI を MST に
  書かない) · consent = `com.etzhayyim.consent.capability`(Ed25519 member-signed, no-server-key
  ADR-2605231525) · 3軸 split clean(payoff/custody/settlement = etzhayyim) · 患者識別子 =
  DID(`patientDid`)、平文氏名/MRN を連結キーにしない。

## 成熟度チェックリスト

| # | 項目 | 状態 | 完了イテレーション |
|---|---|---|---|
| 1 | ADR-2605231100 (EMR Phase 1) | ✅ | init |
| 2 | actor.edn + actor.edn(deploy pipeline)+ CLAUDE.md + NOTICE | ✅ | init |
| 3 | 11 FHIR Lexicons (`com.etzhayyim.karute.*` — patient/encounter/condition/observation/medicationRequest/serviceRequest/carePlan/dispenseRecord/soapNote/homecareEpisode/homeVisit) | ✅ | init |
| 4 | did:web:karute.etzhayyim.com worker LIVE(`50-infra/karute-did-web`) | ✅ | init |
| 5 | **charter-gate テスト** (`methods/test_charter_gates.cljc` — 4 tests / 35 assertions) | ✅ | **iter (this)** |
| 6 | run_tests.clj が charter-gate suite を実行(actor reflex に wired) | ✅ | **iter (this)** |
| 7 | encrypted-envelope 規律をスキーマ層で機械強制(`additionalProperties:false` + 平文 PHI フィールド拒否、R1) | 部分 | governor 層のみ (下記 #12)。lexicon の JSON schema 層は 未 |
| 8 | consent.capability の Ed25519 検証テスト(member-signed / server-refused) | 未 | — |
| 9 | 患者 DID = 30日 rotating pseudonym(ADR-2605181200)の構造検証 | 未 | — |
| 10 | kotoba EAVT への FHIR inner-type 投影(public graph = meta only)の検証 | 未 | — |
| 11 | iryo(レセプト)への hand-off boundary テスト(karute → iryo consent-capability) | 部分 | purpose 束縛は #12 で強制。iryo 側との実 hand-off は 未 |
| 12 | **governed actor core** — 7部品 (facts/store/governor/phase/operation/ledger/sim) + 17 HARD check + 追記専用監査台帳 | ✅ | **iter 2026-08-31** |
| 13 | consent capability の Ed25519 署名検証(#8 と対) — governor は現在 capability の**内容**のみ検査し、**署名**は検査しない | 未 | — |
| 14 | `cognitect.test-runner` が `methods/test_charter_gates.cljc` を発見しない(ns 名が `-test` で終わらない)。`bb run_tests.clj` のみが実行する | 未 | — |

## イテレーション記録

### iter — 2026-08-31
**上げた項目: #12 — 宣言されていた governance を、実行でき拒否できるものにした。**

`manifest.edn` は以前からこの actor の governance を**宣言**していた。`requestIryoBilling`
パイプラインは consent 検査を Cypher の `WHERE` 句として持ち(granter = 患者 AND grantee = iryo
AND purpose = 'insurance-billing' AND status = 'active' AND expiresAt > now)、`:governance` ブロックは
PHI ポリシーを散文で述べていた。しかし `WHERE` 句は 1 つのクエリの内側のフィルタであって、
**どの行が返るか**を決めるだけで**操作が許されたか**を決めない。そして他のパイプラインには
それを再検査するものが無かった —— とりわけ `exportFhirBundle`、**この actor で最も多くの PHI を
開示する**パイプライン(患者の全タイムラインを recipient DID 向けに復号する)には、
consent 検査が manifest 上に**1 つも無かった**。

この iteration はその宣言を実行可能に、そして**拒否可能**にした。新規 7 名前空間(pure `.cljc`、
新規依存ゼロ):

| 部品 | 役割 |
|---|---|
| `karute.facts` | 法域別の PHI 開示法的根拠(JPN/USA/DEU/FRA、条文 + provenance URL 付き)。未収載 = UNKNOWN であって「許可」ではない |
| `karute.store` | consent capability / 公開インデックス / 開示ログ。純関数、値としての db |
| `karute.governor` | 独立コンプライアンス層。**17 の HARD check** |
| `karute.phase` | Phase 0→3 ロールアウト。`:disclosure/*` は**どの phase でも auto にならない** |
| `karute.operation` | advise → govern → decide → record の 5 段。hold も escalate も db を変えない |
| `karute.ledger` | 追記専用監査台帳。ハッシュ連鎖 + `verify` が改竄箇所を指す |
| `karute.sim` | 実演器 `clojure -M:sim` |

**この actor 固有の check (#12 の distinctive)**: `public-meta-not-allowlisted`。
`manifest.edn` は「患者識別情報を含むフィールドの MST 平文書き込みを禁止」と述べ、
同じファイルの全 write パイプラインが `publicMeta` map をそのまま `graph.write` に渡していた。
両者の間に何も無かった。この check は `karute.store/public-meta-allowlist` —— **閉じた集合** ——
の外のキーを拒否する。ブロックリストは「自分が既に思いついた漏洩か?」に答え、
**まだ誰も思いついていない漏洩すべてに対して静かに『いいえ』を返す**。閉じた集合は
「このキーは公開する設計だったか?」に答えるので、来年 lexicon に足されたフィールドは
誰かが意図的にここへ足すまで拒否される。

**実演器は 0 件拒否の実行を pass として報告しない。** `clojure -M:sim` は、拒否 0 件・
commit 0 件・台帳チェーン破損・`gov/all-hard-rules` のうち一度も発火しなかった check が
あれば **exit 1** する。拒否しない governor の実演は実演ではなく、しかも成功した実行と
まったく同じ顔(同じ exit code、同じ明るい出力)をする。

**測定(2026-08-31、pin `aaae559` からの差分):**

- `clojure -M:test` — before 9 tests / 356 assertions → after **71 / 571**
- `bb run_tests.clj` — before 13 / 391 → after **75 / 606**(charter gate 4/35 を含む)
- `clojure -M:sim` — 無改変で exit 0、拒否 17 件 / commit 2 件 / chain verified
- **床の判別性を実測**: governor の granter-mismatch check を 1 つ潰すと sim は exit 1 になり、
  `一度も発火しなかった HARD check: [:consent-granter-mismatch]` と、**潰したものと一致する名前**を
  報告した(escalate が 1→2 に増える —— 他患者の consent での開示が人間の承認待ち行列に
  普通に並ぶ、という当該欠陥の実際の姿)
- **テストの噛み付きを実測**: 10 個の変異(check 無効化 / 有効期間の上界を閉区間に / 不正な
  instant を拒否せず比較 / allowlist に `:name`+`:diagnosis` を追加 / `verify` を常に ok に /
  disclosure op を phase 3 の auto に / adequacy を常に真に / 未知法域を JPN に fallback /
  開示ガードを常に false に / hold が effect を適用)を 1 つずつ当て、**10 件とも赤**になり、
  それぞれ当該不変条件を主張しているテスト名を挙げた。無改変で緑に戻ることを毎回確認

**honest framing —— やっていないこと:**

- **consent capability の署名を検証していない**(#13)。governor は capability の *内容*
  (granter/grantee/purpose/scope/status/有効期間/委譲深さ)を検査するが、**Ed25519 署名は
  検査しない**。store に載った capability は真正であると仮定している。#8 と対で残す
- `karute.facts` の法域は **4 件**。これは世界の調査ではなく出発点であり、
  `coverage` がその数を正直に報告する
- `karute.ledger` のハッシュ連鎖は**改竄の痕跡を残す装置であって暗号学的コミットメントではない**。
  衝突耐性は無く、この台帳に書ける攻撃者は連鎖を再計算できる。**末尾の切り詰めは検出しない**
  (`truncating-the-tail-still-verifies-and-that-is-honest` がその真の性質を assert している)。
  暗号学的な形は substrate 側の署名済みエンベロープ(ADR-2605181100)
- `karute.operation` は langgraph StateGraph **ではない**。同 fleet の兄弟 actor 群は
  `langgraph.graph` を `:local/root` で参照するが、この repo は `repository-contracts.edn` で
  standalone を宣言しており runtime 依存が 1 つも無い。形は同一で、seam は docstring に明記した
- この layer は **charter surface** のものであり、実 EMR(Svelte SuperApp + lg-karute pod)は
  依然として別レイヤ。governor はまだ実パイプラインに配線されていない

### iter (this) — 2026-06-18
**上げた項目: #5 + #6 — charter surface のテスト被覆をゼロから確立。**
`methods/test_charter_gates.cljc` を新規作成(4 deftests / 35 assertions、green)。central FHIR
lexicons を cheshire で読み、charter が依存する**構造的不変条件**を pin した(誤った no-plaintext-PHI
主張はしない — これらは encrypted-envelope の inner-type であり、PHI 機密は envelope 層で強制される):
- **interop** — 全 11 resource が `fhirResourceType` const を pin(Patient/Encounter/Condition/
  Observation/MedicationRequest/ServiceRequest/CarePlan/MedicationDispense/Composition/EpisodeOfCare)。
- **DID-centric identity** — 全 clinical resource(10/11)が `patientDid` を required;患者は DID
  束縛で、平文氏名/MRN を連結キーにしない(subject-DID custody, ADR-2605172400)。
- **accountability** — `soapNote.authorDid` required;prescriber/performer/pharmacist/requester/
  recordedBy は DID フィールド(無名/自由記述の著者は表現不能)。
- **closed clinical vocabularies** — encounter/observation/medicationRequest の status・class・
  category・intent は閉じた FHIR value set。
`bb run_tests.clj` が charter
suite を実行するよう確認(actor reflex に wired)。ゲートは一切弱めず、assert のみ。
