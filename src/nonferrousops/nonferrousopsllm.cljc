(ns nonferrousops.nonferrousopsllm
  "NonFerrousOps-LLM advisor for non-ferrous metal ore operations
  coordination. The LLM can propose coordination asks (logging,
  scheduling, flagging, shipment coordination) but CANNOT propose
  extraction, blasting, or safety-authority decisions -- those are
  forbidden by the governor."
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(defn mock-advisor
  "Deterministic mock advisor for demo/testing. Returns a safe proposal.
  Defaults to a :copper commodity unless the caller's request already
  names one, so demo/test callers can override via `(:commodity request)`."
  [_context request]
  (let [op (:op request)
        subject (:subject request)
        commodity (:commodity request :copper)]
    {:op op
     :subject subject
     :effect :propose
     :value {:spec-basis "internal-mock"
             :commodity commodity}
     :cites ["mock-advisor"]
     :confidence 0.8}))

(defn parse-edn-proposal
  "Parse EDN proposal from LLM response using the safe EDN reader (not the
  full Clojure reader -- LLM output is untrusted input and must never be
  read with a reader that can invoke `#=` eval or arbitrary constructors).
  Defends against malformed LLM output by returning a low-confidence noop."
  [response-str]
  (try
    (edn/read-string response-str)
    (catch #?(:clj Exception :cljs :default) _e
      {:op :noop
       :confidence 0.0
       :error "Failed to parse proposal EDN"})))

(defn llm-advisor
  "LLM-backed advisor for non-ferrous metal ore operations.
  In real deployment, this would call an LLM to generate proposals.
  For now, delegates to mock-advisor as a placeholder."
  [context request]
  (mock-advisor context request))

(defn advisor
  "Select the appropriate advisor (mock or LLM)."
  [strategy context request]
  (case strategy
    :mock (mock-advisor context request)
    :llm (llm-advisor context request)
    (mock-advisor context request)))
