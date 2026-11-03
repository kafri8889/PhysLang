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

class BlockStatementTest {

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
    @DisplayName("Blok sederhana dapat mendeklarasikan dan mencetak variabel di dalamnya")
    fun testBasicBlockExecution() {
        val script = """
            {
                a = 2 km + 10 m
                print a
            }
        """.trimIndent()

        // PhysLang memformat dengan satuan yang cocok (2.01 km)
        assertEquals("2.01 km", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Blok bersarang (Nested Block) dapat membaca variabel dari scope luar (Parent Scope)")
    fun testNestedBlockReadOuterScope() {
        val script = """
            outer = 50 N
            {
                inner = 20 N
                print outer + inner
            }
        """.trimIndent()

        assertEquals("70.0 N", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Variabel di dalam blok tidak boleh bocor ke luar blok (Scope Isolation)")
    fun testBlockScopeIsolation() {
        val script = """
            {
                insideOnly = 100 J
            }
            print insideOnly
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:") && output.contains("insideOnly' is not defined"))
    }

    @Test
    @DisplayName("Blok bertingkat (Nested Block) dapat melakukan isolasi variabel dan perhitungan berlapis")
    fun testNestedBlockIsolation() {
        val script = """
            base = 10 m
            {
                step = 5 m
                {
                    extra = 2 m
                    print base + step + extra
                }
            }
        """.trimIndent()

        assertEquals("17.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Blok dapat memodifikasi variabel lokalnya sendiri tanpa mengganggu state global")
    fun testBlockLocalModification() {
        val script = """
            {
                count = 10 s
                count = count + 5 s
                print count
            }
        """.trimIndent()

        assertEquals("15.0 s", runAndGetOutput(script))
    }

    // ==========================================
    // TIER: ADVANCED BLOCK STATEMENT CASES
    // ==========================================

    @Test
    @DisplayName("Blok kosong {} tidak menimbulkan error dan dapat dieksekusi dengan aman")
    fun testEmptyBlockExecution() {
        val script = """
            print 1 m
            {}
            print 2 m
        """.trimIndent()

        val expected = """
            1.0 m
            2.0 m
        """.trimIndent()

        assertEquals(expected, runAndGetOutput(script))
    }

    @Test
    @DisplayName("Blok dapat memodifikasi variabel luar (Outer Reassignment)")
    fun testBlockModifiesOuterVariableValue() {
        val script = """
            total = 10 m
            {
                total = 25 m
            }
            print total
        """.trimIndent()

        assertEquals("25.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Blok membayangi (shadowing) variabel luar dengan tipe/satuan berbeda di dalam blok")
    fun testBlockShadowingWithDifferentDimension() {
        val script = """
            item = 10 m
            {
                item = 5 kg
                print item
            }
            print item
        """.trimIndent()

        val expected = """
            5.0 kg
            5.0 kg
        """.trimIndent()

        assertEquals(expected, runAndGetOutput(script))
    }

    @Test
    @DisplayName("Blok di dalam ekspresi atau blok bersarang bertingkat 4 (Deeply Nested)")
    fun testDeeplyNestedBlocks() {
        val script = """
            a = 1 m
            {
                b = 2 m
                {
                    c = 3 m
                    {
                        d = 4 m
                        print a + b + c + d
                    }
                }
            }
        """.trimIndent()

        assertEquals("10.0 m", runAndGetOutput(script))
    }

    @Test
    @DisplayName("Variabel di dalam blok anak tidak boleh diakses oleh blok induk atau saudara")
    fun testSiblingScopeIsolation() {
        val script = """
            {
                secret = 42 J
            }
            {
                print secret
            }
        """.trimIndent()

        val output = runAndGetOutput(script)
        assertTrue(output.contains("ERROR:") && output.contains("secret' is not defined"))
    }

    @Test
    @DisplayName("Dua blok sejajar independen dapat menggunakan nama variabel yang sama tanpa konflik")
    fun testSiblingBlocksIndependence() {
        val script = """
            {
                temp = 300 K
                print temp
            }
            {
                temp = 350 K
                print temp
            }
        """.trimIndent()

        val expected = """
            300.0 K
            350.0 K
        """.trimIndent()

        assertEquals(expected, runAndGetOutput(script))
    }
}
