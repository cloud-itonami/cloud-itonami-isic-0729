(ns nonferrousops.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [nonferrousops.facts :as facts]))

(deftest known-commodity-test
  (testing "Recognized non-ferrous commodities"
    (is (true? (facts/known-commodity? :copper)))
    (is (true? (facts/known-commodity? :lithium)))
    (is (true? (facts/known-commodity? :nickel)))
    (is (true? (facts/known-commodity? :cobalt)))
    (is (true? (facts/known-commodity? :rare-earth))))

  (testing "Unrecognized commodities"
    (is (false? (facts/known-commodity? :unobtainium)))
    (is (false? (facts/known-commodity? :iron)))
    (is (false? (facts/known-commodity? nil)))))

(deftest known-jurisdiction-test
  (testing "Recognized jurisdictions"
    (is (true? (facts/known-jurisdiction? :jp)))
    (is (true? (facts/known-jurisdiction? :us)))
    (is (true? (facts/known-jurisdiction? :au)))
    (is (true? (facts/known-jurisdiction? :br))))

  (testing "Unrecognized jurisdiction"
    (is (false? (facts/known-jurisdiction? :zz)))))

(deftest required-evidence-satisfied-test
  (testing "All required evidence present"
    (is (true? (facts/required-evidence-satisfied?
                :us #{:site-record :ore-assay-report :equipment-safety :permit-valid}))))

  (testing "Missing required evidence"
    (is (false? (facts/required-evidence-satisfied?
                 :us #{:site-record :ore-assay-report}))))

  (testing "Unknown jurisdiction has an empty checklist, trivially satisfied"
    (is (true? (facts/required-evidence-satisfied? :zz #{})))))

(deftest production-grade-valid-test
  (testing "Grade within bounds"
    (is (true? (facts/production-grade-valid? 0.8 0.5 1.0))))

  (testing "Grade below minimum"
    (is (false? (facts/production-grade-valid? 0.3 0.5 1.0))))

  (testing "Grade above maximum"
    (is (false? (facts/production-grade-valid? 1.5 0.5 1.0)))))
