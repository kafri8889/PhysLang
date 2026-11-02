import lang.cli.PhysRunner
import lang.core.Environment
import lang.error.ErrorReporter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariableTest {

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

    @Test
    @DisplayName("Deklarasi variabel dengan besaran fisika dan print (id = expr)")
    fun testVariableDeclarationAndPrint() {
        val script = """
            distance = 100 m
            print distance
        """.trimIndent()

        assertEquals("100.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Aritmetika antar variabel berdimensi sama")
    fun testVariableArithmetic() {
        val script = """
            a = 2 km
            b = 500 m
            total = a + b
            print total
        """.trimIndent()

        // 2 km + 500 m = 2.5 km
        assertEquals("2.5 km", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Penugasan ulang variabel (Reassignment) dengan dimensi yang valid")
    fun testVariableReassignment() {
        val script = """
            speed = 10 m / s
            speed = 25 m / s
            print speed
        """.trimIndent()

        assertEquals("25.0 m*s^-1", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Penugasan ulang variabel (Reassignment) dengan operasi matematika yang valid")
    fun testVariableReassignmentWithMath() {
        val script = """
            mass = 50 kg
            mass = mass + 10 kg
            mass = mass - 5 kg
            print mass
        """.trimIndent()

        assertEquals("55.0 kg", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Menolak penggunaan variabel yang belum didefinisikan (Static Semantic Error)")
    fun testUndeclaredVariableError() {
        val script = """
            print undefinedVar
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:") && output.contains("not defined"))
    }

    @Test
    @DisplayName("Menolak penggunaan keyword legacy 'var' untuk deklarasi variabel")
    fun testRejectLegacyVarKeyword() {
        val script = """
            var x = 10 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus menolak keyword var yang sudah tidak didukung di v2")
    }

    @Test
    @DisplayName("Penugasan langsung tanpa var (id = expr) secara otomatis mendeklarasikan variabel baru")
    fun testDirectAssignmentDeclaresVariable() {
        val script = """
            speed = 100 m / s
            print speed
        """.trimIndent()

        assertEquals("100.0 m*s^-1", runAndGetOutput(script))
    }

    // ==========================================
    // TIER: NULLABLE & UNINITIALIZED VARIABLES
    // ==========================================

    @Test
    @DisplayName("Inisialisasi null dan penugasan nilai berikutnya (a = null)")
    fun testUninitializedVariableDeclaration() {
        val script = """
            a = null
            a = 15 m
            print a
        """.trimIndent()

        assertEquals("15.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Deklarasi variabel bernilai null eksplisit (a = null)")
    fun testExplicitNullVariableDeclaration() {
        val script = """
            a = null
            a = 50 kg
            print a
        """.trimIndent()

        assertEquals("50.0 kg", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Mencetak variabel bernilai null harus menampilkan null")
    fun testPrintUninitializedVariable() {
        val script = """
            a = null
            print a
        """.trimIndent()

        assertEquals("null", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Menolak operasi matematika jika salah satu variabel masih bernilai null")
    fun testRejectMathOnUninitializedVariable() {
        val script = """
            a = null
            b = a + 10 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus menolak operasi matematika pada variabel null/belum diinisialisasi")
    }

    @Test
    @DisplayName("Penugasan nilai (assignment) berturut-turut pada variabel yang sama")
    fun testSequentialReassignments() {
        val script = """
            x = 1 m
            x = 2 m
            x = 3 m
            x = 4 m
            print x
        """.trimIndent()

        assertEquals("4.0 m", runAndGetOutput(script))
    }

    // ==========================================
    // TIER: ASSIGNMENT AS EXPRESSION
    // ==========================================

    @Test
    @DisplayName("Assignment expression menghasilkan nilai yang bisa langsung dicetak (print (a = 10 m))")
    fun testAssignmentAsPrintableExpression() {
        val script = """
            print (a = 10 m)
        """.trimIndent()

        assertEquals("10.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Chained Assignment: memberikan nilai sekaligus ke beberapa variabel (a = b = 25 s)")
    fun testChainedAssignment() {
        val script = """
            a = b = 25 s
            print a
            print b
        """.trimIndent()

        val expected = """
            25.0 s
            25.0 s
        """.trimIndent()

        assertEquals(expected, runAndGetOutput(script))
    }

    @Test
    @DisplayName("Assignment di dalam ekspresi matematika (misal (a = 5 m) + 10 m)")
    fun testAssignmentEmbeddedInArithmeticExpression() {
        val script = """
            result = (a = 5 m) + 10 m
            print a
            print result
        """.trimIndent()

        val expected = """
            5.0 m
            15.0 m
        """.trimIndent()

        assertEquals(expected, runAndGetOutput(script))
    }

    @Test
    @DisplayName("Chained Assignment dengan 3 variabel sekaligus (x = y = z = 100 N)")
    fun testTripleChainedAssignment() {
        val script = """
            x = y = z = 100 N
            print x
            print y
            print z
        """.trimIndent()

        val expected = """
            100.0 N
            100.0 N
            100.0 N
        """.trimIndent()

        assertEquals(expected, runAndGetOutput(script))
    }

    // ==========================================
    // STRESS TESTS & COMPLEX FORMULAS
    // ==========================================

    @Test
    @DisplayName("Stress Test: Akumulasi dan mutasi berturut-turut pada variabel yang sama")
    fun testSelfReferentialAccumulation() {
        val script = """
            x = 10 m
            x = x + 5 m
            x = x * 2
            x = x - 10 m
            print x
        """.trimIndent()

        // (10 + 5) * 2 - 10 = 20.0 m
        assertEquals("20.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Stress Test: Rantai konversi multi-unit (km, m, cm, mm) dijumlahkan ke satu variabel")
    fun testMultiUnitChainAddition() {
        val script = """
            d1 = 1 km
            d2 = 200 m
            d3 = 50 cm
            d4 = 30 mm
            total = d1 + d2 + d3 + d4
            print total
        """.trimIndent()

        // 1 km + 0.2 km + 0.0005 km + 0.00003 km = 1.20053 km
        val output = runAndGetOutput(script)
        assertTrue(output.startsWith("1.2005"), "Hasil penjumlahan multi-unit harus 1.20053 km, got: $output")
    }

    @Test
    @DisplayName("Stress Test: Perhitungan Kinematika GLBB s = v0*t + 0.5*a*t^2")
    fun testKinematicsFormulaStressTest() {
        val script = """
            v0 = 20 m
            t = 5
            a = 2 m
            dist = (v0 * t) + (0.5 * a * t * t)
            print dist
        """.trimIndent()

        // (20 * 5) + (0.5 * 2 * 25) = 100 + 25 = 125.0 m
        assertEquals("125.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Stress Test: Hukum Kekekalan Energi Mekanik (Ep + Ek)")
    fun testMechanicalEnergyFormula() {
        val script = """
            m = 2 kg
            g = 9.8 m / s^2
            h = 10 m
            v = 10 m / s
            ep = m * g * h
            ek = 0.5 * m * (v ^ 2)
            em = ep + ek
            print em
        """.trimIndent()

        // Ep = 2 * 9.8 * 10 = 196 J
        // Ek = 0.5 * 2 * 100 = 100 J
        // Em = 296 J (kg*m^2*s^-2)
        val output = runAndGetOutput(script)
        assertTrue(output.contains("296.0"), "Hasil energi mekanik harus 296.0 J")
    }

    @Test
    @DisplayName("Stress Test: Assignment bersarang di dalam operasi pembagian dan penjumlahan")
    fun testNestedAssignmentInDivision() {
        val script = """
            velocity = ((p = 20 m) + (q = 30 m)) / 5 s
            print p
            print q
            print velocity
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("10.0"), "Velocity harus 10.0 m*s^-1")
    }

    @Test
    @DisplayName("Stress Test: Rantai 10 variabel saling bergantungan (Cascading Variables)")
    fun testCascadingTenVariables() {
        val script = """
            v1 = 1 m
            v2 = v1 + 1 m
            v3 = v2 + 1 m
            v4 = v3 + 1 m
            v5 = v4 + 1 m
            v6 = v5 + 1 m
            v7 = v6 + 1 m
            v8 = v7 + 1 m
            v9 = v8 + 1 m
            v10 = v9 + 1 m
            print v10
        """.trimIndent()

        assertEquals("10.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Stress Error: Menolak assignment ke ekspresi biner bukan L-value ((a + 5 m) = 10 m)")
    fun testRejectInvalidAssignmentTargetBinaryExpr() {
        val script = """
            a = 2 m
            (a + 5 m) = 10 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Harus menolak penugasan ke target non-identifier")
    }

    @Test
    @DisplayName("Stress Error: Menolak assignment langsung ke angka literal (5 = 10 m)")
    fun testRejectAssignmentToLiteralNumber() {
        val script = """
            5 = 10 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Harus menolak penugasan ke angka literal")
    }

    @Test
    @DisplayName("Stress Error: Menolak variabel yang menginisialisasi dirinya sendiri sebelum selesai (a = a + 5 m)")
    fun testRejectSelfReferencingInitialization() {
        val script = """
            a = a + 5 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Harus menolak variabel yang belum terdefinisi saat inisialisasi")
    }
}
