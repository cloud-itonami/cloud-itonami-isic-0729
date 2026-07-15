(ns nonferrousops.facts
  "Non-ferrous metal ore mining domain facts and verification.")

(def commodities
  "Recognized non-ferrous metal ore commodity types this actor coordinates
  (ISIC Rev.5 0729 -- mining of other non-ferrous metal ores). Copper,
  lithium, nickel, and cobalt are the primary battery/electronics supply
  chain inputs; rare-earth covers the broader rare-earth-element group
  (neodymium, dysprosium, etc.) mined as a single ore stream."
  #{:copper :lithium :nickel :cobalt :rare-earth})

(defn known-commodity?
  "Is this a recognized non-ferrous ore commodity?"
  [commodity]
  (boolean (commodities commodity)))

(def jurisdictions
  "Registered mine-safety jurisdictions with their audit checklists."
  {
   :jp {:name "Japan (METI Industrial Safety Group)"
        :required-evidence [:site-record :ore-assay-report :equipment-safety :personnel-certification]}
   :us {:name "United States (MSHA)"
        :required-evidence [:site-record :ore-assay-report :equipment-safety :permit-valid]}
   :au {:name "Australia (DMIRS)"
        :required-evidence [:site-record :ore-assay-report :equipment-safety :environmental-assessment]}
   :br {:name "Brazil (ANM)"
        :required-evidence [:site-record :ore-assay-report :equipment-safety :environmental-assessment]}
   })

(defn known-jurisdiction?
  "Is this a registered jurisdiction?"
  [jurisdiction]
  (boolean (jurisdictions jurisdiction)))

(defn required-evidence-for
  "Get the required evidence checklist for a jurisdiction."
  [jurisdiction]
  (:required-evidence (jurisdictions jurisdiction) []))

(defn required-evidence-satisfied?
  "Are all required evidence items present for a jurisdiction?
  Evidence is a set of flags/records from the assessment."
  [jurisdiction evidence-set]
  (let [required (required-evidence-for jurisdiction)]
    (every? (fn [ev] (contains? evidence-set ev)) required)))

(defn production-grade-valid?
  "Verify ore grade (commodity concentration, e.g. % copper or ppm rare-earth
  oxide) is within recorded bounds."
  [grade-actual grade-min grade-max]
  (and (>= grade-actual grade-min)
       (<= grade-actual grade-max)))
