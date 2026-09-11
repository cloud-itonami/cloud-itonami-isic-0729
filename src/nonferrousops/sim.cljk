(ns nonferrousops.sim
  "Demo driver -- `clojure -M:run` / `clojure -M:dev:run`. Drives the
  REAL compiled `langgraph-clj` `StateGraph` (`nonferrousops.operation/
  build`) end-to-end through a clean auto-commit, a low-confidence
  escalate-then-approve, a low-confidence escalate-then-reject, and the
  HARD-block scenarios (forbidden operation, unknown commodity,
  unverified site, safety-concern flag -- ALL five of
  `nonferrousops.governor/check`'s HARD checks route straight to
  `:hold`, never through human approval, see `nonferrousops.operation`'s
  own docstring), then prints the resulting audit ledger. Mirrors
  `transportops.sim` (cloud-itonami-isic-869).

  `run-proposal`, this namespace's old hand-chained
  `advisor/mock-advisor -> governor/check` pipeline (never touched
  `langgraph.graph` at all), is replaced by driving the real compiled
  graph via `langgraph.graph/run*`."
  (:require [langgraph.graph :as g]
            [nonferrousops.store :as store]
            [nonferrousops.operation :as operation]
            [nonferrousops.governor :as governor]))

(defn scenario [title]
  (println "\n" "=" "=" "=" "=" "=" "=" "=" "=" "=" "=")
  (println (str "Scenario: " title))
  (println "=" "=" "=" "=" "=" "=" "=" "=" "=" "="))

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid by]
  (g/run* actor {:approval {:status :approved :by by}}
          {:thread-id tid :resume? true}))

(defn- reject! [actor tid by]
  (g/run* actor {:approval {:status :rejected :by by}}
          {:thread-id tid :resume? true}))

(defn- verified-copper-site-store []
  (store/add-site (store/mem-store) "copper-site-001"
                  {:name "Escondida-style Copper Mine"
                   :jurisdiction :au
                   :verified? true}))

(defn- low-confidence-advisor
  "A hand-built advisor-fn override producing the SAME shape
  `nonferrousops.nonferrousopsllm/mock-advisor` does, but with
  confidence deliberately below the governor's `confidence-floor`
  (0.6). `nonferrousopsllm/mock-advisor`'s confidence is a fixed 0.8
  (never below the floor), so this demo's real-graph escalate/
  request-approval path -- otherwise unreachable through the actual
  advisor as coded -- is exercised via `operation/build`'s `:advisor-fn`
  injection point instead. Governor policy itself is untouched; this
  is purely a test/demo double for the advisor seam."
  [_context request]
  {:op (:op request)
   :subject (:subject request)
   :effect :propose
   :value {:spec-basis "internal-mock" :commodity (:commodity request :copper)}
   :cites ["mock-advisor"]
   :confidence 0.4})

(defn demo
  "Run the compiled StateGraph through a clean auto-commit, a
  low-confidence escalate-then-approve, a low-confidence
  escalate-then-reject, and the HARD-block scenarios; print each result
  and the final audit ledger."
  []
  (println "Non-Ferrous Metal Ore Mining Operations Coordinator Actor - Demo")

  (scenario "Auto-commit: log production record (copper, verified site)")
  (let [s (verified-copper-site-store)
        actor (operation/build s)
        result (exec-op actor "t1"
                        {:op :propose/log-production :subject "copper-site-001"
                         :commodity :copper}
                        {:actor-id "nonferrousops-0729"})]
    (println (:state result))
    (println "Disposition:" (:disposition (:state result))))

  (scenario "Escalate-then-approve: low-confidence proposal")
  (let [s (verified-copper-site-store)
        actor (operation/build s {:advisor-fn low-confidence-advisor})
        held (exec-op actor "t2"
                      {:op :propose/log-production :subject "copper-site-001"
                       :commodity :copper}
                      {:actor-id "nonferrousops-0729"})]
    (println "Status:" (:status held) "Frontier:" (:frontier held))
    (println "-- operations manager approves --")
    (let [approved (approve! actor "t2" "ops-manager-01")]
      (println (:state approved))
      (println "Disposition:" (:disposition (:state approved)))))

  (scenario "Escalate-then-reject: low-confidence proposal")
  (let [s (verified-copper-site-store)
        actor (operation/build s {:advisor-fn low-confidence-advisor})
        held (exec-op actor "t3"
                      {:op :propose/coordinate-shipment :subject "copper-site-001"
                       :commodity :copper}
                      {:actor-id "nonferrousops-0729"})]
    (println "Status:" (:status held) "Frontier:" (:frontier held))
    (println "-- operations manager rejects --")
    (let [rejected (reject! actor "t3" "ops-manager-01")]
      (println (:state rejected))
      (println "Disposition:" (:disposition (:state rejected)))))

  (scenario "HARD-block: forbidden operation (extraction)")
  (let [s (verified-copper-site-store)
        actor (operation/build s)
        result (exec-op actor "t4"
                        {:op :extraction/extract :subject "copper-site-001"}
                        {:actor-id "nonferrousops-0729"})]
    (println "Disposition:" (:disposition (:state result))
             "Audit:" (:audit (:state result))))

  (scenario "HARD-block: unknown commodity")
  (let [s (verified-copper-site-store)
        actor (operation/build s)
        result (exec-op actor "t5"
                        {:op :propose/log-production :subject "copper-site-001"
                         :commodity :unobtainium}
                        {:actor-id "nonferrousops-0729"})]
    (println "Disposition:" (:disposition (:state result))
             "Audit:" (:audit (:state result))))

  (scenario "HARD-block: unverified site")
  (let [s (store/add-site (store/mem-store) "unverified-site" {:verified? false})
        actor (operation/build s)
        result (exec-op actor "t6"
                        {:op :propose/log-production :subject "unverified-site"
                         :commodity :copper}
                        {:actor-id "nonferrousops-0729"})]
    (println "Disposition:" (:disposition (:state result))
             "Audit:" (:audit (:state result))))

  (scenario "HARD-block: safety-concern flag (a HARD violation in this actor's
             governor as coded -- see nonferrousops.operation's own docstring
             for why this is :hold, not :request-approval)")
  (let [s (verified-copper-site-store)
        actor (operation/build s)
        result (exec-op actor "t7"
                        {:op :propose/flag-safety-concern :subject "copper-site-001"}
                        {:actor-id "nonferrousops-0729"})]
    (println "Disposition:" (:disposition (:state result))
             "Audit:" (:audit (:state result))))

  (scenario "HARD-block: missing spec-basis (governor check, pre-graph)")
  (let [s (verified-copper-site-store)
        proposal {:op :propose/log-production :effect :propose :cites []
                  :value {:commodity :copper} :confidence 0.9}
        verdict (governor/check {:op :propose/log-production :subject "copper-site-001"}
                                 {} proposal s)]
    (println "Violations:" (map :rule (:violations verdict))))

  (println "\n" "=" "=" "=" "=" "=" "=" "=" "=" "=" "=")
  (println "Demo completed successfully")
  (println "=" "=" "=" "=" "=" "=" "=" "=" "=" "="))

(defn -main [& _args]
  (demo))

(comment
  (demo))
