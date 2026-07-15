# cloud-itonami-isic-0729

**Non-Ferrous Metal Ore Mining Operations Coordinator** — an OSS business
blueprint and governed-actor implementation for non-ferrous metal ore mining
operations (ISIC Rev.5 0729: mining of other non-ferrous metal ores —
copper, lithium, nickel, cobalt, rare-earth elements).

## Overview

`cloud-itonami-isic-0729` is an independent LLM-backed operations coordinator
for non-ferrous metal ore mining, implementing the governed-actor pattern: an
LLM advisor (NonFerrousOps-LLM) behind an independent Governor (Non-Ferrous
Ore Governor) that enforces domain compliance.

The actor proposes four coordination operations:
- **Log Production Record** — ore output, commodity type, and grade data logging
- **Schedule Maintenance** — equipment maintenance scheduling
- **Flag Safety Concern** — surface mine-safety concerns (always escalates)
- **Coordinate Shipment** — outbound ore shipment coordination

**Scope boundary**: This actor handles COORDINATION only. Extraction authority,
blasting operations, and mine-safety-authority decisions are permanently
excluded and escalate to human specialists or dedicated verticals.

## Architecture

The governed-actor pattern (langgraph StateGraph + independent Governor +
Phase 0→3 rollout) applies checks in priority order, all HARD violations:

1. **Governor: Non-Ferrous Ore Governor** — enforces domain rules
   - Forbidden operations (extraction, blasting, safety authority) -> HARD block
   - Spec-basis citation required (no invented requirements)
   - Site/mine record must be verified
   - Unrecognized commodity type -> HARD block (production/shipment ops)
   - Safety concerns always escalate to human
   - Low confidence escalates to human

2. **LLM Advisor: NonFerrousOps-LLM** — proposes coordination asks
   - Mock advisor (deterministic, offline) for demo/testing
   - LLM advisor placeholder (langchain.model/ChatModel) for deployment
   - Defensive EDN parsing via `clojure.edn/read-string` (malformed/untrusted
     responses -> safe noop, never the full Clojure reader)

3. **Store: MemStore** — persistent state for sites, production records, assessments
   - Protocol-based (MemStore for dev/test, DatomicStore for production)
   - Site verification, assessment checklists, production records

## Domain Model

**Commodities** this actor coordinates (ISIC 0729 scope):
`:copper` `:lithium` `:nickel` `:cobalt` `:rare-earth`

**Sites** (non-ferrous metal ore mining operations) have:
- Jurisdiction (JP, US, AU, BR)
- Verification status (required before any operation)
- Associated production records and assessments

**Governance** enforces:
- Spec-basis citations (no invented requirements)
- Verified site records (no operation on unverified sites)
- Recognized commodity type (no operation on unclassified ore)
- Forbidden operation blocking (extraction/blasting/authority)
- Safety escalation (flag-safety-concern always escalates)
- Confidence floor (low confidence escalates)

## Running

### Demo (mock advisor, offline)

```bash
clojure -M:dev:run
```

Walks through four scenarios:
- Production logging for a recognized commodity (proposed, clean)
- Safety concern flagging (always escalates)
- Shipment coordination (proposed, clean)
- Production logging for an unrecognized commodity (HARD block)

### Tests

```bash
clojure -M:test
```

16 tests / 61 assertions covering domain facts, registry calculations, store
contract, governor rules, commodity classification, and escalation scenarios.

### Lint

```bash
clojure -M:lint
```

Static analysis (clj-kondo) on src/ and test/. Zero errors, zero warnings.

## Architecture Decision Records

- `docs/adr/0001-architecture.md` — Full ADR explaining the governed-actor
  architecture, scope boundary, and design decisions.

## License

AGPL-3.0-or-later. See LICENSE file.

## Contributing

See CONTRIBUTING.md.

## Governance

See GOVERNANCE.md.
