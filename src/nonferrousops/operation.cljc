(ns nonferrousops.operation
  "Non-ferrous metal ore operations definitions and state transitions.")

(def operations
  "Defined operations for non-ferrous metal ore mining coordination.
  All effects are `:propose` -- this actor does NOT execute mining or
  safety authority decisions. It proposes coordination asks that may
  require human approval."
  {
   :propose/log-production
   {:name "Log Production Record"
    :description "Record non-ferrous ore output, commodity type, and grade data"
    :effect :propose
    :scope :coordination}

   :propose/schedule-maintenance
   {:name "Schedule Equipment Maintenance"
    :description "Propose equipment maintenance schedule"
    :effect :propose
    :scope :coordination}

   :propose/flag-safety-concern
   {:name "Flag Safety Concern"
    :description "Surface a mine-safety concern for escalation"
    :effect :propose
    :scope :coordination
    :escalates-always true}

   :propose/coordinate-shipment
   {:name "Coordinate Shipment"
    :description "Coordinate non-ferrous ore shipment logistics"
    :effect :propose
    :scope :coordination}
   })

(defn valid-operation?
  "Is this a known operation?"
  [op]
  (boolean (operations op)))

(defn operation-info
  "Get metadata for an operation."
  [op]
  (operations op))
