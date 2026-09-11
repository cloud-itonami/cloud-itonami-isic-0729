(ns nonferrousops.registry
  "Non-ferrous metal ore operations registry and calculations."
  (:require [nonferrousops.facts :as facts]))

(defn ore-grade-matches-claim?
  "Verify that claimed ore grade matches the independently computed grade,
  for a recognized commodity. This is the same ground-truth-recompute
  discipline every prior actor's own cost/total-matching checks establish."
  [production-record]
  (let [commodity (:commodity production-record)
        claimed (:claimed-grade production-record)
        actual (:grade-actual production-record)
        min-bound (:grade-min production-record)
        max-bound (:grade-max production-record)]
    (and (facts/known-commodity? commodity)
         claimed actual
         (>= actual min-bound)
         (<= actual max-bound)
         (= claimed actual))))

(defn compute-production-value
  "Independently compute the value of produced ore based on:
  quantity (tonnes) x grade (%) x price-per-unit."
  [production-record]
  (let [qty (:quantity-tonnes production-record 0)
        grade (:grade-actual production-record 0)
        price (:price-per-unit production-record 0)]
    (* qty grade price)))

(defn shipment-record-valid?
  "Verify a shipment record has all required fields, including a
  recognized commodity."
  [shipment]
  (and (:shipment-id shipment)
       (:site-id shipment)
       (:quantity-tonnes shipment)
       (:destination shipment)
       (facts/known-commodity? (:commodity shipment))
       (true? (:verified? shipment))))
