import lang.cli.PhysRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals

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

    private fun runAndGetOutput(script: String): String {
        outputStreamCaptor.reset()
        val runner = PhysRunner()
        runner.runScript(script)
        // Normalisasi format baris baru agar aman dijalankan di Windows maupun Mac/Linux
        return outputStreamCaptor.toString().trim().replace("\r\n", "\n")
    }

    @Test
    fun `test basic variable declaration and print`() {
        val script = """
            var mass = 5 kg
            print mass
        """.trimIndent()

        assertEquals("5.0 kg", runAndGetOutput(script))
    }

    @Test
    fun `test math operations and automatic SI normalization`() {
        val script = """
            var distance = 10 m
            var time = 2 s
            var speed = distance / time
            print speed
        """.trimIndent()

        assertEquals("5.0 m·s^-1", runAndGetOutput(script))
    }

    @Test
    fun `test custom unit definition and fallback lookup`() {
        val script = """
            unit velocity = m / s
            var car = 50 velocity
            var boost = 10 velocity
            print car + boost
        """.trimIndent()

        assertEquals("60.0 velocity", runAndGetOutput(script))
    }

    @Test
    fun `test string concatenation with physics values`() {
        val script = """
            var force = 100 kg * m / s / s
            print "Total force is: " + force
        """.trimIndent()

        assertEquals("Total force is: 100.0 N", runAndGetOutput(script))
    }

    @Test
    fun `test complex script with comments and unary operators`() {
        val script = """
            // Define custom gravity as a standalone formula
            unit gravity = 9.8 m / s / s
            var mass = 10 kg
            
            // Calculate total weight (mass * acceleration)
            var weight = mass * gravity
            
            print "Weight: " + weight
            print "Opposite force: " + -weight
        """.trimIndent()

        val expected = "Weight: 98.0 N\nOpposite force: -98.0 N"
        assertEquals(expected, runAndGetOutput(script))
    }
}