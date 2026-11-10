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

/**
 * Compound assignment, sqrt typing, underscore identifiers, scientific notation,
 * hash and trailing comments, dimensionless display, and the sequential file runner.
 */
class LanguageFixesTest {

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

    private fun assertRejected(script: String, vararg expectedMessages: String) {
        val output = runAndGetOutput(script.trimIndent())

        assertTrue(output.contains("ERROR"), "Expected an error, got:\n$output")
        assertFalse(output.lines().any { it.trim() == "UNREACHABLE" }, "Script should not run:\n$output")

        for (message in expectedMessages) {
            assertTrue(output.contains(message), "Expected '$message' in output:\n$output")
        }
    }

    /**
     * Runs a file through the per-statement runner used by `./run.sh file.phys`.
     */
    private fun runFileDirectly(script: String): String {
        val file = File.createTempFile("physlang", ".phys")
        try {
            file.writeText(script.trimIndent())
            outputStreamCaptor.reset()
            ExampleRunner().runFileDirectly(file)
            return outputStreamCaptor.toString().trim().replace("\r\n", "\n")
        } finally {
            file.delete()
        }
    }

    @Nested
    @DisplayName("Tingkat mudah")
    inner class Easy {

        @Test
        @DisplayName("Keempat assignment majemuk (+=, -=, *=, /=) bekerja")
        fun testCompoundAssignments() {
            assertOutput("3.0 m", """
                x = 2 m
                x += 3 m
                x *= 2
                x -= 1 m
                x /= 3
                print x
            """)
        }

        @Test
        @DisplayName("Hasil sqrt dapat disimpan lalu dipakai dalam aritmetika")
        fun testSqrtResultIsQuantity() {
            assertOutput("5.0 m", """
                side = sqrt(16 m^2)
                print side + 1 m
            """)
        }

        @Test
        @DisplayName("Identifier boleh memakai underscore")
        fun testUnderscoreIdentifier() {
            assertOutput("10.0 J", """
                kinetic_energy = 10 J
                print kinetic_energy
            """)
        }

        @Test
        @DisplayName("Notasi ilmiah pada literal angka dan besaran")
        fun testScientificNotation() {
            assertOutput("""
                1500.0 m
                1000.0
            """, """
                print 1.5e3 m
                print 1e3
            """)
        }

        @Test
        @DisplayName("Komentar '#' satu baris diabaikan")
        fun testHashComment() {
            assertOutput("4.0 m", """
                # full line comment
                print 4 m
            """)
        }

        @Test
        @DisplayName("Angka tak berdimensi tidak lagi berlabel 'rad'")
        fun testDimensionlessDisplay() {
            assertOutput("""
                64.0
                2.5
            """, """
                print 2 ^ 3 ^ 2
                print 5 m / 2 m
            """)
        }
    }

    @Nested
    @DisplayName("Tingkat medium")
    inner class Medium {

        @Test
        @DisplayName("Assignment majemuk mengikuti aturan dimensi dan skala")
        fun testCompoundAssignmentDimensions() {
            assertOutput("""
                2.5 km
                2.0 m^2
            """, """
                d = 2 km
                d += 500 m
                print d
                area = 1 m
                area *= 2 m
                print area
            """)
        }

        @Test
        @DisplayName("'+=' pada string melakukan penggabungan")
        fun testStringCompoundConcatenation() {
            assertOutput("Force: 5.0 N", """
                label = "Force: "
                label += 5 N
                print label
            """)
        }

        @Test
        @DisplayName("Ruas kanan assignment majemuk dievaluasi utuh (x *= 2 + 1 berarti x = x * 3)")
        fun testCompoundAssignmentRightSide() {
            assertOutput("6.0 m", """
                x = 2 m
                x *= 2 + 1
                print x
            """)
        }

        @Test
        @DisplayName("sqrt membagi dua pangkat dimensi turunan")
        fun testSqrtDerivedDimensions() {
            assertOutput("""
                3.0
                2.0 m*s^-1
                1000.0 m
            """, """
                print sqrt(9)
                print sqrt(4 m^2 / s^2)
                print sqrt(1 km^2)
            """)
        }

        @Test
        @DisplayName("Variasi notasi ilmiah: E besar, eksponen negatif dan positif")
        fun testScientificNotationVariants() {
            assertOutput("""
                2.0 g
                6.02E23 mol
                0.001 kg
            """, """
                print 2E-3 kg in g
                print 6.02e+23 * 1 mol
                print 1e-3 kg
            """)
        }

        @Test
        @DisplayName("Satuan berawalan 'e' tanpa spasi tidak dianggap eksponen (3eV)")
        fun testUnitStartingWithE() {
            val output = runAndGetOutput("print 3eV in eV")

            assertEquals("3.0 eV", output)
        }

        @Test
        @DisplayName("Komentar di akhir baris dengan '#' maupun '//'")
        fun testTrailingComments() {
            assertOutput("9.0 m", """
                x = 4 m   # trailing hash comment
                y = 5 m   // trailing slash comment
                print x + y
            """)
        }

        @Test
        @DisplayName("Underscore di awal, tengah, dan sebagai satu-satunya karakter")
        fun testUnderscorePositions() {
            assertOutput("""
                1.0 m
                2.0 m
                3.0 m
            """, """
                _hidden = 1 m
                v_0 = 2 m
                _ = 3 m
                print _hidden
                print v_0
                print _
            """)
        }

        @Test
        @DisplayName("Satuan tak berdimensi eksplisit tetap ditampilkan")
        fun testExplicitDimensionlessUnits() {
            assertOutput("""
                90.0 deg
                1.0 rad
            """, """
                print 90 deg
                print 1 rad
            """)
        }
    }

    @Nested
    @DisplayName("Tingkat sulit")
    inner class Hard {

        @Test
        @DisplayName("Runner file per-statement tidak lagi menimpa nilai variabel saat x = x + ...")
        fun testSequentialRunnerSelfReference() {
            assertEquals(
                "5.0 m\n3.0 m\nab",
                runFileDirectly("""
                    y = 1 m
                    y = y + 4 m
                    print y
                    x = 2 m
                    x += 3 m
                    x *= 2
                    x -= 1 m
                    x /= 3
                    print x
                    label = "a"
                    label += "b"
                    print label
                """)
            )
        }

        @Test
        @DisplayName("Assignment majemuk di dalam blok, cabang if, dan rumus")
        fun testCompoundAssignmentInScopes() {
            assertOutput("""
                3.0 m
                accelerated
            """, """
                x = 1 m
                {
                    x += 1 m
                }
                if (x == 2 m) {
                    x += 1 m
                }
                print x
                v = 10 m/s
                v *= 3
                if (v > 20 m/s) {
                    print "accelerated"
                }
            """)
        }

        @Test
        @DisplayName("sqrt dalam rumus dan kondisi if")
        fun testSqrtInFormAndCondition() {
            assertOutput("""
                5.0 m
                hypotenuse ok
            """, """
                form hypotenuse(a, b) = sqrt(a^2 + b^2)
                print hypotenuse(3 m, 4 m)
                if (hypotenuse(3 m, 4 m) == 5 m) {
                    print "hypotenuse ok"
                }
            """)
        }

        @Test
        @DisplayName("Notasi ilmiah pada konstanta fisika dan pangkat satuan")
        fun testScientificNotationPhysics() {
            val output = runAndGetOutput("""
                G = 6.674e-11 N * m^2 / kg^2
                earthMass = 5.972e24 kg
                radius = 6.371e6 m
                g = G * earthMass / radius^2
                print g
            """.trimIndent())

            assertFalse(output.contains("ERROR"), output)
            assertTrue(output.startsWith("9.8"), output)
        }

        @Test
        @DisplayName("Komentar '#' di dalam blok dan percabangan")
        fun testHashCommentsInsideBlocks() {
            assertOutput("inside", """
                if (1 m > 0 m) { # condition holds
                    # comment line inside branch
                    print "inside"  # trailing
                } # after branch
                else {
                    print "UNREACHABLE"
                }
            """)
        }
    }

    @Nested
    @DisplayName("Stress test")
    inner class Stress {

        @Test
        @DisplayName("10.000 assignment majemuk berurutan selesai dalam batas waktu")
        fun testManyCompoundAssignments() {
            val script = buildString {
                appendLine("total = 0 m")
                repeat(10_000) { appendLine("total += 1 m") }
                appendLine("print total")
            }

            val output: String = assertTimeoutPreemptively<String>(Duration.ofSeconds(20)) { runAndGetOutput(script) }

            assertEquals("10000.0 m", output)
        }

        @Test
        @DisplayName("Identifier dengan underscore sangat panjang")
        fun testVeryLongIdentifier() {
            val name = "very_long_" + "segment_".repeat(500) + "end"

            assertEquals("7.0 m", runAndGetOutput("$name = 7 m\nprint $name"))
        }

        @Test
        @DisplayName("Eksponen ilmiah ekstrem: overflow menjadi Infinity, underflow menjadi 0")
        fun testExtremeExponents() {
            assertOutput("""
                Infinity m
                0.0 m
            """, """
                print 1e308 m * 10
                print 1e-400 m
            """)
        }

        @Test
        @DisplayName("Ribuan baris komentar '#' dan '//' bercampur")
        fun testManyComments() {
            val script = buildString {
                repeat(5_000) { appendLine("# hash $it"); appendLine("// slash $it") }
                appendLine("print 1 m")
            }

            assertEquals("1.0 m", runAndGetOutput(script))
        }
    }

    @Nested
    @DisplayName("Edge case ekstrem dan input tidak valid")
    inner class EdgeCases {

        @Test
        @DisplayName("Assignment majemuk dengan dimensi berbeda ditolak")
        fun testCompoundAssignmentDimensionMismatch() {
            assertRejected("x = 2 m\nx += 3 kg\nprint \"UNREACHABLE\"", "Dimension mismatch")
            assertRejected("x = 2 m\nx -= 1 s\nprint \"UNREACHABLE\"", "Dimension mismatch")
        }

        @Test
        @DisplayName("Assignment majemuk ke variabel tak terdefinisi, null, dan string yang dikurangi ditolak")
        fun testInvalidCompoundTargets() {
            assertRejected("undefinedVar += 1 m\nprint \"UNREACHABLE\"", "'undefinedVar' is not defined")
            assertRejected("x = null\nx += 1 m\nprint \"UNREACHABLE\"", "null operand")
            assertRejected("label = \"a\"\nlabel -= 1 m\nprint \"UNREACHABLE\"", "Cannot use '-' with string")
        }

        @Test
        @DisplayName("Assignment majemuk ke bukan-variabel ditolak")
        fun testCompoundAssignmentToNonIdentifier() {
            assertRejected("5 m += 1 m\nprint \"UNREACHABLE\"")
        }

        @Test
        @DisplayName("sqrt dengan pangkat dimensi ganjil ditolak")
        fun testSqrtOddDimensions() {
            assertRejected("print sqrt(8 m^3)\nprint \"UNREACHABLE\"", "requires even dimension powers", "m^3")
            assertRejected("print sqrt(2 m)\nprint \"UNREACHABLE\"", "requires even dimension powers")
            assertRejected("print sqrt(4 N)\nprint \"UNREACHABLE\"", "requires even dimension powers")
        }

        @Test
        @DisplayName("sqrt dengan argumen bukan besaran ditolak")
        fun testSqrtNonQuantity() {
            assertRejected("print sqrt(\"a\")\nprint \"UNREACHABLE\"", "only accepts physical quantities")
            assertRejected("print sqrt(null)\nprint \"UNREACHABLE\"", "only accepts physical quantities")
            assertRejected("print sqrt(1 m^2, 4 m^2)\nprint \"UNREACHABLE\"", "expects 1 arguments")
        }

        @Test
        @DisplayName("Notasi ilmiah tidak lengkap tidak dianggap eksponen")
        fun testIncompleteExponent() {
            // '2e' and '2e+' are the number 2 followed by an unknown unit 'e'
            assertRejected("print 2e\nprint \"UNREACHABLE\"", "Unknown physic unit: e")
            assertRejected("print 2e+ 3\nprint \"UNREACHABLE\"")
        }

        @Test
        @DisplayName("Eksponen ilmiah pada pangkat satuan bukan bilangan bulat literal")
        fun testScientificExponentInUnitPower() {
            assertRejected("print 4 m^2.5\nprint \"UNREACHABLE\"", "integers")
        }

        @Test
        @DisplayName("Underscore tidak membuat keyword menjadi identifier lain")
        fun testUnderscoreAroundKeywords() {
            assertOutput("""
                1.0 m
                2.0 m
            """, """
                if_value = 1 m
                _is = 2 m
                print if_value
                print _is
            """)
        }

        @Test
        @DisplayName("Karakter '#' di dalam string bukan komentar")
        fun testHashInsideString() {
            assertOutput("Sample #1", """
                print "Sample #1"
            """)
        }

        @Test
        @DisplayName("Komentar tidak boleh menyembunyikan token liar di tengah baris")
        fun testCommentDoesNotHideStrayTokens() {
            assertRejected("x = 4 m 5 # comment\nprint \"UNREACHABLE\"", "Unexpected token")
        }
    }
}
