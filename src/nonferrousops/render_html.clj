(ns nonferrousops.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300):
  drives the REAL actor stack
  (`nonferrousops.operation` -> `nonferrousops.governor` ->
  `nonferrousops.store`) through a scenario adapted from this repo's own
  `nonferrousops.sim` demo driver (`clojure -M:dev:run`) -- a clean
  copper production-log auto-commit on a verified site, a low-confidence
  escalate-then-approve, a low-confidence escalate-then-reject, and the
  HARD holds this governor actually produces (forbidden extraction,
  unknown commodity, unverified site, safety-concern flag). Every field
  read by `render` is real governor/store output, not a hand-typed copy.
  Deterministic: no invented numbers, no timestamps in page content.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [nonferrousops.store :as store]
            [nonferrousops.operation :as op]
            [langgraph.graph :as g]))

(def ^:private actor-ctx {:actor-id "nonferrousops-0729"})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context actor-ctx} {:thread-id tid}))

(defn- approve! [actor tid by]
  (g/run* actor {:approval {:status :approved :by by}}
          {:thread-id tid :resume? true}))

(defn- reject! [actor tid by]
  (g/run* actor {:approval {:status :rejected :by by}}
          {:thread-id tid :resume? true}))

(defn- low-confidence-advisor
  "Same shape as `nonferrousops.nonferrousopsllm/mock-advisor` but with
  confidence deliberately below the governor's `confidence-floor` (0.6)
  so the real graph's escalate / request-approval path is exercised.
  Governor policy itself is untouched."
  [_context request]
  {:op (:op request)
   :subject (:subject request)
   :effect :propose
   :value {:spec-basis "internal-mock" :commodity (:commodity request :copper)}
   :cites ["mock-advisor"]
   :confidence 0.4})

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing dispositions this
  actor can reach. Returns the resulting store."
  []
  (let [db (-> (store/mem-store)
               (store/add-site "copper-site-001"
                               {:name "Escondida-style Copper Mine"
                                :jurisdiction :au
                                :verified? true})
               (store/add-site "lithium-site-001"
                               {:name "Atacama Lithium Brine Field"
                                :jurisdiction :cl
                                :verified? true})
               (store/add-site "unverified-site"
                               {:name "Pending Verification Prospect"
                                :jurisdiction :br
                                :verified? false}))
        ;; Default mock-advisor path (confidence 0.8, auto-commit when clean)
        actor (op/build db)
        ;; Low-confidence advisor for the soft-escalate paths only
        low-actor (op/build db {:advisor-fn low-confidence-advisor})]

    ;; --- Auto-commit: clean copper production log on verified site ---
    (exec! actor "t1"
           {:op :propose/log-production
            :subject "copper-site-001"
            :commodity :copper})

    ;; --- Escalate-then-approve: low-confidence log-production ---
    (exec! low-actor "t2"
           {:op :propose/log-production
            :subject "copper-site-001"
            :commodity :copper})
    (approve! low-actor "t2" "ops-manager-01")

    ;; --- Escalate-then-reject: low-confidence shipment coord ---
    (exec! low-actor "t3"
           {:op :propose/coordinate-shipment
            :subject "lithium-site-001"
            :commodity :lithium})
    (reject! low-actor "t3" "ops-manager-01")

    ;; --- HARD holds (never reach a human) ---
    (exec! actor "t4"
           {:op :extraction/extract :subject "copper-site-001"})

    (exec! actor "t5"
           {:op :propose/log-production
            :subject "copper-site-001"
            :commodity :unobtainium})

    (exec! actor "t6"
           {:op :propose/log-production
            :subject "unverified-site"
            :commodity :copper})

    (exec! actor "t7"
           {:op :propose/flag-safety-concern
            :subject "copper-site-001"})

    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- hold-rule [f]
  (or (some-> f :basis first)
      (some-> f :violations first :rule)))

(defn- last-fact-for [ledger sid]
  (last (filter #(= (:subject %) sid) ledger)))

(defn- status-cell [ledger sid]
  (let [f (last-fact-for ledger sid)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f))
      (if (:approved-by f)
        "<span class=\"ok\">approved &amp; committed</span>"
        "<span class=\"ok\">committed</span>")
      (= :governor-hold (:t f))
      (str "<span class=\"critical\">HARD hold: "
           (esc (name (or (hold-rule f) :unknown))) "</span>")
      (= :approval-rejected (:t f))
      (str "<span class=\"warn\">approval rejected"
           (when-let [by (:by f)] (str " by " (esc by)))
           "</span>")
      (= :approval-requested (:t f))
      "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- ledger-row [{:keys [t op subject disposition basis approved-by by]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t))
          (esc (name (or op :n-a)))
          (esc subject)
          (esc (or (some->> basis (map name) (str/join ", "))
                   (some-> disposition name)
                   (when approved-by (str "by " approved-by))
                   (when by (str "by " by))
                   ""))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own closed op contract
  ;; (governor + sim). Documentation of fixed behavior, not runtime
  ;; telemetry, so legitimately hand-described.
  ["        <tr><td><code>:propose/log-production</code></td><td><span class=\"ok\">auto-commit when clean + verified site + known commodity</span></td></tr>"
   "        <tr><td><code>:propose/coordinate-shipment</code></td><td><span class=\"ok\">auto-commit when clean; commodity required</span></td></tr>"
   "        <tr><td><code>:propose/schedule-maintenance</code></td><td><span class=\"warn\">verified-site required; else HARD block</span></td></tr>"
   "        <tr><td><code>:propose/flag-safety-concern</code></td><td><span class=\"critical\">HARD always-hold (safety-concern-escalation; never interactive)</span></td></tr>"
   "        <tr><td><code>:extraction/extract</code> / <code>:extraction/blast</code> / <code>:authority/safety-clearance</code></td><td><span class=\"critical\">HARD forbidden (coordination only; extraction out of scope)</span></td></tr>"
   "        <tr><td>unknown commodity / unverified site / no-spec-basis</td><td><span class=\"critical\">HARD hold (cannot be overridden by human)</span></td></tr>"
   "        <tr><td>low confidence (&lt; 0.6)</td><td><span class=\"warn\">soft escalate → human approval / rejection</span></td></tr>"])

(def ^:private site-ids
  ["copper-site-001" "lithium-site-001" "unverified-site"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        srow (fn [sid]
               (let [s (store/site db sid)]
                 (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
                         (esc sid)
                         (esc (or (:name s) "(missing)"))
                         (esc (str (or (:jurisdiction s) :n-a)))
                         (esc (if (true? (:verified? s)) "verified" "unverified"))
                         (status-cell ledger sid))))
        site-rows (str/join "\n" (map srow site-ids))
        ledger-rows (str/join "\n" (map ledger-row ledger))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-0729 &middot; non-ferrous ore mining</title>"
     "<style>body{font:14px/1.5 sans-serif;margin:0;color:#1a1a1a;background:#f5f5f5}"
     ".bar{background:#1a3a2a;color:#fff;padding:1.2rem 2rem}.bar h1{margin:0;font-size:1.15rem}"
     "main{max-width:980px;margin:1.5rem auto;padding:0 1rem}"
     ".card{background:#fff;border-radius:8px;padding:1.2rem 1.4rem;margin-bottom:1.2rem;box-shadow:0 1px 3px rgba(0,0,0,.08)}"
     ".muted{color:#777;font-size:.82rem}table{border-collapse:collapse;width:100%;font-size:.85rem}"
     "th,td{text-align:left;padding:.42rem .5rem;border-bottom:1px solid #eee}th{font-weight:600;color:#555}"
     ".ok{color:#0a7d33}.warn{color:#9a6700}.critical{color:#b41010;font-weight:600}"
     "code{background:#f0f0f0;padding:.1rem .3rem;border-radius:3px;font-size:.8rem}</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Non-ferrous metal ore mining ops (ISIC 0729) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · coordination only</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Sites / mines</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>nonferrousops.store</code> via <code>nonferrousops.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly. No invented data.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Site</th><th>Name</th><th>Jurisdiction</th><th>Verified</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     site-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Non-Ferrous Ore Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden. This actor coordinates production logging, maintenance scheduling, safety-concern flagging and shipment coordination — never extraction, blasting, or mine-safety-authority decisions.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log from <code>nonferrousops.store/ledger</code> — every commit, hold and approval-rejected fact this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Subject</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)
        out-file (java.io.File. out)]
    (some-> (.getParentFile out-file) .mkdirs)
    (spit out-file html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts )")))
