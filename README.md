# PhysLang

PhysLang is a lightweight domain-specific language (DSL) for classical physics calculations with strict dimensional analysis. It is designed as an academic and thesis-oriented project that demonstrates how a small interpreted language can evaluate mathematical expressions while preserving the physical meaning of units.

Unlike ordinary calculators, PhysLang does not treat every number as a plain `Double`. Runtime values are represented as physical quantities with numeric magnitude, SI dimension vectors, unit scales, and optional unit labels. This allows the interpreter to reject invalid expressions such as adding mass to length while still supporting derived units and custom unit definitions.

## Project Goals

- Provide a compact DSL for physics-oriented mathematical evaluation.
- Enforce dimensional correctness during program execution.
- Support custom unit declarations such as velocity units or derived SI quantities.
- Keep the implementation small enough to explain in an academic paper.
- Demonstrate a hand-written lexer, Pratt parser, semantic analyzer, and visitor-based evaluator in Kotlin.

## Technology Stack

- Kotlin targeting the JVM.
- Gradle Kotlin DSL.
- JUnit 5 for automated tests.
- Hand-written language tooling without parser generators.

## Language Overview

PhysLang currently supports:

- Numeric literals.
- Physical quantity literals such as `10 kg`, `5 m`, and `2 s`.
- Arithmetic operators: `+`, `-`, `*`, `/`, and `^`.
- Parenthesized expressions.
- Unary minus.
- Variable declarations with `var`.
- Variable reassignment.
- Custom unit declarations with `unit`.
- Function calls through native Kotlin-backed functions.
- `print` statements.
- String literals and simple string concatenation for output.

Example:

```phys
unit kph = 1000 m / 3600 s

var topSpeed = 100 kph
print "Custom unit: " + topSpeed
print "Base SI: " + SI(topSpeed)

var energy = 100 kg * m^2 / s^2
var mass = 2 kg
var velocitySquared = (2 * energy) / mass
print velocitySquared
```

The built-in `SI(x)` function normalizes a physical value into base SI scale while keeping its dimensions.

## Dimensional Analysis Model

The core runtime type is `PhysicsValue`. It stores:

- `value`: the raw numeric magnitude.
- `dimensions`: a seven-element integer vector for SI base dimension powers.
- `scale`: a multiplier relative to the base SI unit.
- `unitName`: an optional label used for explicit or custom unit display.

The dimension vector follows the seven SI base quantities:

| Index | Quantity | Example Unit |
| --- | --- | --- |
| 0 | Mass | `kg` |
| 1 | Length | `m` |
| 2 | Time | `s` |
| 3 | Electric current | `A` |
| 4 | Temperature | `K` |
| 5 | Amount of substance | `mol` |
| 6 | Luminous intensity | `cd` |

Arithmetic updates this vector according to physics rules:

- Addition and subtraction require compatible dimensions.
- Multiplication adds dimension powers.
- Division subtracts dimension powers.
- Exponentiation requires a dimensionless integer exponent.
- Unknown units and unknown variables are rejected.

For example:

```phys
var force = 20 kg * 10 m / 2 s / 2 s
print force
```

The resulting dimension is equivalent to `kg m s^-2`, which is the dimension of force.

Invalid dimensional expressions are rejected:

```phys
var invalid = 5 kg + 10 m
```

Mass and length are incompatible for addition, so the evaluator must report an error instead of producing a meaningless result.

## Architecture

PhysLang is implemented as a small interpreter pipeline:

```text
Source code
  -> Lexer
  -> Pratt Parser
  -> Semantic Analyzer
  -> Physics Evaluator
  -> Runtime result
```

### Lexer

The lexer is a custom hand-written scanner located in `src/main/kotlin/lang/lexer`. It converts source text into tokens such as identifiers, numeric literals, operators, parentheses, keywords, strings, and comments.

### Parser

The parser is a Pratt parser located in `src/main/kotlin/lang/parser/Parser.kt`. It uses:

- `nud` methods for prefix and primary expressions.
- `led` methods for infix, assignment, and function-call expressions.
- Binding powers from `Precedence.kt`.

This keeps expression parsing compact while still supporting operator precedence.

### AST

AST definitions live in `src/main/kotlin/lang/ast`. Important nodes include:

- `BinaryExpr`
- `UnaryExpr`
- `LiteralExpr`
- `QuantityExpr`
- `VariableExpr`
- `AssignExpr`
- `CallExpr`
- `VarDeclStmt`
- `PrintStmt`
- `UnitDeclStmt`

### Semantic Analyzer

The semantic analyzer checks language-level validity before evaluation. It helps distinguish declaration rules, assignment rules, callable values, and other static constraints.

### Evaluator

`PhysicsEvaluator` is a tree-walking interpreter that implements the visitor pattern. It evaluates expressions and statements while enforcing dimensional rules. It also resolves variables, built-in units, custom units, and native functions through the runtime environment.

### Environment

The environment stores:

- Variables.
- Custom unit definitions.
- Native functions such as `SI`.

Variable declaration and reassignment are intentionally strict:

- `var` introduces a new variable.
- Assignment updates an existing variable.
- Redeclaration and assignment to undeclared variables are errors.

## Project Structure

```text
src/main/kotlin
  Main.kt
  lang/ast       AST nodes, visitors, and runtime values
  lang/cli       REPL and script runner
  lang/core      environment, callable interface, units, and native functions
  lang/error     diagnostics and error reporting
  lang/lexer     token definitions and lexer
  lang/parser    Pratt parser and precedence table
  lang/semantic  semantic analysis

src/test/kotlin  JUnit tests
assets           example PhysLang scripts
```

## Running the Project

Run the interactive CLI from the `main` function in `Main.kt` using an IDE or another Kotlin/JVM launcher.

Compile the project:

```powershell
.\gradlew.bat classes
```

Run all tests:

```powershell
.\gradlew.bat test
```

## Example Scripts

The `assets` directory contains small PhysLang programs, including examples for custom units and SI conversion.

Example custom velocity unit:

```phys
unit velocity = 1 m / 1 s
var v = 10 velocity
var t = 5 s
var distance = v * t
print distance
```

Example base conversion:

```phys
unit kph = 1000 m / 3600 s
var speed = 2 kph
print "With custom unit: " + speed
print "Unpack unit: " + SI(speed)
```

## Research Relevance

PhysLang is useful as a research artifact because it connects programming language implementation techniques with physics education and dimensional safety:

- It shows how a DSL can encode domain rules directly into the runtime semantics.
- It demonstrates that physical unit consistency can be enforced without a large compiler framework.
- It provides a clear case study for lexer design, Pratt parsing, AST construction, semantic checking, and visitor-based interpretation.
- It can be extended with additional native functions, derived units, and educational examples while preserving a compact core.

## Development Principles

- Keep all source code, identifiers, tests, and exception messages in professional English.
- Preserve the existing Pratt parser architecture.
- Preserve strict dimensional analysis as the main design priority.
- Prefer physics-focused features over general-purpose programming constructs.
- Add tests for both valid calculations and invalid dimensional expressions.
