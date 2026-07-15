(ns nonferrousops.governor-test
  "Governor tests drive ONLY the public `governor/check` entry point (plus
  public `store` helpers) -- never the private per-rule `defn-` violation
  functions inside `nonferrousops.governor`. Reaching into another
  namespace's private vars from a test is exactly the class of mistake
  that broke cloud-itonami-isic-0710's test suite (compile error:
  `governor/forbidden-operation-violations not public`); this suite is
  written to avoid it structurally."
  (:require [clojure.test :refer [deftest is testing]]
            [nonferrousops.governor :as governor]
            [nonferrousops.store :as store]))

(defn- verified-site-store
  "A store with one verified site, ready for coordination proposals."
  []
  (store/add-site (store/mem-store) "site-1" {:verified? true}))

(defn- clean-proposal
  "A proposal that satisfies spec-basis and (optionally) commodity checks."
  ([] (clean-proposal {}))
  ([extra-value]
   {:cites ["source"]
    :value (merge {:spec-basis "rule"} extra-value)
    :confidence 0.9}))

(deftest forbidden-operations-are-blocked
  (testing "Extraction is forbidden"
    (let [st (verified-site-store)
          request {:op :extraction/extract :subject "site-1"}
          verdict (governor/check request {} (clean-proposal) st)]
      (is (true? (:hard? verdict)))
      (is (some #(= :forbidden-operation (:rule %)) (:violations verdict)))))

  (testing "Blasting is forbidden"
    (let [st (verified-site-store)
          request {:op :extraction/blast :subject "site-1"}
          verdict (governor/check request {} (clean-proposal) st)]
      (is (true? (:hard? verdict)))
      (is (some #(= :forbidden-operation (:rule %)) (:violations verdict)))))

  (testing "Mine-safety-authority decisions are forbidden"
    (let [st (verified-site-store)
          request {:op :authority/safety-clearance :subject "site-1"}
          verdict (governor/check request {} (clean-proposal) st)]
      (is (true? (:hard? verdict)))
      (is (some #(= :forbidden-operation (:rule %)) (:violations verdict))))))

(deftest coordination-operations-allowed
  (testing "Log-production with a recognized commodity is clean"
    (let [st (verified-site-store)
          request {:op :propose/log-production :subject "site-1"}
          verdict (governor/check request {} (clean-proposal {:commodity :copper}) st)]
      (is (not (some #(= :forbidden-operation (:rule %)) (:violations verdict))))
      (is (true? (:ok? verdict)))))

  (testing "Coordinate-shipment with a recognized commodity is clean"
    (let [st (verified-site-store)
          request {:op :propose/coordinate-shipment :subject "site-1"}
          verdict (governor/check request {} (clean-proposal {:commodity :lithium}) st)]
      (is (true? (:ok? verdict)))))

  (testing "Schedule-maintenance does not require a commodity"
    (let [st (verified-site-store)
          request {:op :propose/schedule-maintenance :subject "site-1"}
          verdict (governor/check request {} (clean-proposal) st)]
      (is (true? (:ok? verdict))))))

(deftest unknown-commodity-blocks-production-and-shipment
  (testing "Log-production with an unrecognized commodity is a HARD block"
    (let [st (verified-site-store)
          request {:op :propose/log-production :subject "site-1"}
          verdict (governor/check request {} (clean-proposal {:commodity :unobtainium}) st)]
      (is (true? (:hard? verdict)))
      (is (some #(= :unknown-commodity (:rule %)) (:violations verdict)))))

  (testing "Log-production with no commodity at all is a HARD block"
    (let [st (verified-site-store)
          request {:op :propose/log-production :subject "site-1"}
          verdict (governor/check request {} (clean-proposal) st)]
      (is (true? (:hard? verdict)))
      (is (some #(= :unknown-commodity (:rule %)) (:violations verdict)))))

  (testing "Coordinate-shipment with an unrecognized commodity is a HARD block"
    (let [st (verified-site-store)
          request {:op :propose/coordinate-shipment :subject "site-1"}
          verdict (governor/check request {} (clean-proposal {:commodity :iron}) st)]
      (is (true? (:hard? verdict)))
      (is (some #(= :unknown-commodity (:rule %)) (:violations verdict))))))

(deftest safety-concern-escalation
  (testing "Safety concerns always escalate, even fully clean and high-confidence"
    (let [st (verified-site-store)
          request {:op :propose/flag-safety-concern :subject "site-1"}
          verdict (governor/check request {} (clean-proposal) st)]
      (is (some #(= :safety-concern-escalation (:rule %)) (:violations verdict)))
      (is (true? (:escalate? verdict))))))

(deftest site-verification-required
  (testing "Unverified site blocks operations"
    (let [st (store/add-site (store/mem-store) "site-1" {:verified? false})
          request {:op :propose/log-production :subject "site-1"}
          verdict (governor/check request {} (clean-proposal {:commodity :copper}) st)]
      (is (some #(= :site-not-verified (:rule %)) (:violations verdict))))))

(deftest missing-site-record
  (testing "Missing site record blocks operations"
    (let [st (store/mem-store)
          request {:op :propose/log-production :subject "unknown"}
          verdict (governor/check request {} (clean-proposal {:commodity :copper}) st)]
      (is (some #(= :site-record-missing (:rule %)) (:violations verdict))))))

(deftest spec-basis-required
  (testing "Missing cites blocks the proposal"
    (let [st (verified-site-store)
          request {:op :propose/log-production :subject "site-1"}
          proposal {:cites [] :value {:spec-basis "rule" :commodity :copper} :confidence 0.9}
          verdict (governor/check request {} proposal st)]
      (is (some #(= :no-spec-basis (:rule %)) (:violations verdict))))))

(deftest confidence-floor
  (testing "Low confidence escalates even if otherwise clean"
    (let [st (verified-site-store)
          request {:op :propose/log-production :subject "site-1"}
          proposal {:cites ["source"] :value {:spec-basis "rule" :commodity :copper} :confidence 0.4}
          verdict (governor/check request {} proposal st)]
      (is (true? (:escalate? verdict)))
      (is (false? (:hard? verdict))))))
