# Security Policy

## Reporting Security Issues

If you discover a security vulnerability, please email the maintainers privately
rather than using the public issue tracker.

**Do not** open a public GitHub issue for security vulnerabilities.

Include in your report:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Any known workarounds or fixes

## Security Model

This project implements a governed-actor architecture with an independent
Governor layer that enforces domain compliance:

- **Governor checks** are unconditional and cannot be bypassed
- **Scope boundaries** (e.g., no extraction/blasting authority) are permanent
- **Safety escalation** (flag-safety-concern) is not auto-approvable
- **Spec-basis verification** prevents invented requirements
- **Commodity classification** prevents coordination of unrecognized ore types

These are not configuration options or policy choices -- they are architectural
requirements that cannot be disabled or overridden.

## Dependencies

This project depends on:
- `io.github.kotoba-lang/langgraph` — state machine orchestration
- `io.github.kotoba-lang/robotics` — generic robotics contract

All dependencies are pinned to specific versions in `deps.edn`.

## Untrusted Input Handling

LLM-generated proposals are parsed with `clojure.edn/read-string` (the safe
EDN reader), never the full Clojure reader (`clojure.core/read-string`),
because LLM output is untrusted input.

## Audit Trail

All decisions (proposals, verdicts, holds, escalations) are recorded in the
append-only audit ledger and cannot be retroactively modified.

## Scope Exclusions

The following operations are permanently blocked by the Governor and cannot
be proposed by this actor:

- `:extraction/extract` — extraction authority
- `:extraction/blast` — blasting operations
- `:authority/safety-clearance` — mine-safety-authority decisions

Any proposal attempting these operations is rejected unconditionally.
