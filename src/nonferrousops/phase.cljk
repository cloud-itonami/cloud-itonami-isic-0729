(ns nonferrousops.phase
  "Non-ferrous metal ore operations state machine phases.")

(defn initial-phase
  "The initial phase for a new operation: proposal intake."
  [_operation]
  :intake)

(defn advance-phase
  "Advance from one phase to the next based on verdict.
  All coordination proposals follow: intake -> verify -> propose -> resolved."
  [current-phase verdict]
  (case current-phase
    :intake (if (:ok? verdict) :verify :hold)
    :verify (if (:ok? verdict) :propose :hold)
    :propose (if (:ok? verdict) :resolved :hold)
    :hold (if (:ok? verdict) :propose :hold)
    current-phase))

(defn final-phase?
  "Has the operation reached a terminal phase?"
  [phase]
  (contains? #{:resolved :hold} phase))

(defn phase-description
  "Human-readable description of a phase."
  [phase]
  (case phase
    :intake "提案受け入れ"
    :verify "基本検証"
    :propose "提案段階"
    :resolved "完了"
    :hold "ホールド"
    (str "不明: " phase)))
