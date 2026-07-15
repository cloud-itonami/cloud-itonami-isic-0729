(ns nonferrousops.sim
  "Non-ferrous metal ore operations simulation and demo driver."
  (:require [nonferrousops.store :as store]
            [nonferrousops.governor :as governor]
            [nonferrousops.nonferrousopsllm :as advisor]
            [nonferrousops.phase :as phase]))

(defn run-proposal
  "Execute a single proposal through the governor."
  [st proposal-request]
  (let [context {:actor-id "nonferrousops-0729"}
        proposal (advisor/mock-advisor context proposal-request)
        verdict (governor/check proposal-request context proposal st)]
    {:request proposal-request
     :proposal proposal
     :verdict verdict
     :phase-advanced (phase/advance-phase :intake verdict)}))

(defn demo
  "Run a simple demonstration of non-ferrous metal ore operations."
  []
  (let [st (store/mem-store)
        ;; Add a verified site
        st (store/add-site st "copper-site-001"
                           {:name "Escondida-style Copper Mine"
                            :jurisdiction :au
                            :verified? true})
        ;; Demo 1: Log production record for a recognized commodity (should pass)
        req1 {:op :propose/log-production
              :subject "copper-site-001"
              :commodity :copper
              :value {:spec-basis "DMIRS mining regulations"}}
        result1 (run-proposal st req1)
        ;; Demo 2: Flag safety concern (should escalate)
        req2 {:op :propose/flag-safety-concern
              :subject "copper-site-001"
              :value {:concern "Tailings dam pressure anomaly detected"
                      :spec-basis "mine safety protocol"}}
        result2 (run-proposal st req2)
        ;; Demo 3: Coordinate shipment of a recognized commodity (should pass)
        req3 {:op :propose/coordinate-shipment
              :subject "copper-site-001"
              :commodity :copper
              :value {:spec-basis "logistics protocol"}}
        result3 (run-proposal st req3)
        ;; Demo 4: Log production for an unrecognized commodity (should HARD block)
        req4 {:op :propose/log-production
              :subject "copper-site-001"
              :commodity :unobtainium
              :value {:spec-basis "DMIRS mining regulations"}}
        result4 (run-proposal st req4)]
    (println "=== Non-Ferrous Metal Ore Operations Demo ===\n")
    (println "1. Log production record (copper):")
    (println "  Verdict OK:" (:ok? (:verdict result1)))
    (println "  Escalate:" (:escalate? (:verdict result1)))
    (println "\n2. Flag safety concern (always escalates):")
    (println "  Verdict OK:" (:ok? (:verdict result2)))
    (println "  Escalate:" (:escalate? (:verdict result2)))
    (println "\n3. Coordinate shipment (copper):")
    (println "  Verdict OK:" (:ok? (:verdict result3)))
    (println "  Escalate:" (:escalate? (:verdict result3)))
    (println "\n4. Log production for unrecognized commodity (HARD block):")
    (println "  Verdict OK:" (:ok? (:verdict result4)))
    (println "  Violations:" (mapv :rule (:violations (:verdict result4))))
    (println "\nDemo complete.")))

;; Entry point for `clojure -M:dev:run`
(defn -main [& _args]
  (demo))
