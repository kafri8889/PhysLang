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

class ConversionExprTest {

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
    // 1. HAPPY PATH: KONVERSI SATUAN STANDAR
    // ==========================================

    @Test
    @DisplayName("Konversi besaran panjang m ke km")
    fun testConversionMeterToKilometer() {
        val script = """
            d = 1500 m in km
            print d
        """.trimIndent()

        assertEquals("1.5 km", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Konversi besaran massa kg ke g")
    fun testConversionKilogramToGram() {
        val script = """
            massa = 2.5 kg in g
            print massa
        """.trimIndent()

        assertEquals("2500.0 g", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Reassignment variabel sendiri menggunakan konversi: jarak = jarak in km")
    fun testSelfReassignmentWithConversion() {
        val script = """
            jarak = 2000 m
            jarak = jarak in km
            print jarak
        """.trimIndent()

        assertEquals("2.0 km", runAndGetOutput(script))
    }

    // ==========================================
    // 2. KONVERSI DENGAN CUSTOM UNIT
    // ==========================================

    @Test
    @DisplayName("Konversi kecepatan m/s ke custom unit kph (km/h)")
    fun testConversionToCustomUnitKph() {
        val script = """
            unit kph = 1000 m / 3600 s
            v = 20 m / s in kph
            print v
        """.trimIndent()

        assertEquals("72.0 kph", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Konversi energi kinetik ke custom unit kJ")
    fun testConversionEnergyToKilojoules() {
        val script = """
            unit kJ = 1000 J
            Ek = 5000 J in kJ
            print Ek
        """.trimIndent()

        assertEquals("5.0 kJ", runAndGetOutput(script))
    }

    // ==========================================
    // 3. KONVERSI DARI HASIL EKSPRESI ARITMETIKA
    // ==========================================

    @Test
    @DisplayName("Konversi langsung dari ekspresi penjumlahan: 1000 m + 500 m in km")
    fun testConversionAfterArithmeticExpression() {
        val script = """
            total = 1000 m + 500 m in km
            print total
        """.trimIndent()

        assertEquals("1.5 km", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Konversi perhitungan daya: F * v in custom horsepower/kW")
    fun testConversionFromMultiplication() {
        val script = """
            unit kW = 1000 W
            F = 500 N
            v = 10 m / s
            P = (F * v) in kW
            print P
        """.trimIndent()

        assertEquals("5.0 kW", runAndGetOutput(script))
    }

    // ==========================================
    // 4. VALIDASI ERROR SEMANTIK: DIMENSIONAL MISMATCH
    // ==========================================

    @Test
    @DisplayName("Menolak konversi antar dimensi berbeda: Panjang (m) ke Massa (kg)")
    fun testRejectDimensionalMismatch() {
        val script = """
            x = 10 m in kg
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus menghasilkan error semantik")
        assertTrue(output.contains("dimensional mismatch") || output.contains("Cannot convert"), "Pesan error harus menjelaskan ketidakcocokan dimensi")
    }

    @Test
    @DisplayName("Menolak konversi Waktu (s) ke Energi (J)")
    fun testRejectTimeToEnergyConversion() {
        val script = """
            waktu = 60 s in J
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus menghasilkan error semantik")
        assertTrue(output.contains("dimensional mismatch") || output.contains("Cannot convert"))
    }

    // ==========================================
    // 5. VALIDASI ERROR SEMANTIK: SATUAN TIDAK TERDEFINISI
    // ==========================================

    @Test
    @DisplayName("Menolak konversi ke satuan yang belum pernah didefinisikan")
    fun testRejectUndefinedTargetUnit() {
        val script = """
            speed = 20 m / s in cahayaCepat
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus menghasilkan error ketika satuan tidak terdaftar")
        assertTrue(output.contains("cahayaCepat is not defined") || output.contains("Unit 'cahayaCepat' is not defined"))
    }

    // ==========================================
    // 6. VALIDASI ERROR SEMANTIK: SISI KIRI BUKAN BESARAN FISIKA
    // ==========================================

    @Test
    @DisplayName("Menolak konversi jika sisi kiri adalah string literal")
    fun testRejectStringConversion() {
        val script = """
            val = "halo dunia" in km
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus menolak string yang dikonversi ke satuan")
        assertTrue(output.contains("requires a physics quantity") || output.contains("string"))
    }

    @Test
    @DisplayName("Menolak konversi jika sisi kiri adalah nilai null")
    fun testRejectNullConversion() {
        val script = """
            kosong = null
            hasil = kosong in km
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:"), "Harus menolak null yang dikonversi ke satuan")
        assertTrue(output.contains("requires a physics quantity") || output.contains("null"))
    }
}
