# CLAUDE.md

## Development Guidelines

### Git Flow
- Use Git Flow branching: `main`, `develop`, `feature/*`, `release/*`, `hotfix/*`
- Branch from `develop` for features; branch from `main` for hotfixes
- Merge features back into `develop` via pull request; merge releases into both `main` and `develop`
- Tag releases on `main` with semantic versioning (e.g. `v1.2.3`)

### Testing
- Write test cases for all new code before or alongside implementation
- Cover happy paths, edge cases, and error conditions
- Place tests in a `tests/` directory mirroring the source structure

### Linting
- Run the linter before every push; do not push code with lint errors
- Fix all lint warnings unless there is a documented reason to suppress a specific rule

### Code Coverage
- Maintain a minimum code coverage threshold (target: 80%)
- Check coverage after running tests and ensure no regressions
- Do not merge code that significantly reduces overall coverage

### SOLID Principles
- **S**ingle Responsibility: each class/module has one reason to change
- **O**pen/Closed: open for extension, closed for modification
- **L**iskov Substitution: subtypes must be substitutable for their base types
- **I**nterface Segregation: prefer small, focused interfaces over large general ones
- **D**ependency Inversion: depend on abstractions, not concretions
- Review new code against all five principles before merging

### Abstraction & Interfaces
- Prefer interfaces and abstract types over concrete implementations
- Depend on abstractions, not concretions (Dependency Inversion Principle)
- Use interfaces to decouple components and enable testability (e.g. mocking)
- Avoid tight coupling between modules; expose behaviour through contracts
