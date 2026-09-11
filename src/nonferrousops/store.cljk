(ns nonferrousops.store
  "Persistent state store for non-ferrous metal ore operations.

  The append-only audit ledger (`ledger`/`append-ledger!`) is this
  actor's core missing plumbing before this fix: no audit ledger
  existed ANYWHERE in this codebase (not even an aspirational stub).
  Every committed/held/approval-rejected decision fact from
  `nonferrousops.operation`'s compiled StateGraph now lands here, so an
  operation's decision history is always a query over an immutable log
  -- the same discipline every sibling `cloud-itonami-isic-*` actor's
  ledger provides. The ledger stays append-only.")

(defprotocol Store
  "State store contract for non-ferrous metal ore operations."
  (site [st site-id]
    "Retrieve a site/mine record by ID. Returns nil if not found.")
  (add-site [st site-id site-data]
    "Add or update a site/mine record.")
  (production-record [st record-id]
    "Retrieve a production record by ID.")
  (add-production-record [st record-id record-data]
    "Add a production record.")
  (assessment-of [st site-id]
    "Get the safety assessment for a site.")
  (record-assessment [st site-id assessment]
    "Record a safety assessment for a site.")
  (ledger [st]
    "The append-only audit ledger: every committed/held/approval-
    rejected decision fact, in append order.")
  (append-ledger! [st fact]
    "Append one immutable decision fact to the ledger. Returns the
    fact. Genuinely wired into `nonferrousops.operation`'s
    `:commit`/`:hold`/`:request-approval` graph nodes -- not test-only
    plumbing."))

;; MemStore implementation for testing/demo
(deftype MemStore [sites-atom production-atom assessments-atom ledger-atom]
  Store
  (site [_st site-id]
    (@sites-atom site-id))
  (add-site [_st site-id site-data]
    (swap! sites-atom assoc site-id (assoc site-data :id site-id))
    (MemStore. sites-atom production-atom assessments-atom ledger-atom))
  (production-record [_st record-id]
    (@production-atom record-id))
  (add-production-record [_st record-id record-data]
    (swap! production-atom assoc record-id (assoc record-data :id record-id))
    (MemStore. sites-atom production-atom assessments-atom ledger-atom))
  (assessment-of [_st site-id]
    (@assessments-atom site-id))
  (record-assessment [_st site-id assessment]
    (swap! assessments-atom assoc site-id assessment)
    (MemStore. sites-atom production-atom assessments-atom ledger-atom))
  (ledger [_st] @ledger-atom)
  (append-ledger! [_st fact]
    (swap! ledger-atom conj fact)
    fact))

(defn mem-store
  "Create an in-memory store for testing/demo."
  []
  (MemStore. (atom {}) (atom {}) (atom {}) (atom [])))
