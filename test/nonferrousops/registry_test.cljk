(ns nonferrousops.registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [nonferrousops.registry :as registry]))

(deftest ore-grade-matches-claim-test
  (testing "Claimed grade matches actual grade, commodity recognized"
    (is (true? (registry/ore-grade-matches-claim?
                {:commodity :copper :claimed-grade 0.8 :grade-actual 0.8
                 :grade-min 0.5 :grade-max 1.0}))))

  (testing "Claimed grade does not match actual grade"
    (is (false? (registry/ore-grade-matches-claim?
                 {:commodity :copper :claimed-grade 0.9 :grade-actual 0.8
                  :grade-min 0.5 :grade-max 1.0}))))

  (testing "Actual grade out of bounds"
    (is (false? (registry/ore-grade-matches-claim?
                 {:commodity :copper :claimed-grade 1.5 :grade-actual 1.5
                  :grade-min 0.5 :grade-max 1.0}))))

  (testing "Unrecognized commodity never matches, even with consistent numbers"
    (is (false? (registry/ore-grade-matches-claim?
                 {:commodity :unobtainium :claimed-grade 0.8 :grade-actual 0.8
                  :grade-min 0.5 :grade-max 1.0})))))

(deftest compute-production-value-test
  (testing "Value is quantity x grade x price"
    (is (= 800.0 (registry/compute-production-value
                  {:quantity-tonnes 1000 :grade-actual 0.8 :price-per-unit 1.0}))))

  (testing "Missing fields default to zero"
    (is (= 0 (registry/compute-production-value {})))))

(deftest shipment-record-valid-test
  (testing "Fully valid shipment with a recognized commodity"
    (is (true? (registry/shipment-record-valid?
                {:shipment-id "ship-1" :site-id "site-1" :quantity-tonnes 500
                 :destination "Port of Antwerp" :commodity :cobalt :verified? true}))))

  (testing "Unrecognized commodity invalidates the shipment"
    (is (false? (registry/shipment-record-valid?
                 {:shipment-id "ship-1" :site-id "site-1" :quantity-tonnes 500
                  :destination "Port of Antwerp" :commodity :unobtainium :verified? true}))))

  (testing "Unverified shipment is invalid"
    (is (false? (registry/shipment-record-valid?
                 {:shipment-id "ship-1" :site-id "site-1" :quantity-tonnes 500
                  :destination "Port of Antwerp" :commodity :cobalt :verified? false}))))

  (testing "Missing required field is invalid"
    ;; `and` short-circuits on the missing `:shipment-id` key and returns
    ;; nil (not boolean false) here -- assert falsy, not exactly `false?`.
    (is (not (registry/shipment-record-valid?
              {:site-id "site-1" :quantity-tonnes 500
               :destination "Port of Antwerp" :commodity :cobalt :verified? true})))))
