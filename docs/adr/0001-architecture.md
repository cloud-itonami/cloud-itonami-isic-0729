# ADR-0001: NonFerrousOps-LLM ⊣ Non-Ferrous Ore Governor architecture

## Status

Accepted. `cloud-itonami-isic-0729` implemented in the `kotoba-lang/industry`
registry as a coordination-only governed actor.

## Context

`cloud-itonami-isic-0729` publishes an OSS business blueprint for non-ferrous
metal ore mining operations coordination (production logging, maintenance
scheduling, safety concern flagging, shipment coordination) covering ISIC
Rev.5 0729 (mining of other non-ferrous metal ores — copper, lithium,
nickel, cobalt, rare-earth elements). Like every prior actor in the
cloud-itonami fleet, the blueprint alone is not an implementation: this ADR
records the governed-actor architecture that promotes it to real, tested
code, following the same langgraph StateGraph + independent Governor +
Phase 0→3 rollout pattern established across the fleet, most directly
`cloud-itonami-isic-0710` (iron ore mining), whose coordination-only scope
boundary this actor directly inherits.

This build is a direct structural port of `cloud-itonami-isic-0710`, adapted
for non-ferrous commodity vocabulary: iron ore's single-grade model is
replaced with a recognized-commodity-type model (`:copper` / `:lithium` /
`:nickel` / `:cobalt` / `:rare-earth`), since ISIC 0729 covers multiple
distinct metal ore types rather than one.

Unlike extraction/blasting verticals, this actor's scope is explicitly
COORDINATION ONLY: proposing scheduling/logging/safety-flagging/shipment
asks, never extraction authority, blasting authority, or mine-safety-
authority decisions. Those are permanently excluded from this actor's
proposal set and handled by specialist verticals or human authorities.

This blueprint's own `:itonami.blueprint/governor` keyword,
`:nonferrous-ore-governor`, is a fresh, domain-distinct governor identity
(distinct from `:iron-ore-governor` used by isic-0710).

## Decision

### Decision 1: fresh governor identity

`:nonferrous-ore-governor` is a distinct governor identity from every prior
actor's governor keyword, following the SAME governed-actor architecture as
every prior actor (most directly isic-0710) but scoped to non-ferrous ore.

### Decision 2: coordination-only scope, permanent exclusion of extraction/blasting/authority

This actor proposes four coordination operations, identical in shape to
isic-0710:
- `:propose/log-production` — ore output/commodity/grade data logging
- `:propose/schedule-maintenance` — equipment maintenance scheduling proposal
- `:propose/flag-safety-concern` — surface mine-safety concern (always escalates)
- `:propose/coordinate-shipment` — outbound ore shipment coordination

Three operations are PERMANENTLY FORBIDDEN and trigger HARD violations
(identical set to isic-0710 — the fleet-wide mining-scope-boundary precedent):
- `:extraction/extract` — actual extraction authority (forbidden)
- `:extraction/blast` — blasting authority (forbidden)
- `:authority/safety-clearance` — mine-safety-authority decisions (forbidden)

Any proposal attempting these is rejected unconditionally; no human override
applies.

### Decision 3: commodity classification — the domain-specific adaptation

Unlike iron ore (a single commodity), ISIC 0729 covers multiple distinct
non-ferrous metal ore types. `:propose/log-production` and
`:propose/coordinate-shipment` proposals MUST name a recognized commodity
(`:copper` `:lithium` `:nickel` `:cobalt` `:rare-earth`); an unrecognized or
missing commodity is a NEW sixth HARD violation (`:unknown-commodity`) not
present in isic-0710 — this actor cannot coordinate ore it cannot classify.
`:propose/schedule-maintenance` and `:propose/flag-safety-concern` do not
require a commodity (maintenance and safety concerns are not necessarily
commodity-specific).

### Decision 4: safety-concern-escalation — cardinal escalation trigger

`:propose/flag-safety-concern` is the only operation that ALWAYS escalates
to human, even if every other governor check passes and confidence is high.
This mirrors isic-0710 and the fleet-wide principle that mine-safety
concerns cannot auto-proceed under any circumstance.

### Decision 5: entity and op shape

The primary entity is a `site` (mine/non-ferrous ore operation). Four ops,
identical shape to isic-0710:
- `:propose/log-production` — directory upsert, no capital risk
- `:propose/schedule-maintenance` — maintenance proposal, no capital risk
- `:propose/flag-safety-concern` — safety escalation, always human
- `:propose/coordinate-shipment` — shipment coordination proposal

All operations require:
1. Spec-basis citation (no invented requirements)
2. Verified site/mine record to exist
3. (log-production / coordinate-shipment only) a recognized commodity
4. Low confidence or safety flags -> escalate

### Decision 6: site verification and jurisdiction checklist

Before any operation, the site/mine record MUST be verified (`:verified? true`).
A known set of jurisdictions (JP, US, AU, BR — the same set as isic-0710,
kept for fleet consistency rather than re-derived per-commodity) each have
required evidence checklists (site record, ore assay report, equipment
safety, certifications, permits, environmental assessment) per jurisdiction.

### Decision 7: no extraction/blasting domain logic needed in this actor

This actor does NOT implement extraction simulation, blasting-safety-clearance
logic, or mine-safety-authority decisions. Those belong to specialist verticals
or human authorities. This actor's domain is coordination: logging, scheduling,
flagging, shipment coordination. The governor enforces this scope boundary by
permanently blocking any proposal that attempts to cross into extraction/authority
territory.

### Decision 8: confidence floor escalation

Low confidence (< 0.6) always escalates to human, even if all other checks pass.
This is the same floor isic-0710 and every prior governor establishes.

### Decision 9: Store protocol, MemStore for dev/tests/demo

`nonferrousops.store/Store` is implemented by `MemStore` (atom-backed, default
for dev/tests/demo). Full `DatomicStore` implementation can be added following
the pattern established by prior siblings.

### Decision 10: mock + LLM advisor pair, safe EDN parsing

`nonferrousops.nonferrousopsllm` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run offline) and
a placeholder for `llm-advisor` (backed by `langchain.model/ChatModel`).
Proposal parsing uses `clojure.edn/read-string` (the safe EDN reader) rather
than the full Clojure reader, since LLM output is untrusted input; a
malformed or adversarial response degrades to a safe low-confidence noop
rather than ever auto-proposing extraction or authority acts. This is a
correction over isic-0710's reference implementation, which used the bare,
unqualified `read-string` (JVM-only and not portable to the `.cljc` file's
declared ClojureScript reader-conditional branch) — caught by this build's
own `clj-kondo` lint pass, kept fixed here rather than ported as-is.

## Alternatives considered

- **An extraction/blasting operation in this actor's proposal set.** Rejected:
  this actor's scope is coordination only. Extraction authority belongs to
  specialist verticals or human authorities with real mine-safety training,
  not to a general operations coordinator.
- **Auto-approval of safety-concern flags.** Rejected: mine-safety concerns
  cannot auto-proceed. Human escalation is non-negotiable.
- **Omitting the spec-basis check.** Rejected: like every prior actor, proposals
  must cite official sources, never invent requirements.
- **Omitting the commodity-classification check.** Rejected: unlike iron ore,
  ISIC 0729 spans multiple distinct metal ore types with different downstream
  supply chains (battery metals vs. rare-earth magnets); coordinating an
  unclassified ore stream is out of scope for a general operations coordinator.
- **Reusing isic-0710's `ore-grade-survey` evidence keyword verbatim.**
  Renamed to `:ore-assay-report` — "assay" is the standard term of art for
  ore-composition testing in non-ferrous/battery-metal mining; kept the
  remaining evidence keywords unchanged for fleet consistency.

## Consequences

- New actor in the cloud-itonami fleet, coordination-only, mirroring
  isic-0710's scope-boundary precedent for mining-ops actors.
- Establishes commodity classification as a governor-enforced HARD check,
  a pattern future multi-commodity mining actors can reuse.
- Always-escalate rule on safety-concern flags enforces the principle that
  mine-safety cannot auto-proceed.
- 16 tests / 61 assertions pass; lint is clean (0 errors, 0 warnings); the
  demo walks three coordination lifecycles (production logging, safety
  escalation, shipment coordination) and one forbidden/unknown-commodity
  block.
- Learned directly from isic-0710's own revert history (registry.edn
  comment: "test file references a private var
  (governor/forbidden-operation-violations not public) -- compile error"):
  this build's governor test suite exercises ONLY the public
  `governor/check` entry point and public `store` helpers, never reaching
  into `nonferrousops.governor`'s private per-rule `defn-` functions.

## References

- `cloud-itonami-isic-0710/docs/adr/0001-architecture.md` (direct template,
  origin of the coordination-only mining-ops scope-boundary reasoning)
- Federal Mine Safety and Health Act (Mine Act), 30 U.S.C. §801 et seq. (US)
- Work Health and Safety (Prevention of Serious Injury from Hazardous Energy)
  Regulation 2009 (AU)
- Agência Nacional de Mineração (ANM) regulations (Brazil)
- 鉱山保安法 (Mine Safety Act) (Japan)
