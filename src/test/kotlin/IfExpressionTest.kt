import lang.cli.PhysRunner
import lang.core.Environment
import lang.error.ErrorReporter
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IfExpressionTest {

    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    @BeforeEach
    fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @AfterEach
    fun tearDown() {
        System.setOut(standardOut)
    }

    private fun runAndGetOutput(script: String): String {
        outputStreamCaptor.reset()
        val environment = Environment()
        val reporter = ErrorReporter()
        val runner = PhysRunner(environment, reporter)
        runner.runScript(script)
        return outputStreamCaptor.toString().trim().replace("\r\n", "\n")
    }

    private fun assertOutput(expected: String, script: String) {
        assertEquals(expected.trimIndent(), runAndGetOutput(script.trimIndent()))
    }

    /**
     * Invalid scripts must report an error and must never execute a branch (marked with "BRANCH").
     */
    private fun assertRejected(script: String, vararg expectedMessages: String) {
        val output = runAndGetOutput(script.trimIndent())

        assertTrue(output.contains("ERROR"), "Expected an error, got:\n$output")
        // Diagnostics echo the source line, so only an exact printed line counts as an executed branch
        assertFalse(output.lines().any { it.trim() == "BRANCH" }, "No branch should be executed:\n$output")

        for (message in expectedMessages) {
            assertTrue(output.contains(message), "Expected '$message' in output:\n$output")
        }
    }

    @Nested
    @DisplayName("Tingkat mudah")
    inner class Easy {

        @Test
        @DisplayName("If dengan kondisi benar mengeksekusi cabangnya")
        fun testIfTrue() {
            assertOutput("yes", """
                if (2 m > 1 m) {
                    print "yes"
                }
            """)
        }

        @Test
        @DisplayName("If dengan kondisi salah tanpa else tidak mencetak apa pun")
        fun testIfFalseWithoutElse() {
            assertOutput("", """
                if (1 m > 2 m) {
                    print "no"
                }
            """)
        }

        @Test
        @DisplayName("If-else memilih cabang else saat kondisi salah")
        fun testIfElse() {
            assertOutput("else branch", """
                if (1 kg > 2 kg) {
                    print "then branch"
                } else {
                    print "else branch"
                }
            """)
        }

        @Test
        @DisplayName("Rantai else-if memilih cabang pertama, tengah, dan terakhir")
        fun testElseIfChain() {
            val script = { speed: String ->
                """
                    speed = $speed
                    if (speed < 10 m/s) {
                        print "slow"
                    } else if (speed < 30 m/s) {
                        print "moderate"
                    } else {
                        print "fast"
                    }
                """.trimIndent()
            }

            assertEquals("slow", runAndGetOutput(script("5 m/s")))
            assertEquals("moderate", runAndGetOutput(script("20 m/s")))
            assertEquals("fast", runAndGetOutput(script("50 m/s")))
        }

        @Test
        @DisplayName("Keenam operator pembanding menghasilkan nilai benar dan salah dengan tepat")
        fun testAllComparisonOperators() {
            val cases = listOf(
                "1 m < 2 m" to true, "2 m < 1 m" to false,
                "2 m <= 2 m" to true, "3 m <= 2 m" to false,
                "3 m > 2 m" to true, "1 m > 2 m" to false,
                "2 m >= 2 m" to true, "1 m >= 2 m" to false,
                "2 m == 2 m" to true, "2 m == 3 m" to false,
                "2 m != 3 m" to true, "2 m != 2 m" to false
            )

            for ((condition, expected) in cases) {
                val output = runAndGetOutput("if ($condition) {\n print \"true\"\n} else {\n print \"false\"\n}")
                assertEquals(expected.toString(), output, "Condition: $condition")
            }
        }

        @Test
        @DisplayName("Blok kosong pada cabang tetap valid")
        fun testEmptyBlock() {
            assertOutput("after", """
                if (1 m > 0 m) {}
                print "after"
            """)
        }

        @Test
        @DisplayName("If sebagai ekspresi sederhana dapat disimpan ke variabel")
        fun testSimpleIfExpression() {
            assertOutput("5.0 N", """
                a = if (1 m > 0 m) 5 N else 3 N
                print a
            """)
        }
    }

    @Nested
    @DisplayName("Tingkat medium")
    inner class Medium {

        @Test
        @DisplayName("Perbandingan memakai skala SI sehingga satuan berbeda bisa dibandingkan")
        fun testCrossScaleComparison() {
            assertOutput("""
                km
                mm
                h
                rounding
            """, """
                if (1 km > 500 m) {
                    print "km"
                }
                if (1000 mm == 1 m) {
                    print "mm"
                }
                if (1 h == 60 min) {
                    print "h"
                }
                if (0.1 m + 0.2 m == 0.3 m) {
                    print "rounding"
                }
            """)
        }

        @Test
        @DisplayName("Toleransi 'within' menerima selisih di dalam toleransi dan menolak di luar toleransi")
        fun testWithinTolerance() {
            assertOutput("""
                inside
                outside
                not equal
            """, """
                if (9.8 m/s^2 == 9.81 m/s^2 within 0.05 m/s^2) {
                    print "inside"
                }
                if (9.8 m/s^2 == 9.81 m/s^2 within 0.001 m/s^2) {
                    print "BRANCH"
                } else {
                    print "outside"
                }
                if (9.8 m/s^2 != 9.81 m/s^2 within 0.001 m/s^2) {
                    print "not equal"
                }
            """)
        }

        @Test
        @DisplayName("'between' inklusif pada kedua batas dan menolak nilai di luar rentang")
        fun testBetweenRange() {
            assertOutput("""
                lower
                upper
                outside
                mixed scale
            """, """
                if (273.15 K between 273.15 K and 373.15 K) {
                    print "lower"
                }
                if (373.15 K between 273.15 K and 373.15 K) {
                    print "upper"
                }
                if (400 K between 273.15 K and 373.15 K) {
                    print "BRANCH"
                } else {
                    print "outside"
                }
                if (1 km between 500 m and 2000 m) {
                    print "mixed scale"
                }
            """)
        }

        @Test
        @DisplayName("'is' memeriksa dimensi terhadap satuan bawaan, ekspresi satuan, dan satuan kustom")
        fun testDimensionCheck() {
            assertOutput("""
                force
                composite
                velocity
                frequency
                not energy
            """, """
                unit kph = 1000 m / 3600 s
                f = 12 kg * 3 m/s^2
                if (f is N) {
                    print "force"
                }
                if (5 kN is kg*m/s^2) {
                    print "composite"
                }
                if (20 m/s is kph) {
                    print "velocity"
                }
                if (50 Hz is 1/s) {
                    print "frequency"
                }
                if (f is J) {
                    print "BRANCH"
                } else {
                    print "not energy"
                }
            """)
        }

        @Test
        @DisplayName("If ekspresi dapat dipakai di print, aritmetika, argumen fungsi, dan konversi")
        fun testIfExpressionPositions() {
            assertOutput("""
                printed
                4.0 m
                2.0 m
                1.5 km
            """, """
                print if (1 m > 0 m) "printed" else "BRANCH"
                print 2 * if (1 m > 0 m) 2 m else 3 m
                print SI(if (1 m < 0 m) 5 m else 2 m)
                print (if (1 m > 0 m) 1500 m else 1 m) in km
            """)
        }

        @Test
        @DisplayName("Nilai cabang blok adalah ekspresi terakhir di dalam blok")
        fun testBlockBranchValue() {
            assertOutput("98.0 J", """
                mass = 2 kg
                energy = if (mass > 1 kg) {
                    gravity = 9.8 m/s^2
                    height = 5 m
                    mass * gravity * height
                } else {
                    0 J
                }
                print energy
            """)
        }

        @Test
        @DisplayName("'else' boleh di baris berikutnya dan diselingi komentar")
        fun testElseOnNextLine() {
            assertOutput("else", """
                if (1 m > 2 m) {
                    print "BRANCH"
                }
                // comment between the branches
                else {
                    print "else"
                }
            """)
        }

        @Test
        @DisplayName("Variabel lokal cabang tidak bocor, tetapi assignment ke variabel luar tetap berlaku")
        fun testBranchScope() {
            assertOutput("2.0 m", """
                x = 1 m
                if (1 m > 0 m) {
                    x = 2 m
                }
                print x
            """)

            assertRejected("""
                if (1 m > 0 m) {
                    inner = 5 m
                }
                print inner
                print "BRANCH"
            """, "'inner' is not defined")
        }

        @Test
        @DisplayName("Cabang berupa ekspresi assignment pada if statement")
        fun testExpressionBranchInStatement() {
            assertOutput("3.0 m", """
                x = 1 m
                if (1 m > 0 m) x = 3 m
                print x
            """)
        }
    }

    @Nested
    @DisplayName("Tingkat sulit")
    inner class Hard {

        @Test
        @DisplayName("If bersarang tiga level di cabang then dan else")
        fun testNestedIfThreeLevels() {
            assertOutput("liquid, warm", """
                T = 330 K
                if (T between 273.15 K and 373.15 K) {
                    if (T > 300 K) {
                        if (T < 350 K) {
                            print "liquid, warm"
                        } else {
                            print "liquid, hot"
                        }
                    } else {
                        print "liquid, cool"
                    }
                } else {
                    if (T < 273.15 K) {
                        print "solid"
                    } else {
                        print "gas"
                    }
                }
            """)
        }

        @Test
        @DisplayName("Form piecewise dipanggil dengan beberapa nilai")
        fun testPiecewiseForm() {
            assertOutput("""
                200.0 N
                150.0 N
            """, """
                maxStatic = 250 N
                kinetic = 150 N
                form friction(push) = if (push <= maxStatic) push else kinetic
                print friction(200 N)
                print friction(400 N)
            """)
        }

        @Test
        @DisplayName("Narrowing dimensi 'is' membuat form valid untuk input berdimensi berbeda")
        fun testDimensionalNarrowing() {
            assertOutput("""
                3.0 m
                4.0 kg
                force
                energy
                other
            """, """
                form addOne(q) = if (q is m) q + 1 m else q + 1 kg
                print addOne(2 m)
                print addOne(3 kg)
                form describe(q) = if (q is N) "force" else if (q is J) "energy" else "other"
                print describe(10 N)
                print describe(5 kJ)
                print describe(3 s)
            """)
        }

        @Test
        @DisplayName("Narrowing tetap melaporkan error pada cabang yang terpilih")
        fun testNarrowingStillChecksSelectedBranch() {
            assertRejected("""
                form addOne(q) = if (q is m) q + 1 m else q + 1 kg
                print addOne(2 s)
                print "BRANCH"
            """, "Dimension mismatch")
        }

        @Test
        @DisplayName("Error pada cabang yang tidak dieksekusi tetap terdeteksi sebelum eksekusi")
        fun testErrorInDeadBranch() {
            assertRejected("""
                if (1 m > 0 m) {
                    print "BRANCH"
                } else {
                    print 1 m + 1 kg
                }
            """, "Dimension mismatch")
        }

        @Test
        @DisplayName("Unifikasi tipe cabang: dimensi sama dengan skala berbeda, string, dan null")
        fun testBranchTypeUnification() {
            val output = runAndGetOutput("""
                v = if (1 m > 0 m) 10 m/s else 36 km/h
                print v
                label = if (1 m > 0 m) "a" else "b"
                print label
                n = if (1 m > 0 m) null else 2 m
                print n
            """.trimIndent())

            assertFalse(output.contains("ERROR"), output)
            assertEquals(listOf("10.0", "a", "null"), output.lines().map { it.substringBefore(' ') })
        }

        @Test
        @DisplayName("If berurutan saling bergantung melalui variabel yang diubah di cabang")
        fun testDependentSequentialIfs() {
            val output = runAndGetOutput("""
                speed = 10 m/s
                if (speed < 20 m/s) {
                    speed = speed * 3
                }
                if (speed > 20 m/s) {
                    print "accelerated"
                }
                print speed
            """.trimIndent())

            assertEquals(listOf("accelerated", "30.0"), output.lines().map { it.substringBefore(' ') })
        }

        @Test
        @DisplayName("Rantai else-if menggabungkan within, between, dan is")
        fun testMixedPhysicsConditions() {
            val script = { q: String ->
                """
                    form classify(q) = if (q is m/s) {
                        "velocity"
                    } else if (q == 9.81 m/s^2 within 0.1 m/s^2) {
                        "standard gravity"
                    } else if (q between 0 m/s^2 and 5 m/s^2) {
                        "gentle acceleration"
                    } else {
                        "strong acceleration"
                    }
                    print classify($q)
                """.trimIndent()
            }

            assertEquals("velocity", runAndGetOutput(script("3 m/s")))
            assertEquals("standard gravity", runAndGetOutput(script("9.8 m/s^2")))
            assertEquals("gentle acceleration", runAndGetOutput(script("2 m/s^2")))
            assertEquals("strong acceleration", runAndGetOutput(script("20 m/s^2")))
        }
    }

    @Nested
    @DisplayName("Stress test")
    inner class Stress {

        @Test
        @DisplayName("Rantai else-if 1000 cabang hanya mengeksekusi satu cabang yang tepat")
        fun testLongElseIfChain() {
            val script = buildString {
                appendLine("x = 500.5 m")
                append("if (x < 1 m) {\n print \"branch 0\"\n}")
                for (i in 1 until 1000) {
                    append(" else if (x < ${i + 1} m) {\n print \"branch $i\"\n}")
                }
                append(" else {\n print \"none\"\n}")
            }

            assertEquals("branch 500", runAndGetOutput(script))
        }

        @Test
        @DisplayName("If statement bersarang 200 level tidak menyebabkan stack overflow")
        fun testDeepNestedStatements() {
            val depth = 200
            val script = buildString {
                repeat(depth) { appendLine("if (1 m > 0 m) {") }
                appendLine("print \"deep\"")
                repeat(depth) { appendLine("}") }
            }

            assertEquals("deep", runAndGetOutput(script))
        }

        @Test
        @DisplayName("If ekspresi bersarang 200 level menghasilkan nilai terdalam")
        fun testDeepNestedExpressions() {
            var expression = "7 m"
            repeat(200) { expression = "if (1 m > 0 m) $expression else 0 m" }

            assertEquals("7.0 m", runAndGetOutput("a = $expression\nprint a"))
        }

        @Test
        @DisplayName("10.000 if statement berurutan selesai dalam batas waktu")
        fun testManySequentialIfs() {
            val script = buildString {
                appendLine("counter = 0 m")
                repeat(10_000) { appendLine("if (counter >= 0 m) {\n counter = counter + 1 m\n}") }
                appendLine("print counter")
            }

            val output: String = assertTimeoutPreemptively<String>(Duration.ofSeconds(20)) { runAndGetOutput(script) }

            assertEquals("10000.0 m", output)
        }

        @Test
        @DisplayName("Nilai ekstrem: sangat besar, sangat kecil, tak hingga, dan toleransi nol")
        fun testExtremeValues() {
            assertOutput("""
                huge
                infinite
                infinite equal
                exact
                strict
            """, """
                big = (10 ^ 300) * 1 m
                tiny = 1 m / (10 ^ 300)
                if (big > tiny) {
                    print "huge"
                }
                inf = 1 m / 0 s
                if (inf > 1000000 m/s) {
                    print "infinite"
                }
                if (inf == inf) {
                    print "infinite equal"
                }
                if (1 m == 1 m within 0 m) {
                    print "exact"
                }
                if (1 m == 1.0000001 m) {
                    print "BRANCH"
                } else {
                    print "strict"
                }
            """)
        }
    }

    @Nested
    @DisplayName("Edge case ekstrem dan input tidak valid")
    inner class EdgeCases {

        @Test
        @DisplayName("Membandingkan dimensi berbeda ditolak")
        fun testDimensionMismatch() {
            assertRejected("if (5 m > 2 kg) {\n print \"BRANCH\"\n}", "cannot compare 'm' with 'kg'")
            assertRejected("if (3 m/s < 2 m/s^2) {\n print \"BRANCH\"\n}", "Dimension mismatch in comparison")
        }

        @Test
        @DisplayName("Besaran berdimensi tidak boleh dibandingkan dengan angka tanpa satuan")
        fun testQuantityVersusScalar() {
            assertRejected("v = 3 m/s\nif (v > 0) {\n print \"BRANCH\"\n}", "with 'dimensionless'")
        }

        @Test
        @DisplayName("Dimensi toleransi dan batas rentang wajib sama")
        fun testWithinAndBetweenDimensionMismatch() {
            assertRejected("if (1 m == 1 m within 1 kg) {\n print \"BRANCH\"\n}", "Tolerance dimension mismatch")
            assertRejected("if (1 m between 0 m and 2 kg) {\n print \"BRANCH\"\n}", "Dimension mismatch in range")
        }

        @Test
        @DisplayName("Operand bukan besaran fisika ditolak: string, null, formula, dan nama tak terdefinisi")
        fun testNonQuantityOperands() {
            assertRejected("if (\"a\" == \"a\") {\n print \"BRANCH\"\n}", "requires physics quantities, got string and string")
            assertRejected("if (null > 1 m) {\n print \"BRANCH\"\n}", "cannot be applied to null operand")
            assertRejected("form f(x) = x\nif (f > 1 m) {\n print \"BRANCH\"\n}", "got formula and physics quantity")
            assertRejected("if (unknownName > 1 m) {\n print \"BRANCH\"\n}", "'unknownName' is not defined")
        }

        @Test
        @DisplayName("Kondisi tanpa relasi fisis ditolak (tidak ada truthiness)")
        fun testNoTruthiness() {
            assertRejected("x = 1 m\nif (x) {\n print \"BRANCH\"\n}", "Expected comparison operator")
            assertRejected("if (a = 5 m) {\n print \"BRANCH\"\n}", "Expected comparison operator")
        }

        @Test
        @DisplayName("Target 'is' wajib ekspresi satuan murni")
        fun testInvalidDimensionCheckTarget() {
            assertRejected("v = 2 m\nif (v is v) {\n print \"BRANCH\"\n}", "must be a unit expression")
            assertRejected("if (2 m is 5) {\n print \"BRANCH\"\n}", "must be a unit expression")
            assertRejected("if (2 m is \"N\") {\n print \"BRANCH\"\n}", "must be a unit expression")
            assertRejected("m = 5 kg\nif (2 kg is m) {\n print \"BRANCH\"\n}", "must be a unit expression")
        }

        @Test
        @DisplayName("If ekspresi tanpa else, cabang beda dimensi, beda tipe, atau blok tanpa nilai ditolak")
        fun testInvalidIfExpressions() {
            assertRejected("a = if (1 m > 0 m) 1 m\nprint \"BRANCH\"", "must have an 'else' branch")
            assertRejected("a = if (1 m > 0 m) 5 m/s else 3 kg\nprint \"BRANCH\"", "Dimension mismatch between if branches")
            assertRejected("a = if (1 m > 0 m) \"text\" else 3 kg\nprint \"BRANCH\"", "Incompatible if branch types")
            assertRejected(
                "a = if (1 m > 0 m) {\n print \"x\"\n} else {\n 1 m\n}\nprint \"BRANCH\"",
                "must end with an expression"
            )
        }

        @Test
        @DisplayName("Kesalahan sintaks pada kondisi dan kurung ditolak")
        fun testConditionSyntaxErrors() {
            assertRejected("if 5 m > 2 m {\n print \"BRANCH\"\n}", "Expected '(' after 'if'")
            assertRejected("if () {\n print \"BRANCH\"\n}", "empty condition")
            assertRejected("if (1 m > 0 m {\n print \"BRANCH\"\n}", "Expected ')' to close if condition")
            assertRejected("if (1 m < 2 m < 3 m) {\n print \"BRANCH\"\n}", "Expected ')' to close if condition, got '<'")
            assertRejected("if (1 m < 2 m within 1 m) {\n print \"BRANCH\"\n}", "'within' can only follow '==' or '!='")
            assertRejected("if (1 m between 0 m 2 m) {\n print \"BRANCH\"\n}", "Expected 'and'")
        }

        @Test
        @DisplayName("Kesalahan struktur cabang dan else ditolak")
        fun testBranchSyntaxErrors() {
            assertRejected("if (1 m > 0 m) {\n print \"BRANCH\"\n", "Expected '}'")
            assertRejected("if (1 m > 0 m) print \"BRANCH\"", "must be wrapped in '{ }'")
            assertRejected("else {\n print \"BRANCH\"\n}", "Unexpected token 'else'")
            assertRejected("if (1 m > 0 m) {\n print \"BRANCH\"\n} else {\n print \"BRANCH\"\n} else {\n print \"BRANCH\"\n}", "'else'")
            assertRejected("if (1 m > 0 m) {\n print \"BRANCH\"\n} else", "Expected expression or '{' after 'else'")
            assertRejected("if (1 m > 0 m) {\n print \"BRANCH\"\n} else (1 m > 0 m) {\n print \"BRANCH\"\n}")
        }

        @Test
        @DisplayName("Operator pembanding di luar if tetap ditolak")
        fun testComparisonOutsideIf() {
            assertRejected("print 5 m > 2 m", "Unexpected token '>'")
        }

        @Test
        @DisplayName("Keyword baru tidak bisa dipakai sebagai nama variabel")
        fun testKeywordsAsIdentifiers() {
            assertRejected("if = 5 m", "Expected '(' after 'if'")
            assertRejected("is = 2 m", "Unexpected token 'is'")
            assertRejected("and = 1 kg", "Unexpected token 'and'")
            assertRejected("between = 1 kg", "Unexpected token 'between'")
            assertRejected("within = 1 kg", "Unexpected token 'within'")
        }

        @Test
        @DisplayName("Toleransi negatif dan rentang terbalik menghasilkan runtime error")
        fun testRuntimeRangeErrors() {
            assertRejected("if (1 m == 1 m within -1 m) {\n print \"BRANCH\"\n}", "RUNTIME ERROR", "must not be negative")
            assertRejected("if (1 m between 10 m and 1 m) {\n print \"BRANCH\"\n}", "RUNTIME ERROR", "Invalid range")
        }

        @Test
        @DisplayName("NaN tidak pernah memenuhi perbandingan dan tidak membuat crash")
        fun testNaNCondition() {
            assertOutput("""
                not greater
                not equal
                not in range
            """, """
                nan = 0 m / 0 m
                if (nan > 1) {
                    print "BRANCH"
                } else {
                    print "not greater"
                }
                if (nan != 1) {
                    print "not equal"
                }
                if (nan between 0 and 1) {
                    print "BRANCH"
                } else {
                    print "not in range"
                }
            """)
        }
    }

    @Nested
    @DisplayName("Contoh pada folder assets/fitur")
    inner class FeatureExamples {

        private fun runFeatureFile(name: String): String {
            return runAndGetOutput(File("assets/fitur/$name").readText())
        }

        @Test
        @DisplayName("15_if_else.phys")
        fun testIfElseExample() {
            assertEquals(
                """
                    Distance exceeds the limit
                    Distance is not below the limit
                    Moderate
                    1000 mm == 1 m
                    1 h != 59 min
                    60 s <= 1 min
                    2 kg >= 2000 g
                    Floating point rounding is absorbed by '=='
                """.trimIndent(),
                runFeatureFile("15_if_else.phys")
            )
        }

        @Test
        @DisplayName("16_if_expression.phys")
        fun testIfExpressionExample() {
            val output = runFeatureFile("16_if_expression.phys")

            assertFalse(output.contains("ERROR"), output)
            assertTrue(output.contains("Weight class: heavy"))
            assertTrue(output.contains("Potential energy: 98.0 J"))
            assertTrue(output.contains("Doubled length: 2.0 m"))
            assertTrue(output.contains("Mass is exactly 2 kg"))
            assertTrue(output.contains("Clamped 50 m/s: 30.0"))
        }

        @Test
        @DisplayName("17_physics_conditions.phys")
        fun testPhysicsConditionsExample() {
            assertEquals(
                """
                    Measurement agrees with standard gravity
                    But not within 1 mm/s^2
                    Water is liquid at this temperature
                    kg*m/s^2 has the dimensions of a force
                    Explicit unit expressions work too
                    Length + 1 m: 3.0 m
                    Mass + 1 kg: 4.0 kg
                    10 N -> force
                    5 kJ -> energy
                    2 kW -> power
                    3 m -> other quantity
                """.trimIndent(),
                runFeatureFile("17_physics_conditions.phys")
            )
        }
    }
}
