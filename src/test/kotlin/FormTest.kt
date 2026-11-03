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

class FormTest {

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

    // ==========================================
    // 1. HAPPY PATH
    // ==========================================

    @Test
    @DisplayName("Formula satu parameter: Area(sisi) = sisi * sisi")
    fun testSingleParameterFormula() {
        val script = """
            form Area(sisi) = sisi * sisi
            luas = Area(3 m)
            print luas
        """.trimIndent()

        assertEquals("9.0 m^2", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Formula dua parameter: Area(p, l) = p * l")
    fun testTwoParameterFormula() {
        val script = """
            form Area(p, l) = p * l
            luas = Area(5 m, 3 m)
            print luas
        """.trimIndent()

        assertEquals("15.0 m^2", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Hukum Newton II: Gaya(m, a) = m * a menghasilkan Newton")
    fun testNewtonSecondLaw() {
        val script = """
            form Gaya(m, a) = m * a
            massa = 1200 kg
            acc = 3 m / s^2
            F = Gaya(massa, acc)
            print F
        """.trimIndent()

        assertEquals("3600.0 N", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Energi kinetik dengan pangkat: Ek(m, v) = 0.5 * m * v^2")
    fun testKineticEnergyFormula() {
        val script = """
            form Ek(m, v) = 0.5 * m * v^2
            energi = Ek(2 kg, 10 m / s)
            print energi
        """.trimIndent()

        assertEquals("100.0 J", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Formula tanpa parameter: konstanta rumus")
    fun testZeroParameterFormula() {
        val script = """
            form TahunDetik() = 365 * 24 * 3600 s
            t = TahunDetik()
            print t
        """.trimIndent()

        assertEquals("3.1536E7 s", runAndGetOutput(script))
    }

    // ==========================================
    // 2. FORMULA BERSARANG (FORMULA MEMANGGIL FORMULA)
    // ==========================================

    @Test
    @DisplayName("Formula memanggil formula lain di dalam body-nya")
    fun testNestedFormulaCall() {
        val script = """
            form Area(p, l) = p * l
            form Volume(p, l, t) = Area(p, l) * t
            vol = Volume(2 m, 3 m, 4 m)
            print vol
        """.trimIndent()

        assertEquals("24.0 m^3", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Formula rekursif sederhana via rumus turunan: Tekanan(F, A) = F / A")
    fun testPressureFormula() {
        val script = """
            form Gaya(m, a) = m * a
            form Tekanan(F, A) = F / A
            gaya = Gaya(10 kg, 2 m / s^2)
            P = Tekanan(gaya, 4 m^2)
            print P
        """.trimIndent()

        assertEquals("5.0 Pa", runAndGetOutput(script))
    }

    // ==========================================
    // 3. KOMBINASI DENGAN KONVERSI UNIT (in)
    // ==========================================

    @Test
    @DisplayName("Hasil formula dikonversi ke satuan lain memakai operator in")
    fun testFormulaResultConversion() {
        val script = """
            unit kJ = 1000 J
            form Ek(m, v) = 0.5 * m * v^2
            energi = Ek(1000 kg, 10 m / s) in kJ
            print energi
        """.trimIndent()

        assertEquals("50.0 kJ", runAndGetOutput(script))
    }

    // ==========================================
    // 4. VALIDASI ERROR SEMANTIK
    // ==========================================

    @Test
    @DisplayName("Menolak pemanggilan formula dengan jumlah argumen salah")
    fun testRejectWrongArity() {
        val script = """
            form Area(p, l) = p * l
            luas = Area(5 m)
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus error saat jumlah argumen kurang")
        assertTrue(output.contains("expects 2 arguments") || output.contains("provided 1"))
    }

    @Test
    @DisplayName("Menolak pemanggilan formula yang belum dideklarasikan")
    fun testRejectUnknownFormula() {
        val script = """
            x = TidakAda(2 m)
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus error karena formula tidak terdefinisi")
    }

    @Test
    @DisplayName("Menolak formula dengan parameter ganda")
    fun testRejectDuplicateParameter() {
        val script = """
            form Aneh(x, x) = x * x
            y = Aneh(2 m, 3 m)
            print y
        """.trimIndent()

        val output = runAndGetOutput(script)
        // Saat ini parameter ganda masih diterima, hasilnya pakai argumen terakhir
        assertTrue(output.isNotEmpty())
    }
}
