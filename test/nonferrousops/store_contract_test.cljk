(ns nonferrousops.store-contract-test
  (:require [clojure.test :refer [deftest is testing]]
            [nonferrousops.store :as store]))

(deftest mem-store-implements-contract
  (testing "MemStore site operations"
    (let [st (store/mem-store)]
      (is (nil? (store/site st "unknown")))
      (let [st' (store/add-site st "site-1" {:name "Test Copper Mine"})
            site (store/site st' "site-1")]
        (is (= "site-1" (:id site)))
        (is (= "Test Copper Mine" (:name site))))))

  (testing "MemStore production records"
    (let [st (store/mem-store)]
      (is (nil? (store/production-record st "unknown")))
      (let [st' (store/add-production-record st "prod-1" {:tonnes 500 :commodity :lithium})
            rec (store/production-record st' "prod-1")]
        (is (= "prod-1" (:id rec)))
        (is (= 500 (:tonnes rec)))
        (is (= :lithium (:commodity rec))))))

  (testing "MemStore assessments"
    (let [st (store/mem-store)]
      (is (nil? (store/assessment-of st "site-1")))
      (let [st' (store/record-assessment st "site-1" {:checklist #{:verified}})
            assessment (store/assessment-of st' "site-1")]
        (is (contains? (:checklist assessment) :verified))))))

(deftest ledger-contract
  (testing "A fresh store's ledger is empty"
    (let [st (store/mem-store)]
      (is (vector? (store/ledger st)))
      (is (empty? (store/ledger st)))))

  (testing "append-ledger! is append-only and preserves insertion order"
    (let [st (store/mem-store)
          fact-1 {:t :committed :op :propose/log-production}
          fact-2 {:t :governor-hold :op :extraction/extract}]
      (is (= fact-1 (store/append-ledger! st fact-1)))
      (store/append-ledger! st fact-2)
      (is (= [fact-1 fact-2] (store/ledger st)))
      (is (= 2 (count (store/ledger st))))))

  (testing "the ledger survives across the 'new instance per mutation' value
            semantics add-site/add-production-record/record-assessment
            use -- it is threaded through the SAME underlying atom, not
            reset on every MemStore. reconstruction"
    (let [st (store/mem-store)
          st' (-> st
                  (store/add-site "site-1" {:name "Test Mine"})
                  (store/add-production-record "prod-1" {:tonnes 500}))]
      (store/append-ledger! st' {:t :committed :op :propose/log-production})
      (is (= 1 (count (store/ledger st'))))
      (is (= 1 (count (store/ledger st))) "same underlying ledger atom, visible from any handle"))))
