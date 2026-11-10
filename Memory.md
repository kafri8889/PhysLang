# PhysLang Project Memory

PhysLang is a Kotlin/JVM domain-specific language for mathematical calculations and strict dimensional analysis in classical physics. The project is intended for an academic paper or thesis, so implementation choices should stay lightweight, explainable, and focused on physics semantics.

## Current Stack

- Language/runtime: Kotlin targeting JVM.
- Build: Gradle Kotlin DSL with Kotlin JVM plugin.
- Tests: JUnit 5 through the Gradle test task.
- Entry points: `Main.kt`, `lang.cli.PhysCli`, and `lang.cli.PhysRunner`.

## Current Architecture

- `lang.lexer.Lexer` is a custom hand-written lexer.
- `lang.parser.Parser` is a Pratt parser using `nud` for prefix/primary parsing and `led` for infix/postfix parsing.
- `lang.ast` contains expression and statement nodes such as `BinaryExpr`, `UnaryExpr`, `QuantityExpr`, `IdentifierExpr`, `AssignExpr`, `CallExpr`, `VarDeclStmt`, `PrintStmt`, and `UnitDeclStmt`.
- `lang.ast.visitors.PhysicsEvaluator` is a tree-walking interpreter that implements expression and statement visitors.
- `lang.core.Environment` stores variables, custom units, and native functions.
- `lang.ast.PhysicsValue` is the core runtime quantity type.

## Runtime Quantity Model

`PhysicsValue` stores:

- `value`: raw numeric magnitude.
- `dimensions`: seven SI base dimension powers as an `IntArray`.
- `scale`: multiplier relative to base SI units.
- `unitName`: optional explicit/custom unit label.

The dimension vector order is:

1. Mass
2. Length
3. Time
4. Electric current
5. Temperature
6. Amount of substance
7. Luminous intensity

Built-in unit definitions live in `PhysicsValue.unitRegistry`. The evaluator also checks custom units from `Environment`.

## Existing Semantics

- `name = expression` declares a variable on first assignment and reassigns it afterwards (`var`/`val` are no longer keywords).
- `unit name = expression` evaluates the expression and registers it as a custom unit.
- Numeric literals evaluate to dimensionless `PhysicsValue` values.
- Quantity literals such as `10 kg` become `QuantityExpr` and use unit registry metadata.
- Function calls are parsed as `CallExpr` and require a `RuntimeValue.NativeFunction`.
- The currently registered native conversion function is `SI(x)`, which normalizes a physical value to base SI scale.

## Strict Physics Rules

- Addition and subtraction require compatible dimensions.
- Multiplication adds dimension powers.
- Division subtracts dimension powers.
- Power requires a dimensionless integer exponent.
- Unknown variables or units must be rejected.
- Custom units must be created from physical values, not strings.
- Future functions such as `sqrt(x)` should reject inputs whose dimension powers cannot be transformed safely into integer powers.

## Conditionals (`if` / `else if` / `else`)

Implemented as a single AST node `IfExpr` (`lang/ast/Expr.kt`) with a `Condition` (`lang/ast/Condition.kt`) and `IfBranch` (expression or block).

- Statement form (`if` at the start of a statement): `else` is optional, block branches run like normal blocks.
- Expression form (`a = if (...) x else y`, `print if ...`, function arguments, `form` bodies): `else` is required; a block branch evaluates to its last expression.
- `else if` is an else branch holding another `IfExpr`, inheriting the statement/expression mode.
- Conditions are physical relations only (no truthiness, no `&&`/`||`, no chained comparisons):
  - `a <op> b` with `<`, `<=`, `>`, `>=`, `==`, `!=`; both sides must have identical dimensions (a dimensioned quantity cannot be compared with a plain number). Compared on `scaledValue`.
  - `==` / `!=` use a `1e-9` relative tolerance; `a == b within tol` uses an absolute tolerance with the same dimensions (negative tolerance is a runtime error).
  - `x between low and high` is inclusive; all three must share dimensions; `low > high` is a runtime error.
  - `q is <unit-expr>` compares dimensions with a pure unit expression (`N`, `m/s`, `kg*m/s^2`, `1/s`, custom units; not variables or numbers).
- Dimensional narrowing: `is` is resolved statically by `SemanticAnalyzer`, so only the selected branch is analyzed (enables `form f(q) = if (q is m) q + 1 m else q + 1 kg`). Other conditions analyze every branch.
- Expression-form branches must unify: identical dimensions for quantities, both strings, or `null` mixed with either.
- New keywords: `within`, `between`, `and`, `is` (cannot be used as identifiers).
- `SemanticAnalyzer.visitAssignExpr` now records string variables as strings (previously stored as null, which broke `"text" + stringVar` in `PhysRunner`).
- Examples: `assets/fitur/15_if_else.phys`, `16_if_expression.phys`, `17_physics_conditions.phys`, and `assets/13`–`17`. Tests: `src/test/kotlin/IfExpressionTest.kt` (easy, medium, hard, stress, edge cases).
- Known quirk (pre-existing): variables shadow units with the same name (e.g. a variable `s` hides seconds).

## Lexical and Runtime Fixes

- Identifiers may contain and start with `_` (`kinetic_energy`, `_temp`).
- Number literals support scientific notation (`6.674e-11`, `2E+5`); `e` is an exponent only when a digit (optionally after `+`/`-`) follows, so `3eV` is still `3 eV`. The parser treats such literals as decimals (`isDecimalLiteral`).
- `#` starts a comment like `//`; comments are allowed at the end of an expression statement.
- Compound assignments `+=`, `-=`, `*=`, `/=` are desugared by the parser into `x = x <op> y` (synthesized operator token keeps the original text for diagnostics).
- `sqrt()` is typed by `SemanticAnalyzer`: argument must be a quantity with even dimension powers, result has halved powers. `SqrtFunction` also rejects odd powers at runtime.
- Dimensionless values print as plain numbers (no `rad` label) unless a unit label is explicitly attached.
- The per-statement runners (`ExampleRunner.executeSequentially` used by `./run.sh file.phys`, and the REPL in `PhysCli`) analyze each statement in a child `Environment`, so analyzer placeholder values no longer overwrite runtime values (previously `x = x + 3 m` gave the wrong result there). Side effect: these runners allow redeclaring a `form`, `PhysRunner` still rejects it.
- Remaining known limits: integer literals above `Int.MAX_VALUE` crash (`toInt`), no string escapes, `true`/`false` reserved only, `in` target must be a single unit name, no loops.
- Tests: `src/test/kotlin/LanguageFixesTest.kt`. Example: `assets/fitur/18_compound_and_notation.phys`. Full reference: `docs/PHYSLANG_LANGUAGE_REFERENCE.md`.
- Working-copy line endings are mixed (some files CRLF, index is LF, `core.autocrlf=input`). Preserve each file's existing line ending when editing; avoid `git stash`, which rewrote CRLF files as LF.

## Important Project Direction

- Keep source code, variable names, test names, comments intended for the codebase, and exception messages in professional English.
- Preserve the Pratt parser and visitor-based evaluator architecture.
- Prefer a small, physics-oriented DSL over generic programming-language features.
- Tests should emphasize correct dimensional behavior and explicit failure cases.
- If DSL examples mention `toBase(x)`, remember that the current implementation uses `SI(x)`; rename deliberately and consistently if the DSL surface changes.

## Domain-Specific Feature Roadmap (Novelty & Utility Ideas)

1. **Physical Constants Registry**: Built-in physical constants (`c`, `G`, `h`, `k_B`, `e`, `eps_0`) with their exact SI dimension vectors and values.
2. **Strictly-Guarded Comparison & Conditionals**: Comparison operators (`==`, `!=`, `<`, `<=`, `>`, `>=`) strictly enforced by `SemanticAnalyzer` to require identical dimension vectors (e.g. `5 m > 2 m` valid, `5 m > 2 kg` rejected).
3. **Convergence Loop (`converge delta < 0.001 m { ... }`)**: Physics-native loop construct where termination tolerance must strictly match the dimension of the monitored quantity.
4. **User-Defined Functions & Closures**: Lexically-scoped functions with call frames and return unwinding.
5. **Dimensional Contracts on Functions**: Function signatures declaring input/output dimension types (e.g. `fun force(m: kg, a: m/s^2) -> N`).
6. **Uncertainty Propagation**: Quantities with error intervals (`value ± sigma`) evaluated via derivative quadrature.
7. **Dimensional Inference on ℤ⁷**: Abelian group unification for static dimension checking and inference.

## Useful Commands

Run all tests:

```powershell
.\gradlew.bat test
```

List project files:

```powershell
rg --files
```
