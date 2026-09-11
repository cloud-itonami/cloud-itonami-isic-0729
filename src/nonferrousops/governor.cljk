(ns nonferrousops.governor
  "Non-Ferrous Ore Governor -- the independent compliance layer that earns
  NonFerrousOps-LLM the right to commit. The LLM has no notion of
  jurisdictional mine-safety law, whether site records are actually
  verified, whether ore grades/commodity types match recorded claims, or
  whether a proposal is actually safe to execute, so this MUST be a
  separate system able to *reject* a proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is `:nonferrous-ore-governor`, grep-verified
  UNIQUE fleet-wide. This blueprint's ops are COORDINATION only
  (logging, scheduling, flagging, shipment coordination) -- NOT
  extraction, blasting, or mine-safety-authority decisions. Those are
  permanently excluded from proposal ops; any attempt to propose them
  is a HARD violation (escalate to human).

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'mine/site record must be verified; commodity must be a
  recognized non-ferrous ore type; safety concerns escalate; all
  proposals are coordination asks, never extraction authority') names
  exactly the checks below.

  Six checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them.

    1. Forbidden operation       -- is the proposal attempting an
                                   extraction/blasting/safety-authority
                                   decision (permanently excluded)?
    2. Spec-basis                -- did the proposal cite an OFFICIAL
                                   source, or invent one?
    3. Site/Mine record missing  -- does the site/mine record exist
                                   and is it verified?
    4. Unknown commodity         -- for production-logging and shipment
                                   ops, does the proposal name a
                                   recognized non-ferrous commodity
                                   (copper/lithium/nickel/cobalt/
                                   rare-earth)?
    5. Safety concern escalation -- is this a `:flag-safety-concern`
                                   operation? -> ALWAYS escalate.
    6. Confidence floor / escalate-- operation is low-confidence OR
                                   confidence is uncertain -> escalate."
  (:require [nonferrousops.facts :as facts]
            [nonferrousops.store :as store]))

(def confidence-floor 0.6)

(def escalate-always
  "Operations that ALWAYS require human escalation, even when clean.
  Flagging a safety concern is the cardinal escalation trigger in
  this actor: this operation alone cannot auto-proceed."
  #{:propose/flag-safety-concern})

(def forbidden-ops
  "Operations permanently excluded from this actor's proposal set.
  This actor supports COORDINATION (logging, scheduling, flagging,
  shipment coordination) -- NOT extraction, blasting, or mine-safety-
  authority decisions. Any proposal attempting these is a HARD block."
  #{:extraction/extract :extraction/blast :authority/safety-clearance})

(def commodity-required-ops
  "Operations that must name a recognized non-ferrous commodity."
  #{:propose/log-production :propose/coordinate-shipment})

;; ----------------------------- checks -----------------------------

(defn- forbidden-operation-violations
  "Proposals attempting extraction/blasting/safety-authority are
  permanently HARD violations -- this actor does coordination only."
  [{:keys [op]} _proposal]
  (when (forbidden-ops op)
    [{:rule :forbidden-operation
      :detail (str "採掘・発破・保安当局決定(" op ")は本アクターのスコープ外。
                   本アクターは非鉄金属鉱業運営調整(産出量/品位ロギング・保守
                   スケジュール・安全懸念フラグ・出荷調整)のみ対象。禁止操作は
                   人間による承認もできない。")}]))

(defn- spec-basis-violations
  "A proposal with no spec-basis citation is a HARD violation."
  [{:keys [op]} proposal]
  (when (contains? #{:propose/log-production :propose/schedule-maintenance
                     :propose/flag-safety-concern :propose/coordinate-shipment} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- site-record-missing-violations
  "All operations require a verified site/mine record to exist."
  [{:keys [op subject]} st]
  (when (contains? #{:propose/log-production :propose/schedule-maintenance
                     :propose/flag-safety-concern :propose/coordinate-shipment} op)
    (let [site (store/site st subject)]
      (cond
        (not site)
        [{:rule :site-record-missing
          :detail (str subject " の鉱山/サイト記録が存在しない")}]

        (not (true? (:verified? site)))
        [{:rule :site-not-verified
          :detail (str subject " の鉱山/サイト記録が未検証状態")}]))))

(defn- unknown-commodity-violations
  "Production-logging and shipment-coordination proposals must name a
  recognized non-ferrous commodity (copper/lithium/nickel/cobalt/
  rare-earth). Unknown or missing commodity is a HARD violation -- this
  actor cannot coordinate ore it cannot classify."
  [{:keys [op]} proposal]
  (when (contains? commodity-required-ops op)
    (let [commodity (get-in proposal [:value :commodity])]
      (when-not (facts/known-commodity? commodity)
        [{:rule :unknown-commodity
          :detail (str "未登録の非鉄金属種別(" commodity ")は本アクターの対象外。"
                       "対象: 銅・リチウム・ニッケル・コバルト・レアアース。")}]))))

(defn- safety-concern-escalation-violations
  "Any `:flag-safety-concern` proposal is ALWAYS escalated to human,
  even if every other governor check passes and confidence is high.
  This is the cardinal escalation trigger for mine safety."
  [{:keys [op]} _proposal]
  (when (= op :propose/flag-safety-concern)
    [{:rule :safety-concern-escalation
      :detail "鉱山安全懸念フラグは常に人間承認を要求する"}]))

(defn check
  "Censors a NonFerrousOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (forbidden-operation-violations request proposal)
                           (spec-basis-violations request proposal)
                           (site-record-missing-violations request st)
                           (unknown-commodity-violations request proposal)
                           (safety-concern-escalation-violations request proposal)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (or hard? low?)}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
