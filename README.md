# PhysLang

PhysLang is a lightweight domain-specific language (DSL) for classical physics calculations with strict dimensional analysis. It is designed as an academic and thesis-oriented project that demonstrates how a small interpreted language can evaluate mathematical expressions while preserving the physical meaning of units.

Unlike ordinary calculators, PhysLang does not treat every number as a plain `Double`. Runtime values are physical quantities with a numeric magnitude, an SI dimension vector, a unit scale, and an optional unit label. The interpreter rejects meaningless expressions such as adding mass to length **before the program runs**, while still supporting derived units, custom units, unit conversion, physics formulas, and physics-aware branching.

📘 **Full language reference (features, semantics, and complete EBNF grammar):** [`docs/PHYSLANG_LANGUAGE_REFERENCE.md`](docs/PHYSLANG_LANGUAGE_REFERENCE.md)

## Project Goals

- Provide a compact DSL for physics-oriented mathematical evaluation.
- Enforce dimensional correctness statically, before execution.
- Support custom unit declarations and conversions between compatible units.
- Make conditions physically meaningful: dimension-checked comparisons, measurement tolerances, ranges, and dimension tests.
- Keep the implementation small enough to explain in an academic paper.
- Demonstrate a hand-written lexer, Pratt parser, semantic analyzer, and visitor-based evaluator in Kotlin.

## Technology Stack

- Kotlin targeting the JVM.
- Gradle Kotlin DSL.
- JUnit 5 for automated tests.
- Hand-written language tooling without parser generators.

## Language Overview

| Feature | Example |
| --- | --- |
| Numbers and scientific notation | `42`, `3.14`, `6.674e-11` |
| Quantity literals | `10 kg`, `9.8 m/s^2`, `4 m^2` |
| Arithmetic | `+`, `-`, `*`, `/`, `^`, unary `-`, parentheses |
| Variables | `mass = 2 kg`, `mass += 500 g` |
| Custom units | `unit kph = 1000 m / 3600 s` |
| Unit conversion | `distance in km` |
| Physics formulas | `form Force(m, a) = m * a` |
| Native functions | `SI(x)`, `sqrt(x)` |
| Branching (statement or expression) | `if (...) { } else if (...) { } else { }`, `a = if (...) x else y` |
| Physics conditions | `<`, `<=`, `>`, `>=`, `==`, `!=`, `within`, `between ... and ...`, `is` |
| Blocks and scopes | `{ ... }` |
| Output | `print "Energy: " + E` |
| Comments | `// comment`, `# comment` |

Example:

```phys
unit kph = 1000 m / 3600 s

top_speed = 100 kph
print "Custom unit: " + top_speed      // 100.0 kph
print "Base SI: " + SI(top_speed)      // 27.77... m*s^-1

form KineticEnergy(m, v) = 0.5 * m * v^2
energy = KineticEnergy(1200 kg, top_speed)
print energy in kJ                     // 462.96... kJ

# Physics-aware branching
if (energy > 500 kJ) {
    print "High-energy impact"
} else {
    print "Low-energy impact"          // printed
}
```

### Physics-Aware Conditions

`if` works as a statement and, like Kotlin, as an expression. Conditions must be physical relations; a quantity is never "true" or "false" by itself.

```phys
// Comparisons require identical dimensions and compare in SI scale
if (1 km > 500 m) { print "longer" }          // valid
if (5 m > 2 kg) { }                           // ERROR: cannot compare 'm' with 'kg'

// Measurement tolerance
if (g == 9.81 m/s^2 within 0.05 m/s^2) { print "standard gravity" }

// Inclusive physical range
phase = if (T between 273.15 K and 373.15 K) "liquid" else "not liquid"

// Dimension test with dimensional narrowing: only the selected branch is checked
form addOne(q) = if (q is m) q + 1 m else q + 1 kg
print addOne(2 m)      // 3.0 m
print addOne(3 kg)     // 4.0 kg

// Piecewise formula; all branches of an if expression must share dimensions
form friction(push) = if (push <= maxStatic) push else kinetic
```

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
- `sqrt` halves dimension powers and requires them to be even.
- Comparisons, tolerances, and ranges require identical dimensions.
- Unknown units and unknown variables are rejected.

For example:

```phys
force = 20 kg * 10 m / 2 s / 2 s
print force          // 50.0 N
```

Invalid dimensional expressions are rejected with a diagnostic, and nothing is executed:

```text
ERROR: Dimension mismatch: cannot operate different dimensions.
 --> line 1, column 16
invalid = 5 kg + 10 m
               ^
```

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

The lexer is a custom hand-written scanner located in `src/main/kotlin/lang/lexer`. It converts source text into tokens such as identifiers, numeric literals (including scientific notation), operators, parentheses, keywords, strings, and comments.

### Parser

The parser is a Pratt parser located in `src/main/kotlin/lang/parser/Parser.kt`. It uses:

- `nud` methods for prefix and primary expressions, including `if` expressions.
- `led` methods for infix, assignment, conversion, and function-call expressions.
- Binding powers from `Precedence.kt`.

Conditions (`<`, `within`, `between`, `is`, ...) are parsed only inside `if (...)`, so they never leak into general arithmetic.

### AST

AST definitions live in `src/main/kotlin/lang/ast`. Important nodes include:

- Expressions: `BinaryExpr`, `UnaryExpr`, `LiteralExpr`, `QuantityExpr`, `IdentifierExpr`, `AssignExpr`, `CallExpr`, `ConversionExpr`, `IfExpr`
- Conditions: `Condition.Comparison`, `Condition.Between`, `Condition.DimensionCheck`
- Statements: `ExprStmt`, `PrintStmt`, `UnitDeclStmt`, `FormDeclStmt`, `BlockStmt`

### Semantic Analyzer

The semantic analyzer checks the whole program before evaluation: dimensional consistency, defined names, function arity, conversion targets, condition dimensions, and type consistency between `if` branches. It also performs *dimensional narrowing* for `is` conditions.

### Evaluator

`PhysicsEvaluator` is a tree-walking interpreter that implements the visitor pattern. It evaluates expressions and statements, resolves variables, built-in units, custom units, formulas, and native functions through the runtime environment, and compares quantities in SI scale.

### Environment

The environment stores:

- Variables and formulas.
- Custom unit definitions.
- Native functions such as `SI` and `sqrt`.

Environments are nested, so blocks and `if` branches have their own scope while still reading (and updating) variables from enclosing scopes.

## Project Structure

```text
src/main/kotlin
  Main.kt
  lang/ast       AST nodes, conditions, visitors, and runtime values
  lang/cli       REPL and script runner
  lang/core      environment, callable interface, keywords, units, and native functions
  lang/error     diagnostics and error reporting
  lang/lexer     token definitions and lexer
  lang/parser    Pratt parser and precedence table
  lang/semantic  semantic analysis

src/test/kotlin  JUnit tests
assets           example PhysLang programs (assets/fitur: one file per feature)
docs             language reference, specification, and research notes
```

## Running the Project

Compile the project:

```bash
./gradlew classes            # Windows: .\gradlew.bat classes
```

Run one or more `.phys` files:

```bash
./run.sh assets/fitur/15_if_else.phys
```

Run without arguments to open the interactive menu (REPL, run a file by path, or browse the example files):

```bash
./run.sh
```

The `main` function in `Main.kt` can also be launched from an IDE.

Run all tests:

```bash
./gradlew test               # Windows: .\gradlew.bat test
```

## Example Scripts

The `assets` directory contains PhysLang programs:

- `assets/fitur/` — one small file per language feature (literals, arithmetic, units, conversion, formulas, blocks, null safety, `if`, physics conditions, compound assignment, ...).
- `assets/01`–`17` — realistic physics scenarios such as vehicle crash dynamics, hydraulic lifts, orbital energy, friction regimes, structural load checks, and water phase states.
- Files containing `error` in their name intentionally fail semantic analysis.

Example custom velocity unit:

```phys
unit velocity = 1 m / 1 s
v = 10 velocity
t = 5 s
distance = v * t
print distance        // 50.0 m
```

Example structural safety check:

```phys
unit yieldStrength = 250 MPa
area = 2 cm^2

form stress(load) = load / area
form safetyState(load) = if (stress(load) <= 0.6 * yieldStrength) "SAFE"
    else if (stress(load) == yieldStrength within 1 MPa) "CRITICAL: at yield point"
    else if (stress(load) < yieldStrength) "WARNING"
    else "FAILED"

print safetyState(20 kN)    // SAFE
print safetyState(50 kN)    // CRITICAL: at yield point
```

## Research Relevance

PhysLang is useful as a research artifact because it connects programming language implementation techniques with physics education and dimensional safety:

- It shows how a DSL can encode domain rules directly into static and runtime semantics.
- It demonstrates that physical unit consistency can be enforced without a large compiler framework.
- Its conditional constructs are physics-specific: comparisons are dimension-checked, equality can carry a measurement tolerance, ranges are dimension-checked, and `is` enables branching on dimensions with static narrowing.
- It provides a clear case study for lexer design, Pratt parsing, AST construction, semantic checking, and visitor-based interpretation.
- It can be extended with additional native functions, derived units, and educational examples while preserving a compact core.

## Development Principles

- Keep all source code, identifiers, tests, and exception messages in professional English.
- Preserve the existing Pratt parser architecture.
- Preserve strict dimensional analysis as the main design priority.
- Prefer physics-focused features over general-purpose programming constructs.
- Add tests for both valid calculations and invalid dimensional expressions.

## License

PhysLang is released under the [MIT License](LICENSE).
