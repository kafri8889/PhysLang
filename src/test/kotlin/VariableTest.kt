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
    @DisplayName("Deklarasi variabel dengan besaran fisika dan print")
    fun testVariableDeclarationAndPrint() {
        val script = """
            var distance = 100 m
            print distance
        """.trimIndent()

        assertEquals("100.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Aritmetika antar variabel berdimensi sama")
    fun testVariableArithmetic() {
        val script = """
            var a = 2 km
            var b = 500 m
            var total = a + b
            print total
        """.trimIndent()

        // 2 km + 500 m = 2.5 km
        assertEquals("2.5 km", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Penugasan ulang variabel (Reassignment) dengan dimensi yang valid")
    fun testVariableReassignment() {
        val script = """
            var speed = 10 m / s
            speed = 25 m / s
            print speed
        """.trimIndent()

        assertEquals("25.0 m*s^-1", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Penugasan ulang variabel (Reassignment) dengan operasi matematika yang valid")
    fun testVariableReassignmentWithMath() {
        val script = """
            var mass = 50 kg
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
    @DisplayName("Menolak deklarasi variabel ganda dengan nama yang sama pada scope yang sama")
    fun testDuplicateVariableDeclaration() {
        val script = """
            var x = 10 m
            var x = 20 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:") && output.contains("already defined"))
    }

    @Test
    @DisplayName("Menolak penugasan nilai ke variabel yang belum pernah dideklarasikan")
    fun testReassignmentToUndeclaredVariable() {
        val script = """
            speed = 100 m / s
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:") && output.contains("not defined"))
    }

    // ==========================================
    // TIER: NULLABLE & UNINITIALIZED VARIABLES
    // ==========================================

    @Test
    @DisplayName("Deklarasi variabel tanpa inisialisasi awal (var a)")
    fun testUninitializedVariableDeclaration() {
        val script = """
            var a
            a = 15 m
            print a
        """.trimIndent()

        assertEquals("15.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Deklarasi variabel bernilai null eksplisit (var a = null)")
    fun testExplicitNullVariableDeclaration() {
        val script = """
            var a = null
            a = 50 kg
            print a
        """.trimIndent()

        assertEquals("50.0 kg", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Mencetak variabel yang belum diinisialisasi harus menampilkan null")
    fun testPrintUninitializedVariable() {
        val script = """
            var a
            print a
        """.trimIndent()

        assertEquals("null", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Menolak operasi matematika jika salah satu variabel masih belum diinisialisasi / null")
    fun testRejectMathOnUninitializedVariable() {
        val script = """
            var a
            var b = a + 10 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus menolak operasi matematika pada variabel null/belum diinisialisasi")
    }

    @Test
    @DisplayName("Penugasan nilai (assignment) berturut-turut pada variabel yang sama")
    fun testSequentialReassignments() {
        val script = """
            var x = 1 m
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
            var a
            print (a = 10 m)
        """.trimIndent()

        assertEquals("10.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Chained Assignment: memberikan nilai sekaligus ke beberapa variabel (a = b = 25 s)")
    fun testChainedAssignment() {
        val script = """
            var a
            var b
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
            var a
            var result = (a = 5 m) + 10 m
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
            var x
            var y
            var z
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
            var x = 10 m
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
            var d1 = 1 km
            var d2 = 200 m
            var d3 = 50 cm
            var d4 = 30 mm
            var total = d1 + d2 + d3 + d4
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
            var v0 = 20 m
            var t = 5
            var a = 2 m
            var dist = (v0 * t) + (0.5 * a * t * t)
            print dist
        """.trimIndent()

        // (20 * 5) + (0.5 * 2 * 25) = 100 + 25 = 125.0 m
        assertEquals("125.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Stress Test: Hukum Kekekalan Energi Mekanik (Ep + Ek)")
    fun testMechanicalEnergyFormula() {
        val script = """
            var m = 2 kg
            var g = 9.8 m / s^2
            var h = 10 m
            var v = 10 m / s
            var ep = m * g * h
            var ek = 0.5 * m * (v ^ 2)
            var em = ep + ek
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
            var p
            var q
            var velocity = ((p = 20 m) + (q = 30 m)) / 5 s
            print p
            print q
            print velocity
        """.trimIndent()

        val expected = """
            50.0 m
            30.0 m
            10.0 m*s^-1
        """.trimIndent()

        // p bernilai total ekspresi p = 20 m, lalu diuji
        val output = runAndGetOutput(script)
        assertTrue(output.contains("10.0"), "Velocity harus 10.0 m*s^-1")
    }

    @Test
    @DisplayName("Stress Test: Rantai 10 variabel saling bergantungan (Cascading Variables)")
    fun testCascadingTenVariables() {
        val script = """
            var v1 = 1 m
            var v2 = v1 + 1 m
            var v3 = v2 + 1 m
            var v4 = v3 + 1 m
            var v5 = v4 + 1 m
            var v6 = v5 + 1 m
            var v7 = v6 + 1 m
            var v8 = v7 + 1 m
            var v9 = v8 + 1 m
            var v10 = v9 + 1 m
            print v10
        """.trimIndent()

        assertEquals("10.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Stress Error: Menolak assignment ke ekspresi biner bukan L-value ((a + 5 m) = 10 m)")
    fun testRejectInvalidAssignmentTargetBinaryExpr() {
        val script = """
            var a = 2 m
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
    @DisplayName("Stress Error: Menolak variabel yang menginisialisasi dirinya sendiri sebelum selesai (var a = a + 5 m)")
    fun testRejectSelfReferencingInitialization() {
        val script = """
            var a = a + 5 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Harus menolak variabel yang belum terdefinisi saat inisialisasi")
    }
}

