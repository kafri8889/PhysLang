
import lang.ast.PhysicsValue
import lang.ast.visitors.PhysicsEvaluator
import lang.lexer.Lexer
import lang.parser.Parser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PhysicsEvaluatorTest {

    private fun executeCode(sourceCode: String): PhysicsValue {
        val tokens = Lexer(sourceCode).lex()
        val parser = Parser(tokens)
        val ast = parser.parseExpression() ?: throw Exception("Parser return null!")

        val evaluator = PhysicsEvaluator()
        return ast.accept(evaluator) as PhysicsValue
    }

    @Test
    @DisplayName("Harus mengeksekusi prioritas perkalian sebelum penjumlahan")
    fun testOperatorPrecedence() {
        val result = executeCode("-10 kg + 5 kg * 2")

        // Ekspektasi: 5 * 2 = 10, lalu -10 + 10 = 0.0
        assertEquals(0.0, result.scaledValue, 0.0001, "Value matematika salah")
        assertEquals("kg", result.dimensionsToString())
    }

    @Test
    @DisplayName("Harus menghormati tanda kurung dan Unary Minus")
    fun testParenthesesAndUnary() {
        val result = executeCode("( -3 kg + 7 kg ) / 2")

        // Ekspektasi: (-3 + 7) / 2 = 4 / 2 = 2.0
        assertEquals(2.0, result.scaledValue, 0.0001)
    }

    @Test
    @DisplayName("Harus menangani pembagian satuan bersarang (Analisis Newton)")
    fun testComplexDimensionalAnalysis() {
        // Simulasi Massa * Jarak / Waktu / Waktu = Gaya (Newton)
        val result = executeCode("20 kg * 10 m / 2 s / 2 s")

        // Ekspektasi Magnitude: 20 * 10 = 200. 200 / 2 = 100. 100 / 2 = 50.0
        assertEquals(50.0, result.scaledValue, 0.0001)

        // Ekspektasi Vektor Dimensi: [1, 1, -2, 0, 0, 0, 0] (kg * m / s^2)
        val expectedVector = intArrayOf(1, 1, -2, 0, 0, 0, 0)
        assertArrayEquals(expectedVector, result.dimensions, "Vektor satuan fisika tidak cocok (Bukan Newton)")
    }

    // ==========================================
    // TIER 3: NEGATIVE TESTING (PENGUJIAN KEGAGALAN)
    // ==========================================
    // Ingat: Sistem yang baik harus tahu kapan ia harus menolak komputasi ilegal!

    @Test
    @DisplayName("Harus CRASH (melempar Exception) jika dimensi ditambah secara ilegal")
    fun testSemanticErrorOnInvalidAddition() {
        // Kita memerintahkan JUnit untuk mengekspektasikan terjadinya Exception
        val exception = assertThrows<Exception> {
            executeCode("5 kg + 10 m")
        }

        // Validasi pesan error dari kompilator Anda
        assertTrue(exception.message!!.contains("Dimensi"), "Pesan error tidak menjelaskan masalah dimensi")
    }

    @Test
    @DisplayName("Harus CRASH pada operasi gabungan yang menghasilkan dimensi ilegal")
    fun testSemanticErrorOnComplexMismatch() {
        assertThrows<Exception>("Kompilator gagal mendeteksi waktu ditambah jarak") {
            // 10 * 2m = 20m. 20m + 5s = ILEGAL!
            executeCode("10 * 2 m + 5 s")
        }
    }

    // ==========================================
    // TIER 4: STRESS TESTING & EDGE CASES
    // ==========================================

    @Test
    @DisplayName("Harus menangani komputasi campuran antara Skalar Murni dan Besaran Fisika")
    fun testScalarMixedWithQuantity() {
        // Skalar (tanpa dimensi) berinteraksi dengan massa.
        // Formula: (10 / 2) * 5 kg - 2 kg * (3 + 1)
        // Evaluasi: 5 * 5 kg - 2 kg * 4 = 25 kg - 8 kg = 17 kg
        val result = executeCode("(10 / 2) * 5 kg - 2 kg * (3 + 1)")

        assertEquals(17.0, result.scaledValue, 0.0001)

        // Memastikan hasil akhirnya tetap berdimensi massa [1, 0, 0, 0, 0, 0, 0]
        val expectedVector = intArrayOf(1, 0, 0, 0, 0, 0, 0)
        assertArrayEquals(expectedVector, result.dimensions, "Interaksi skalar merusak dimensi!")
    }

    @Test
    @DisplayName("Harus menangani penjumlahan satuan turunan yang kompleks (Momentum)")
    fun testComplexDerivedUnitAddition() {
        // Menjumlahkan dua entitas yang dihitung secara terpisah,
        // tapi hasil dimensinya ternyata sama (kg * m / s)
        val result = executeCode("(10 kg * 2 m / 1 s) + (5 kg * 4 m / 1 s)")

        // Kiri: 20. Kanan: 20. Total = 40.0
        assertEquals(40.0, result.scaledValue, 0.0001)

        // Dimensi Momentum: Massa(1), Panjang(1), Waktu(-1)
        val expectedVector = intArrayOf(1, 1, -1, 0, 0, 0, 0)
        assertArrayEquals(expectedVector, result.dimensions)
    }

    @Test
    @DisplayName("Harus melakukan pembatalan dimensi (Dimensional Cancellation) dan Inversi")
    fun testDimensionalCancellationAndInversion() {
        // Fisika murni: Jarak dibagi Kecepatan = Waktu
        // Formula: 100 m / (10 m / 2 s) -> 100m / 5m/s = 20 s
        val result = executeCode("100 m / (10 m / 2 s)")

        assertEquals(20.0, result.scaledValue, 0.0001)

        // m dibagi (m/s) -> meter saling menghilangkan, s naik ke atas menjadi s^1
        // Ekspektasi akhir: murni Waktu [0, 0, 1, 0, 0, 0, 0]
        val expectedVector = intArrayOf(0, 0, 1, 0, 0, 0, 0)
        assertArrayEquals(expectedVector, result.dimensions, "Gagal membatalkan dimensi pembagian!")
    }

    @Test
    @DisplayName("Harus melempar Exception dari dalam Node yang paling dalam (Error Bubbling)")
    fun testDeepNestedSemanticError() {
        // Error tidak terjadi di luar, tapi jauh di dalam tanda kurung.
        // Kompilator harus mendeteksinya sebelum mencoba mengalikannya dengan 10 kg.
        val exception = assertThrows<Exception>("Kompilator gagal mendeteksi error di dalam kurung") {
            executeCode("10 kg * (5 m + 2 s)")
        }

        assertTrue(exception.message!!.contains("mismatch", ignoreCase = true) ||
                exception.message!!.contains("Dimensi", ignoreCase = true))
    }
}