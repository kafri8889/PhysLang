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
- `lang.ast` contains expression and statement nodes such as `BinaryExpr`, `UnaryExpr`, `QuantityExpr`, `VariableExpr`, `AssignExpr`, `CallExpr`, `VarDeclStmt`, `PrintStmt`, and `UnitDeclStmt`.
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

- `var name = expression` declares a variable and rejects redeclaration.
- `name = expression` reassigns an existing variable and rejects assignment to undeclared names.
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

## Important Project Direction

- Keep source code, variable names, test names, comments intended for the codebase, and exception messages in professional English.
- Preserve the Pratt parser and visitor-based evaluator architecture.
- Prefer a small, physics-oriented DSL over generic programming-language features.
- Tests should emphasize correct dimensional behavior and explicit failure cases.
- If DSL examples mention `toBase(x)`, remember that the current implementation uses `SI(x)`; rename deliberately and consistently if the DSL surface changes.

## Useful Commands

Run all tests:

```powershell
.\gradlew.bat test
```

List project files:

```powershell
rg --files
```
