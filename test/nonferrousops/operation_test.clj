(ns nonferrousops.operation-test
  "Integration tests for `nonferrousops.operation/build` -- builds the
  REAL compiled `langgraph.graph` StateGraph and runs it end-to-end via
  `langgraph.graph/run*` through commit / hard-hold / escalate-approve /
  escalate-reject routes. This namespace did not exist before: the file
  previously named `operation.cljc` was a static ops-metadata table
  with no state-transition logic at all, and `nonferrousops.sim`'s
  `run-proposal` hand-chained `advisor/mock-advisor -> governor/check`
  directly -- never touching `kotoba-lang/langgraph`. These tests prove
  the compiled graph is real and that the audit ledger
  (`nonferrousops.store/append-ledger!`, also new in this fix) is
  genuinely wired into the `:commit`/`:hold`/`:request-approval` nodes."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [nonferrousops.operation :as operation]
            [nonferrousops.store :as store]))

(defn- verified-site-store []
  (store/add-site (store/mem-store) "site-1"
                  {:name "Test Copper Mine" :jurisdiction :au :verified? true}))

(defn- exec [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- low-confidence-advisor [_context request]
  {:op (:op request)
   :subject (:subject request)
   :effect :propose
   :value {:spec-basis "internal-mock" :commodity (:commodity request :copper)}
   :cites ["mock-advisor"]
   :confidence 0.4})

(deftest commit-path-clean-proposal
  (testing "a clean, verified-site production-log request commits through
            the real compiled graph and appends to the audit ledger"
    (let [s (verified-site-store)
          actor (operation/build s)
          result (exec actor "t-commit"
                       {:op :propose/log-production :subject "site-1" :commodity :copper}
                       {:actor-id "op-01"})
          state (:state result)]
      (is (= :done (:status result)))
      (is (= :commit (:disposition state)))
      (let [ledger (store/ledger s)]
        (is (= 1 (count ledger)))
        (is (= :committed (:t (first ledger))))
        (is (= :propose/log-production (:op (first ledger))))
        (is (= "site-1" (:subject (first ledger))))))))

(deftest ledger-stays-empty-until-real-commit
  (testing "the ledger is empty before a run, and stays empty across a run
            that ends up escalated (not yet committed) -- only
            :commit/:hold/rejection append"
    (let [s (verified-site-store)
          actor (operation/build s {:advisor-fn low-confidence-advisor})]
      (is (empty? (store/ledger s)) "ledger starts empty")
      (let [held (exec actor "t-escalate-empty"
                       {:op :propose/log-production :subject "site-1" :commodity :copper}
                       {:actor-id "op-01"})]
        (is (= :interrupted (:status held)))
        (is (empty? (store/ledger s))
            "an interrupted (awaiting human sign-off) run has NOT committed anything yet")))))

(deftest hard-hold-path-forbidden-operation
  (testing "extraction is a forbidden operation -- a HARD, permanent
            governor violation that routes straight to :hold, NEVER
            through :request-approval"
    (let [s (verified-site-store)
          actor (operation/build s)
          result (exec actor "t-forbidden"
                       {:op :extraction/extract :subject "site-1"}
                       {:actor-id "op-01"})
          state (:state result)]
      (is (= :done (:status result)))
      (is (= :hold (:disposition state)))
      (let [ledger (store/ledger s)]
        (is (= 1 (count ledger)))
        (is (= :governor-hold (:t (first ledger))))
        (is (some #(= :forbidden-operation (:rule %)) (:violations (first ledger))))))))

(deftest hard-hold-path-unknown-commodity
  (testing "an unrecognized commodity is a HARD violation on a
            production-logging op"
    (let [s (verified-site-store)
          actor (operation/build s)
          result (exec actor "t-commodity"
                       {:op :propose/log-production :subject "site-1" :commodity :unobtainium}
                       {:actor-id "op-01"})]
      (is (= :hold (:disposition (:state result))))
      (is (some #(= :unknown-commodity (:rule %))
                (:violations (first (store/ledger s))))))))

(deftest hard-hold-path-site-not-verified
  (testing "an unverified site is a HARD violation, independently
            re-derived from the site's own store record, never trusted
            from the proposal"
    (let [s (store/add-site (store/mem-store) "site-2" {:verified? false})
          actor (operation/build s)
          result (exec actor "t-unverified"
                       {:op :propose/log-production :subject "site-2" :commodity :copper}
                       {:actor-id "op-01"})]
      (is (= :hold (:disposition (:state result))))
      (is (some #(= :site-not-verified (:rule %))
                (:violations (first (store/ledger s))))))))

(deftest hard-hold-path-site-record-missing
  (testing "a totally unregistered site is a HARD violation"
    (let [s (store/mem-store)
          actor (operation/build s)
          result (exec actor "t-missing"
                       {:op :propose/log-production :subject "unknown-site" :commodity :copper}
                       {:actor-id "op-01"})]
      (is (= :hold (:disposition (:state result))))
      (is (some #(= :site-record-missing (:rule %))
                (:violations (first (store/ledger s))))))))

(deftest hard-hold-path-safety-concern-is-permanently-blocked-not-escalated
  (testing "governor rejection blocks commit: :propose/flag-safety-concern
            is a HARD violation in nonferrousops.governor/check AS CODED
            (safety-concern-escalation-violations is folded into `hard`)
            -- it routes straight to :hold, the graph NEVER offers a
            human the chance to approve it via :request-approval"
    (let [s (verified-site-store)
          actor (operation/build s)
          result (exec actor "t-safety"
                       {:op :propose/flag-safety-concern :subject "site-1"}
                       {:actor-id "op-01"})
          state (:state result)]
      (is (= :done (:status result))
          "a HARD violation completes in one pass -- it is never :interrupted")
      (is (= :hold (:disposition state)))
      (is (some #(= :safety-concern-escalation (:rule %))
                (:violations (first (store/ledger s))))))))

(deftest escalate-then-approve-commits
  (testing "a low-confidence proposal escalates -- the real graph
            GENUINELY interrupts (checkpointed) at :request-approval; a
            human operations manager approve! resumes the SAME compiled
            graph and commits via the graph's own
            :request-approval -> :commit edge, durably appending to the
            ledger"
    (let [s (verified-site-store)
          actor (operation/build s {:advisor-fn low-confidence-advisor})
          held (exec actor "t-approve"
                     {:op :propose/log-production :subject "site-1" :commodity :copper}
                     {:actor-id "op-01"})]
      (is (= :interrupted (:status held)))
      (is (= [:request-approval] (:frontier held)))
      (is (empty? (store/ledger s)) "not yet committed -- awaiting human sign-off")
      (let [approved (g/run* actor {:approval {:status :approved :by "ops-manager-01"}}
                             {:thread-id "t-approve" :resume? true})
            approved-state (:state approved)]
        (is (= :done (:status approved)))
        (is (= :commit (:disposition approved-state)))
        (let [ledger (store/ledger s)]
          (is (= 1 (count ledger)))
          (is (= :committed (:t (first ledger))))
          (is (= "ops-manager-01" (:approved-by (first ledger)))))))))

(deftest escalate-then-reject-holds
  (testing "a human operations manager rejecting an escalated (low-
            confidence) request routes to :hold via the
            :request-approval node's own decision, and durably records
            the rejection -- not a hand-rolled parallel path"
    (let [s (verified-site-store)
          actor (operation/build s {:advisor-fn low-confidence-advisor})
          _held (exec actor "t-reject"
                      {:op :propose/coordinate-shipment :subject "site-1" :commodity :copper}
                      {:actor-id "op-01"})
          rejected (g/run* actor {:approval {:status :rejected :by "ops-manager-01"}}
                           {:thread-id "t-reject" :resume? true})
          rejected-state (:state rejected)]
      (is (= :done (:status rejected)))
      (is (= :hold (:disposition rejected-state)))
      (let [ledger (store/ledger s)]
        (is (= 1 (count ledger)))
        (is (= :approval-rejected (:t (first ledger))))
        (is (= "ops-manager-01" (:by (first ledger))))))))

(deftest schedule-maintenance-commits-without-commodity
  (testing "schedule-maintenance does not require a commodity and
            auto-commits when clean"
    (let [s (verified-site-store)
          actor (operation/build s)
          result (exec actor "t-maint"
                       {:op :propose/schedule-maintenance :subject "site-1"}
                       {:actor-id "op-01"})]
      (is (= :commit (:disposition (:state result))))
      (is (= 1 (count (store/ledger s)))))))
