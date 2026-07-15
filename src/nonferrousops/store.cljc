(ns nonferrousops.store
  "Persistent state store for non-ferrous metal ore operations.")

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
    "Record a safety assessment for a site."))

;; MemStore implementation for testing/demo
(deftype MemStore [sites-atom production-atom assessments-atom]
  Store
  (site [_st site-id]
    (@sites-atom site-id))
  (add-site [_st site-id site-data]
    (swap! sites-atom assoc site-id (assoc site-data :id site-id))
    (MemStore. sites-atom production-atom assessments-atom))
  (production-record [_st record-id]
    (@production-atom record-id))
  (add-production-record [_st record-id record-data]
    (swap! production-atom assoc record-id (assoc record-data :id record-id))
    (MemStore. sites-atom production-atom assessments-atom))
  (assessment-of [_st site-id]
    (@assessments-atom site-id))
  (record-assessment [_st site-id assessment]
    (swap! assessments-atom assoc site-id assessment)
    (MemStore. sites-atom production-atom assessments-atom)))

(defn mem-store
  "Create an in-memory store for testing/demo."
  []
  (MemStore. (atom {}) (atom {}) (atom {})))
