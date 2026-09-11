(ns nonferrousops.operation
  "OperationActor -- one non-ferrous metal ore mining operations
  coordination request = one supervised actor run, expressed as a REAL
  compiled `langgraph-clj` `StateGraph` (`langgraph.graph/state-graph` +
  `compile-graph`). The advisor (NonFerrousOps-LLM) is sealed into a
  single node (`:advise`); its proposal is ALWAYS routed through the
  independent Non-Ferrous Ore Governor
  (`nonferrousops.governor/check`, `:govern`) before anything commits
  to the SSoT.

  This namespace did not exist before this fix. The file previously
  named `operation.cljc` was NOT a StateGraph builder at all -- it was
  a static ops-metadata table (`operations`/`valid-operation?`/
  `operation-info`, a plain lookup map with no state-transition logic
  whatsoever, grep-verified unused by any other namespace in this
  repo). `nonferrousops.sim`'s `run-proposal` hand-chained
  `advisor/mock-advisor -> governor/check` directly with plain function
  calls -- a fake StateGraph, arguably worse than the deferred-stub
  pattern in some sibling repos since no StateGraph was even attempted
  here (zero `require` of `langgraph.graph`, zero use of
  `state-graph`/`add-node`/`compile-graph` anywhere in `src/`), despite
  `deps.edn` already correctly declaring the real
  `io.github.kotoba-lang/langgraph` dependency in its top-level `:deps`
  map (this repo did NOT have isic-0143's deferred-dependency problem
  -- the dependency was real and reachable, nothing in `src/` ever
  called it). No audit ledger existed anywhere in the codebase either,
  and there was no CI workflow.

  The old static ops-metadata table's role is superseded, not
  preserved: this actor's closed proposal-op allowlist is already
  independently enforced by `nonferrousops.governor`'s own
  `forbidden-ops` HARD block and by `nonferrousops.nonferrousopsllm`'s
  own op dispatch -- the removed table was descriptive-only metadata
  with no caller anywhere in this repo and duplicated, rather than fed,
  that enforcement. It is not reintroduced.

  State machine:
  intake -> advise -> govern -> decide -+-> commit
                                         +-> request-approval -> commit
                                         +-> hold

  Everything the actor depends on is injected, so each is a swap, not a
  rewrite:
    - the Store    (`nonferrousops.store/mem-store`, or any `Store`
                     impl)
    - the Advisor  (mock today via `nonferrousops.nonferrousopsllm/
                     advisor`'s `:mock`/`:llm` strategy dispatch --
                     already the injection point)

  One graph run = one non-ferrous ore operations coordination request.
  No unbounded inner loop -- each run is auditable and checkpointed.
  Every commit/hold/approval-rejected decision fact lands in
  `nonferrousops.store`'s append-only ledger (`store/append-ledger!`,
  new in this fix -- no ledger existed anywhere in this repo before),
  reachable from the `:commit`, `:hold`, and `:request-approval`
  (rejection) nodes.

  `nonferrousops.governor`'s six HARD checks and
  `nonferrousops.nonferrousopsllm`'s advisor dispatch are reused
  UNCHANGED -- this fix only wires the existing domain policy into a
  real compiled graph and a real ledger, it does not redesign the
  non-ferrous-ore compliance rules. IMPORTANT, and preserved EXACTLY as
  `nonferrousops.governor/check` actually computes it (not as its
  prose comment alone might be read): `check`'s `:hard?` flag is
  `true` whenever ANY of the FIVE violation-producing checks folded
  into its `hard` vector fires -- forbidden-operation, no-spec-basis,
  site-record-missing/not-verified, unknown-commodity, AND
  `:propose/flag-safety-concern`'s `safety-concern-escalation-
  violations`. All five are therefore HARD violations in the code as
  written: they route straight to `:hold` in this graph, THE SAME as
  every other hard violation, NEVER through `:request-approval` --
  matching this governor's own docstring header verbatim (\"ALL HARD
  violations ... a human approver CANNOT override them\"). Only LOW
  CONFIDENCE alone (`check`'s `low?`, computed separately and NOT
  folded into `hard`) produces `:hard? false` with `:escalate? true`,
  and is the only path that reaches `:request-approval`. This fix does
  not change that classification (a safety-concern flag being
  un-overridable rather than merely escalated-for-review is a property
  of the existing `governor.cljc` code, not something introduced
  here) -- it wires the graph to respect `nonferrousops.governor`'s
  actual output exactly as it already computes it.

  `nonferrousops.phase`'s `advance-phase` (a per-run workflow-STAGE
  tracker -- intake -> verify -> propose -> resolved/hold -- distinct
  from sibling actors' 0->3 autonomy-rollout `phase` namespaces) is
  reused UNCHANGED and now genuinely surfaced: the `:decide` node calls
  it exactly as `nonferrousops.sim`'s old `run-proposal` did
  (`(phase/advance-phase :intake verdict)`), and the result is recorded
  as `:workflow-phase` on every decision audit fact instead of being
  computed and immediately discarded (the old `run-proposal` returned
  it as `:phase-advanced` in its result map but nothing ever read or
  persisted it).

  Human-in-the-loop = real approval workflow:
  `interrupt-before #{:request-approval}` pauses the actor at the
  `:request-approval` node until a human operations manager resumes it
  with a decision."
  (:require [langgraph.graph :as g]
            [langgraph.checkpoint :as cp]
            [nonferrousops.nonferrousopsllm :as advisor]
            [nonferrousops.governor :as governor]
            [nonferrousops.phase :as phase]
            [nonferrousops.store :as store]))

;; ============================================================================
;; Audit-fact builders
;; ============================================================================

(defn- commit-fact
  "The audit fact written when a proposal commits. `:proposal` carries
  the full advisor proposal (production/maintenance/shipment
  coordination data, spec-basis citations) -- nonferrousops has no
  separate stateful commit-record! entity beyond the site/production/
  assessment directories, so the ledger fact itself is the durable
  record of what happened."
  [request context proposal approval workflow-phase]
  (cond-> {:t              :committed
           :op             (:op request)
           :actor          (:actor-id context)
           :subject        (:subject request)
           :disposition    :commit
           :basis          (:cites proposal)
           :proposal       proposal
           :workflow-phase workflow-phase}
    approval (assoc :approved-by (:by approval))))

;; ============================================================================
;; Compiled StateGraph
;; ============================================================================

(defn build
  "Compiles an OperationActor graph bound to `store`. opts:
    :advisor-strategy -- `:mock` | `:llm`, passed to
                          `nonferrousops.nonferrousopsllm/advisor`
                          (default: `:mock`)
    :advisor-fn        -- optional `(fn [context request] proposal)`
                          override for the `:advise` node, bypassing
                          `advisor-strategy` entirely (default: nil,
                          meaning `(partial nonferrousopsllm/advisor
                          advisor-strategy)`). This is the injection
                          seam `nonferrousops.sim` uses to exercise the
                          real graph's escalate/`:request-approval` path
                          in tests/demo -- `nonferrousopsllm/mock-advisor`
                          always returns a fixed 0.8 confidence (never
                          below the governor's floor), so a genuine
                          low-confidence run is otherwise unreachable
                          through the actual advisor as coded. Governor
                          policy is untouched by this seam.
    :checkpointer      -- a `langgraph.checkpoint/Checkpointer`
                          (default: in-memory `cp/mem-checkpointer`)

  The compiled graph's input map: `{:request .. :context ..}` --
  `context` carries `:actor-id` (matching the old `run-proposal`'s
  hard-coded `{:actor-id \"nonferrousops-0729\"}`, now a caller-supplied
  value)."
  [store & [{:keys [advisor-strategy advisor-fn checkpointer]
             :or   {advisor-strategy :mock
                    checkpointer     (cp/mem-checkpointer)}}]]
  (-> (g/state-graph
       {:channels
        {:request        {:default nil}
         :context        {:default {}}
         :proposal       {:default nil}
         :verdict        {:default nil}
         :disposition    {:default nil}
         :workflow-phase {:default nil}
         :approval       {:default nil}
         :audit          {:reducer into :default []}}})

      (g/add-node :intake (fn [s] s))

      (g/add-node :advise
        (fn [{:keys [request context]}]
          {:proposal ((or advisor-fn (partial advisor/advisor advisor-strategy))
                      context request)}))

      (g/add-node :govern
        (fn [{:keys [request context proposal]}]
          {:verdict (governor/check request context proposal store)}))

      (g/add-node :decide
        (fn [{:keys [request context verdict]}]
          (let [{:keys [hard? escalate?]} verdict
                wp (phase/advance-phase :intake verdict)]
            (cond
              ;; HARD governor violations are a permanent block -- NEVER
              ;; routed through human approval, straight to :hold. This
              ;; includes :propose/flag-safety-concern, whose own
              ;; safety-concern-escalation-violations is folded into
              ;; `hard` by `governor/check` -- see this ns's own
              ;; docstring.
              hard?
              {:disposition    :hold
               :workflow-phase wp
               :audit [(assoc (governor/hold-fact request context verdict)
                              :workflow-phase wp)]}

              ;; escalate? with hard? false means low confidence alone
              ;; (the only soft-escalation path `governor/check`
              ;; produces) -- routes through real human sign-off.
              escalate?
              {:disposition    :escalate
               :workflow-phase wp
               :audit [{:t              :approval-requested
                        :op             (:op request)
                        :actor          (:actor-id context)
                        :subject        (:subject request)
                        :reason         :low-confidence
                        :confidence     (:confidence verdict)
                        :workflow-phase wp}]}

              :else
              {:disposition    :commit
               :workflow-phase wp}))))

      (g/add-node :request-approval
        (fn [{:keys [request context approval verdict workflow-phase]}]
          (if (= :approved (:status approval))
            {:disposition :commit
             :audit [{:t       :approval-granted
                      :op      (:op request)
                      :actor   (:actor-id context)
                      :subject (:subject request)
                      :by      (:by approval)}]}
            {:disposition :hold
             :audit [(assoc (governor/hold-fact request context verdict)
                            :t :approval-rejected
                            :workflow-phase workflow-phase
                            :by (:by approval))]})))

      (g/add-node :commit
        (fn [{:keys [request context proposal approval workflow-phase]}]
          (let [f (commit-fact request context proposal approval workflow-phase)]
            (store/append-ledger! store f)
            {:audit [f]})))

      (g/add-node :hold
        (fn [{:keys [audit]}]
          (when-let [hf (last (filter #(#{:governor-hold :approval-rejected} (:t %)) audit))]
            (store/append-ledger! store hf))
          {}))

      (g/set-entry-point :intake)
      (g/add-edge :intake :advise)
      (g/add-edge :advise :govern)
      (g/add-edge :govern :decide)

      (g/add-conditional-edges :decide
        (fn [{:keys [disposition]}]
          (case disposition
            :commit   :commit
            :escalate :request-approval
            :hold)))

      (g/add-conditional-edges :request-approval
        (fn [{:keys [disposition]}]
          (if (= :commit disposition) :commit :hold)))

      (g/set-finish-point :commit)
      (g/set-finish-point :hold)

      (g/compile-graph
       {:checkpointer     checkpointer
        :interrupt-before #{:request-approval}})))
