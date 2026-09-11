(ns nonferrousops.export
  "Cross-actor supply-chain-linkage export (ADR-2607999980, DIRECT PORT
  of ADR-2607999970's `ironops.export` pattern -- `cloud-itonami-
  isic-0729` non-ferrous metal ore mining -> `cloud-itonami-isic-2610`
  semiconductor/electronics fab, the origin hop of the smartphone
  chain: isic-0729 -> isic-2610 -> isic-2630). `pedigree-for-
  production-record` is this actor's own `ironops.export/pedigree-
  for-production-record`/`steelworks.export/pedigree-for-heat`/
  `autoparts.export/pedigree-for-part-lot` equivalent -- same shape,
  same `kotoba.pedigree` contract, same 'pure data transform over
  data already on file, never a live network call and never an
  invented claim' discipline every prior export fn in this fleet
  already establishes.

  THIS IS ANOTHER LINK IN THE CHAIN WHOSE EVIDENTIARY BASIS IS NOT A
  PHYSICS SIMULATION -- disclosed honestly, the SAME distinction
  `ironops.export`'s own docstring already draws for iron-ore mining,
  now applied to non-ferrous metal ore mining. `cloud-itonami-
  isic-2610`/`cloud-itonami-isic-2630` both build (or will build)
  their own downstream pedigrees from REAL `physics-2d` time-stepped
  rigid-body simulation readings -- fully reproducible, deterministic,
  re-runnable from the same inputs to the identical number.
  `cloud-itonami-isic-0729` runs NO physics simulation at all: it is a
  COORDINATION-ONLY actor (mirrors `cloud-itonami-isic-0710`'s own
  scope) -- `nonferrousops.governor`'s `forbidden-ops` permanently
  excludes extraction, blasting and mine-safety-authority decisions
  from its proposal set; this actor never models the physical
  extraction/ore-processing process itself. Its evidentiary basis is
  instead a LOGGED, ON-FILE PRODUCTION RECORD: an ore-grade survey
  reading and a measured tonnage a mine operator recorded via
  `nonferrousops.store/add-production-record`, the SAME kind of
  ground-truth operational data `nonferrousops.facts/production-
  grade-valid?` already independently re-verifies against the
  record's own recorded `:grade-min`/`:grade-max` bounds.

  This is a genuinely WEAKER kind of evidentiary basis than a
  deterministic physics simulation, and this namespace says so
  plainly rather than letting the shared `kotoba.pedigree` shape
  imply parity it does not have: a physics-2d simulation is fully
  reproducible from its inputs alone; a production record's
  `:grade-actual`/`:quantity-tonnes` are themselves human/instrument-
  reported measurements this actor did not itself generate or
  re-derive -- it only packages what is already on file, exactly once,
  never re-measuring or re-simulating it.

  A SECOND, DISTINCT honesty caveat this namespace ALSO discloses
  (absent from `ironops.export`'s own docstring, because iron-ore
  mining is a single-commodity vertical): `nonferrousops.facts/
  commodities` covers FIVE structurally different non-ferrous
  commodities (copper, lithium, nickel, cobalt, rare-earth), each with
  its own conventional grade-reporting basis and typical numeric
  range in real mining practice (e.g. copper ore/concentrate grade in
  %Cu, lithium spodumene concentrate commonly reported as %Li2O,
  rare-earth ore commonly reported as %REO or ppm of specific
  elements). `kotoba.pedigree/claim`'s `:pedigree/claims` map has NO
  slot for commodity identity -- every claim value must be a bare
  number (see that ns's own docstring) -- so `:grade-actual` here is
  reported EXACTLY as recorded on the production record, with NO unit
  conversion or cross-commodity normalization attempted by this fn.
  A downstream actor's governor comparing this claim against its own
  acceptance floor is therefore comparing raw grade percentages that
  are NOT directly comparable across different commodities unless
  BOTH sides already agree on which commodity is in play (folded into
  `:pedigree/evidence-basis` as a citation, see below, but NOT a
  `:pedigree/claims` key) -- an honesty caveat this fn's docstring
  states plainly rather than letting the shared numeric-claim shape
  imply a false cross-commodity comparability `kotoba.pedigree`
  itself makes no promise about either way.

  A downstream actor's governor independently re-verifies the shape
  and the claim value against ITS OWN disclosed acceptance floor --
  the same 'ground truth, not self-report' discipline this fleet
  applies everywhere else, but it can only re-verify the NUMBER on
  file, never re-run a measurement this actor never took itself
  either. `kotoba.pedigree` itself makes no claim about HOW a number
  was obtained (see that ns's docstring: 'this library does not know
  what any particular claim key means'); the honesty obligation is on
  THIS export fn's evidence-basis citation and docstring, not on the
  shared schema."
  (:require [kotoba.pedigree :as pedigree]))

(defn pedigree-for-production-record
  "Builds a `kotoba.pedigree` record for `production-record`, a
  production record ALREADY on file via `nonferrousops.store/add-
  production-record` (ADR-2607999980, direct port of ADR-2607999970's
  `ironops.export/pedigree-for-production-record`). Unlike a
  downstream physics-simulation-backed export fn, this fn does NOT
  package a physics-simulation reading -- see this ns's own docstring
  for the honest disclosure of that difference (and this vertical's
  own additional multi-commodity comparability caveat). It only
  packages the recorded ore-grade survey and tonnage, mirroring how
  every other export fn in this fleet only ever materializes a
  package body over data already on file, never computes new
  evidence.

  `:pedigree/claims` reports TWO numeric fields, both already
  treated as this actor's ground-truth production data by existing
  code, never invented for this export fn:

    - `:grade-actual` -- the ore-grade survey reading (commodity
      concentration, e.g. percent Cu or percent Li2O -- see
      `nonferrousops.facts/commodities`), the same field
      `nonferrousops.facts/production-grade-valid?` independently
      re-verifies against the record's own recorded `:grade-min`/
      `:grade-max`, and `nonferrousops.registry/ore-grade-matches-
      claim?` cross-checks against `:claimed-grade`.
    - `:quantity-tonnes` -- the measured tonnage, the same field
      `nonferrousops.registry/compute-production-value` already uses
      as ground truth.

  `:site-id`/`:commodity` (strings/keywords, when present) are NOT
  included in `:pedigree/claims` -- `kotoba.pedigree/valid?` requires
  every claim value to be numeric, never a self-reported string/
  keyword/id; they are instead folded into `:pedigree/evidence-basis`
  as citations, and `:pedigree/subject-lot-id` is the production
  record's own `:id` (the specific extraction/lot record this
  pedigree is about, mirroring a heat's `:id`/a part-lot's `:id`/an
  isic-0710 production record's own `:id` in every prior link of this
  pattern).

  `issued-at` (an ISO date string) is a caller-supplied argument, not
  a wall-clock read -- pure/deterministic, the same discipline every
  sibling `pedigree-for-*` fn already establishes.

  This fn does NOT gate on `nonferrousops.facts/production-grade-
  valid?` itself (whether the grade clears its OWN recorded min/max
  bounds) -- that is a separate, ALREADY-independently-checked fact
  about the record's internal consistency, not this fn's job; and it
  does NOT gate on whether the grade would clear any DOWNSTREAM
  actor's acceptance floor either -- that acceptance-threshold
  judgment belongs entirely to the downstream actor's own governor
  (`kotoba.pedigree`'s own library-wide contract, see that ns's
  docstring), mirroring how every sibling `pedigree-for-*` fn
  packages its own telemetry/record unconditionally rather than
  pre-judging whether it would clear some downstream floor.

  Returns nil (never a fabricated pedigree) when `production-record`
  carries no real `:id`, or no numeric `:grade-actual`/`:quantity-
  tonnes` on file -- the SAME disclosed 'missing data != inventable'
  discipline every sibling export fn in this fleet already
  establishes."
  [{:keys [id site-id commodity grade-actual quantity-tonnes]} issued-at]
  (when (and id (number? grade-actual) (number? quantity-tonnes))
    (pedigree/claim
     (str "PEDIGREE-" id) id "cloud-itonami-isic-0729"
     {:grade-actual grade-actual :quantity-tonnes quantity-tonnes}
     :evidence-basis
     [(str "nonferrousops.store/production-record"
           (when site-id (str " (site " site-id ")"))
           (when commodity (str " (commodity " (name commodity) ")"))
           " -- logged, on-file ore-grade survey + tonnage record, "
           "human/instrument-verified operational data, NOT a "
           "physics-2d simulation reading; this actor is coordination-"
           "only and runs no physics simulation of its own, see ns "
           "docstring")]
     :issued-at issued-at)))
