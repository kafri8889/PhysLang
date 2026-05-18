import lang.ast.PhysicsValue
import lang.cli.PhysRunner
import lang.core.Environment
import lang.error.ErrorReporter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhysRunnerTest {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    @BeforeEach
    fun setUp() {
        // Mengalihkan output print() agar masuk ke memori untuk dites
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @AfterEach
    fun tearDown() {
        // Mengembalikan standar output ke konsol asli setelah tes selesai
        System.setOut(standardOut)
    }

    private fun runAndGetOutput(
        script: String,
        configureEnvironment: (Environment) -> Unit = {}
    ): String {
        outputStreamCaptor.reset()
        val environment = Environment()
        configureEnvironment(environment)
        val runner = PhysRunner(environment, ErrorReporter())
        runner.runScript(script)
        // Normalisasi format baris baru agar aman dijalankan di Windows maupun Mac/Linux
        return outputStreamCaptor.toString().trim().replace("\r\n", "\n")
    }

    @Test
    fun `test basic variable declaration and print`() {
        val script = """
            print 5 kg
        """.trimIndent()

        assertEquals("5.0 kg", runAndGetOutput(script))
    }

    @Test
    fun `test math operations and automatic SI normalization`() {
        val script = """
            print 10 m / 2 s
        """.trimIndent()

        assertEquals("5.0 m*s^-1", runAndGetOutput(script))
    }

    @Test
    fun `test custom unit definition and fallback lookup`() {
        val script = """
            print 50 velocity + 10 velocity
        """.trimIndent()

        val velocity = PhysicsValue(
            value = 1.0,
            dimensions = intArrayOf(0, 1, -1, 0, 0, 0, 0)
        )

        assertEquals("60.0 velocity", runAndGetOutput(script) { it.putUnit("velocity", velocity) })
    }

    @Test
    fun `test string concatenation with physics values`() {
        val script = """
            print "Total force is: " + 100 kg * m / s / s
        """.trimIndent()

        assertEquals("Total force is: 100.0 N", runAndGetOutput(script))
    }

    @Test
    fun `test complex script with comments and unary operators`() {
        val script = """
            // Define custom gravity as a standalone formula
            print "Weight: " + 10 kg * 9.8 m / s / s
            print "Opposite force: " + -(10 kg * 9.8 m / s / s)
        """.trimIndent()

        val expected = "Weight: 98.0 N\nOpposite force: -98.0 N"
        assertEquals(expected, runAndGetOutput(script))
    }

    /**
     * Skenario paling rawan di mana user (seperti studi kasusmu sebelumnya) lupa menulis var. Tes ini memastikan sistem keamanan strict declaration milikmu berfungsi.
     */
    @Test
    fun `test error when assigning to undeclared variable (forgetting var)`() {
        val script = """
            // Langsung assign tanpa var
            massa = 50 kg
        """.trimIndent()

        val output = runAndGetOutput(script)
        // Memastikan mesin memprotes karena variabel belum dideklarasikan
        assertTrue(output.startsWith("ERROR:"), "The runner must print a semantic error when 'var' is missing")
        assertTrue(output.contains("Variable 'massa' is not defined!"))
    }

    @Test
    fun `test error when using undeclared variable in expression`() {
        val script = """
            // waktu belum pernah dideklarasikan
            print 10 m / waktu
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.startsWith("ERROR:"), "The runner must print a semantic error for undeclared variables")
        assertTrue(output.contains("Variable or unit 'waktu' is not defined!"))
    }

    /**
     * Menguji kepintaran Environment. Mesin harus bisa membedakan mana yang deklarasi baru (wajib var) dan mana yang menimpa nilai lama (hanya pakai =).
     */
    @Test
    fun `test valid variable reassignment`() {
        val script = """
            // Re-assign tanpa var (ini valid karena lacinya sudah dibuat)
            speed = 30 m / s
            print speed
        """.trimIndent()

        val speed = PhysicsValue(
            value = 10.0,
            dimensions = intArrayOf(0, 1, -1, 0, 0, 0, 0)
        )

        assertEquals("30.0 m*s^-1", runAndGetOutput(script) { it.putVar("speed", speed) })
    }

    @Test
    fun `test error on complex expression with missing var`() {
        val script = """
            // Lupa nulis var untuk weight
            weight = 5 kg * 10 m / s^2
            print weight
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.startsWith("ERROR:"), "The semantic analyzer must reject creating a new variable without 'var'")
        assertTrue(output.contains("Variable 'weight' is not defined!"))
    }

    @Test
    fun `test custom unit retains its custom name when printed`() {
        val script = """
            print 100 kph
        """.trimIndent()

        val kph = PhysicsValue(
            value = 1000.0 / 3600.0,
            dimensions = intArrayOf(0, 1, -1, 0, 0, 0, 0)
        )

        // Memastikan output mempertahankan angka mentah dan label 'kph'
        assertEquals("100.0 kph", runAndGetOutput(script) { it.putUnit("kph", kph) }.trim())
    }

    @Test
    fun `test SI function converts simple custom unit to base SI`() {
        val script = """
            print SI(2 sak)
        """.trimIndent()

        val sak = PhysicsValue(
            value = 50.0,
            dimensions = intArrayOf(1, 0, 0, 0, 0, 0, 0)
        )

        // 2 sak * 50 kg = 100.0 kg
        assertEquals("100.0 kg", runAndGetOutput(script) { it.putUnit("sak", sak) }.trim())
    }

    @Test
    fun `test SI function converts complex derived unit to base SI`() {
        val script = """
            print SI(36 kph)
        """.trimIndent()

        val kph = PhysicsValue(
            value = 1000.0 / 3600.0,
            dimensions = intArrayOf(0, 1, -1, 0, 0, 0, 0)
        )

        // 36 * (1000/3600) = 10.0 m/s
        assertEquals("10.0 m*s^-1", runAndGetOutput(script) { it.putUnit("kph", kph) }.trim()) // Sesuaikan format string 'm/s' dengan toString() bawaanmu
    }

    @Test
    fun `test error when SI function receives non-physics value`() {
        val script = """
            print SI("Kecepatan")
        """.trimIndent()

        val output = runAndGetOutput(script)
        // Memastikan fungsi SI() ditolak oleh semantic analyzer sebelum evaluator berjalan
        assertTrue(output.startsWith("ERROR:"), "The runner must print a semantic error when SI() receives a string")
    }

    @Test
    fun `test error when SI function receives invalid number of arguments`() {
        val script = """
            // Memasukkan 2 argumen padahal SI() hanya butuh 1
            print SI(10 m/s, 10 m/s)
        """.trimIndent()

        val output = runAndGetOutput(script)
        // Memastikan validasi 'arity' (jumlah argumen) berfungsi dengan baik
        assertTrue(output.startsWith("ERROR:"), "The semantic analyzer must reject invalid function arity")
        assertTrue(output.contains("expects 1 arguments"))
    }

}
