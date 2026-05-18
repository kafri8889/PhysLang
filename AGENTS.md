# PhysLang Agent Instructions

You are working on PhysLang, a Kotlin/JVM domain-specific language for classical physics calculations with strict dimensional analysis. Keep all source code, identifiers, comments intended for the codebase, tests, and exception messages in professional English.

## Project Direction

- Treat PhysLang as an academic/thesis DSL project, not a general-purpose programming language.
- Prioritize strict physics rules over permissive language behavior.
- Keep the language focused on physical expressions, mathematical evaluation, unit declarations, native functions, and simple execution flow.
- Do not introduce complex generic programming features such as classes, generic arrays, object systems, or broad application scripting constructs inside the DSL unless explicitly requested.

## Architecture To Preserve

- Lexer: hand-written scanner in `src/main/kotlin/lang/lexer`.
- Parser: Pratt parser in `src/main/kotlin/lang/parser/Parser.kt`, using `nud` and `led` methods with precedence from `Precedence.kt`.
- AST: expression and statement nodes in `src/main/kotlin/lang/ast`.
- Evaluator: tree-walking visitor implementation in `src/main/kotlin/lang/ast/visitors/PhysicsEvaluator.kt`.
- Runtime environment: variables, native functions, and custom units are managed by `src/main/kotlin/lang/core/Environment.kt`.
- Runtime values: physical quantities are represented by `PhysicsValue`, not plain `Double`.

## PhysicsValue Rules

`PhysicsValue` is the core runtime type. Preserve these fields and semantics:

- `value`: raw numeric magnitude.
- `dimensions`: `IntArray(7)` representing SI base dimension powers in this order: mass, length, time, electric current, temperature, amount of substance, luminous intensity.
- `scale`: multiplier relative to the base SI unit.
- `unitName`: optional display label for custom or explicit units.

Arithmetic must maintain dimensional correctness:

- Addition and subtraction require compatible dimensions.
- Multiplication adds dimension powers.
- Division subtracts dimension powers.
- Exponentiation only accepts dimensionless integer exponents.
- Functions that transform dimensions, such as square root, must validate dimension powers before producing a result.

## Language Semantics

- `var` declares a new variable. Redeclaration should be rejected.
- Assignment updates an existing variable. Assignment to an undeclared variable should be rejected.
- `unit name = expression` declares a custom unit from an evaluated physical expression.
- Built-in and custom units should compose naturally in expressions such as `1000 m / 3600 s`.
- Native functions implement `Callable` and are injected through the global environment.
- Current code registers `SI(...)` as the base-unit normalization function. If the public DSL name changes to `toBase(...)`, update tests, examples, and documentation together.

## Implementation Guidelines

- Follow existing package boundaries and style before adding new abstractions.
- Add focused JUnit 5 tests for parser/evaluator changes, especially for dimensional errors.
- Prefer clear exceptions that explain the invalid physics rule or language rule.
- Keep examples and fixtures under `assets` aligned with supported syntax.
- Avoid silently coercing incompatible dimensions or treating unknown units as scalars.
- When changing unit formatting, preserve the seven-base-dimension model and verify custom scale behavior.

## Verification

Use the Gradle wrapper from the repository root:

```powershell
.\gradlew.bat test
```

If a change only edits documentation, tests are optional. For code changes, run the relevant tests before reporting completion.
