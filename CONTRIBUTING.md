# Contributing

Thank you for your interest in contributing to cloud-itonami-isic-0729!

## Getting Started

1. Clone the repository
2. Run `clojure -M:test` to verify the test suite passes
3. Run `clojure -M:lint` to check code style
4. Make your changes in a feature branch

## Guidelines

- **All code is `.cljc`** (portable Clojure) -- no JVM-only constructs
- **Tests are required** for new features or bug fixes
- **ADRs are required** for architectural decisions
- **Lint must pass** (`clj-kondo` with no errors)

## Testing

```bash
clojure -M:test
```

## Linting

```bash
clojure -M:lint
```

## Submitting Changes

1. Create a feature branch: `git checkout -b feature/my-feature`
2. Make your changes and commit with clear messages
3. Push to GitHub and open a pull request
4. Ensure tests and linting pass in CI
5. Request review from maintainers

## Scope Boundaries

This actor implements COORDINATION ONLY:
- Production logging, maintenance scheduling, safety flagging, shipment coordination
- NOT extraction operations, blasting operations, mine-safety-authority decisions

These scope boundaries are enforced by the Governor and cannot be overridden.

## License

By contributing, you agree that your contributions will be licensed under
AGPL-3.0-or-later.
