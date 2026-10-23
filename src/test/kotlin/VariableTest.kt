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
}
