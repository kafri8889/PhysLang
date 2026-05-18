import lang.cli.PhysRunner
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
        assertTrue(output.startsWith("Error:"), "Mesin harus mengeluarkan Error jika lupa memakai 'var'")
    }

    @Test
    fun `test error when using undeclared variable in expression`() {
        val script = """
            var distance = 10 m
            // waktu belum pernah dideklarasikan
            var speed = distance / waktu
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.startsWith("Error:"), "Mesin harus mengeluarkan Error jika memakai variabel gaib")
    }

    /**
     * Menguji kepintaran Environment. Mesin harus bisa membedakan mana yang deklarasi baru (wajib var) dan mana yang menimpa nilai lama (hanya pakai =).
     */
    @Test
    fun `test valid variable reassignment`() {
        val script = """
            var speed = 10 m / s
            // Re-assign tanpa var (ini valid karena lacinya sudah dibuat)
            speed = 30 m / s
            print speed
        """.trimIndent()

        assertEquals("30.0 m·s^-1", runAndGetOutput(script))
    }

    @Test
    fun `test error on complex expression with missing var`() {
        val script = """
            unit gravity = 10 m / s^2
            var mass = 5 kg
            // Lupa nulis var untuk weight
            weight = mass * gravity
            print weight
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.startsWith("Error:"), "Compiler harus menolak pembuatan variabel baru tanpa 'var'")
    }

    @Test
    fun `test custom unit retains its custom name when printed`() {
        val script = """
            unit kph = 1000 m / 3600 s
            var topSpeed = 100 kph
            print topSpeed
        """.trimIndent()

        // Memastikan output mempertahankan angka mentah dan label 'kph'
        assertEquals("100.0 kph", runAndGetOutput(script).trim())
    }

    @Test
    fun `test SI function converts simple custom unit to base SI`() {
        val script = """
            unit sak = 50 kg
            var semen = 2 sak
            print SI(semen)
        """.trimIndent()

        // 2 sak * 50 kg = 100.0 kg
        assertEquals("100.0 kg", runAndGetOutput(script).trim())
    }

    @Test
    fun `test SI function converts complex derived unit to base SI`() {
        val script = """
            unit kph = 1000 m / 3600 s
            var speed = 36 kph
            print SI(speed)
        """.trimIndent()

        // 36 * (1000/3600) = 10.0 m/s
        assertEquals("10.0 m·s^-1", runAndGetOutput(script).trim()) // Sesuaikan format string 'm/s' dengan toString() bawaanmu
    }

    @Test
    fun `test error when SI function receives non-physics value`() {
        val script = """
            var text = "Kecepatan"
            print SI(text)
        """.trimIndent()

        val output = runAndGetOutput(script)
        // Memastikan fungsi SI() menolak string dan melempar Exception
        assertTrue(output.startsWith("Error:"), "Mesin harus mengeluarkan Error jika argumen SI() berupa String")
    }

    @Test
    fun `test error when SI function receives invalid number of arguments`() {
        val script = """
            var v = 10 m/s
            // Memasukkan 2 argumen padahal SI() hanya butuh 1
            print SI(v, v)
        """.trimIndent()

        val output = runAndGetOutput(script)
        // Memastikan validasi 'arity' (jumlah argumen) berfungsi dengan baik
        assertTrue(output.startsWith("Error:"), "Mesin harus menolak jika jumlah argumen fungsi tidak sesuai arity")
    }

}