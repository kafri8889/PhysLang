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

class NullSafetyTest {

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
    // 1. DEKLARASI & PRINT NILAI NULL
    // ==========================================

    @Test
    @DisplayName("Deklarasi eksplisit var a = null dan dicetak menghasilkan output 'null'")
    fun testExplicitNullDeclarationAndPrint() {
        val script = """
            var a = null
            print a
        """.trimIndent()

        assertEquals("null", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Deklarasi tanpa inisialisasi var a default bernilai null saat dicetak")
    fun testUninitializedVariableDefaultPrint() {
        val script = """
            var a
            print a
        """.trimIndent()

        assertEquals("null", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Variabel yang sudah memiliki nilai dapat di-assign ulang menjadi null")
    fun testReassignValueToNull() {
        val script = """
            var item = 15 kg
            item = null
            print item
        """.trimIndent()

        assertEquals("null", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Variabel yang awalnya null dapat di-assign menjadi nilai fisika valid")
    fun testReassignNullToPhysicsValue() {
        val script = """
            var speed = null
            speed = 25 m / s
            print speed
        """.trimIndent()

        assertEquals("25.0 m*s^-1", runAndGetOutput(script))
    }

    // ==========================================
    // 2. UNARY OPERATOR PADA NULL (HARUS DITOLAK)
    // ==========================================

    @Test
    @DisplayName("Menolak operasi unary minus pada variabel null (-a)")
    fun testRejectUnaryMinusOnNullVariable() {
        val script = """
            var a = null
            print -a
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Harus menghasilkan compile/semantic error saat -a dievaluasi")
    }

    @Test
    @DisplayName("Menolak operasi unary minus langsung pada literal -null")
    fun testRejectUnaryMinusOnNullLiteral() {
        val script = """
            print -null
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Harus menghasilkan compile/semantic error saat -null dievaluasi")
    }

    // ==========================================
    // 3. OPERASI PENJUMLAHAN (+) PADA NULL (HARUS DITOLAK)
    // ==========================================

    @Test
    @DisplayName("Menolak penjumlahan variabel null dengan besaran fisika (a + 10 kg)")
    fun testRejectNullPlusPhysicsValue() {
        val script = """
            var a = null
            var result = a + 10 kg
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Operasi null + 10 kg harus ditolak oleh semantic analyzer")
    }

    @Test
    @DisplayName("Menolak penjumlahan besaran fisika dengan variabel null (10 kg + a)")
    fun testRejectPhysicsValuePlusNull() {
        val script = """
            var a = null
            var result = 10 kg + a
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Operasi 10 kg + null harus ditolak oleh semantic analyzer")
    }

    @Test
    @DisplayName("Menolak penjumlahan sesama variabel null (a + b)")
    fun testRejectNullPlusNull() {
        val script = """
            var a = null
            var b = null
            var c = a + b
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Operasi null + null harus ditolak oleh semantic analyzer")
    }

    // ==========================================
    // 4. OPERASI PENGURANGAN (-), PERKALIAN (*), PEMBAGIAN (/), PANGKAT (^)
    // ==========================================

    @Test
    @DisplayName("Menolak pengurangan variabel null (a - 5 m)")
    fun testRejectNullSubtraction() {
        val script = """
            var a = null
            var diff = a - 5 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Operasi null - 5 m harus ditolak")
    }

    @Test
    @DisplayName("Menolak perkalian variabel null dengan skalar atau besaran (a * 2)")
    fun testRejectNullMultiplication() {
        val script = """
            var a = null
            var product = a * 2
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Operasi null * 2 harus ditolak")
    }

    @Test
    @DisplayName("Menolak pembagian variabel null (100 J / a)")
    fun testRejectNullDivision() {
        val script = """
            var a = null
            var ratio = 100 J / a
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Operasi 100 J / null harus ditolak")
    }

    @Test
    @DisplayName("Menolak operasi pangkat pada variabel null (a ^ 2)")
    fun testRejectNullPower() {
        val script = """
            var a = null
            var squared = a ^ 2
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Operasi null ^ 2 harus ditolak")
    }

    // ==========================================
    // 5. COMPOUND ASSIGNMENT PADA VARIABEL NULL
    // ==========================================

    @Test
    @DisplayName("Menolak compound assignment += pada variabel yang bernilai null (a += 10 m)")
    fun testRejectCompoundAssignOnNullVariable() {
        val script = """
            var a = null
            a += 10 m
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR"), "Operasi a += 10 m pada a null harus ditolak")
    }
}
