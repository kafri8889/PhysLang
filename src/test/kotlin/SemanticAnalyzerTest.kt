import lang.core.Environment
import lang.error.ErrorReporter
import lang.lexer.Lexer
import lang.parser.Parser
import lang.semantic.SemanticAnalyzer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemanticAnalyzerTest {

    private fun analyzeExpression(sourceCode: String): ErrorReporter {
        val reporter = ErrorReporter()
        val parser = Parser(Lexer(sourceCode).lex(), reporter)
        val expression = parser.parseExpression() ?: return reporter

        if (!reporter.hasErrors()) {
            expression.accept(SemanticAnalyzer(Environment(), reporter))
        }

        return reporter
    }

    private fun analyzeProgram(sourceCode: String): ErrorReporter {
        val reporter = ErrorReporter()
        val parser = Parser(Lexer(sourceCode).lex(), reporter)
        val statements = parser.parseProgram()

        if (!reporter.hasErrors()) {
            val analyzer = SemanticAnalyzer(Environment(), reporter)
            statements.forEach { it.accept(analyzer) }
        }

        return reporter
    }

    @Test
    fun `reports dimension mismatch for incompatible addition`() {
        val reporter = analyzeExpression("5 kg + 10 m")

        assertTrue(reporter.hasErrors())
        assertTrue(reporter.errors.any { it.message.contains("Dimension mismatch", ignoreCase = true) })
    }

    @Test
    fun `reports nested dimension mismatch before evaluation`() {
        val reporter = analyzeExpression("10 kg * (5 m + 2 s)")

        assertTrue(reporter.hasErrors())
        assertTrue(reporter.errors.any { it.message.contains("Dimension mismatch", ignoreCase = true) })
    }

    @Test
    fun `reports undeclared variable usage`() {
        val reporter = analyzeExpression("10 m / waktu")

        assertTrue(reporter.hasErrors())
        assertTrue(reporter.errors.any { it.message.contains("Variable or unit 'waktu' is not defined!") })
    }

    @Test
    fun `reports assignment to undeclared variable`() {
        val reporter = analyzeExpression("mass = 10 kg")

        assertTrue(reporter.hasErrors())
        assertTrue(reporter.errors.any { it.message.contains("Variable 'mass' is not defined!") })
    }

    @Test
    fun `reports invalid native function arity`() {
        val reporter = analyzeExpression("SI(10 m, 20 m)")

        assertTrue(reporter.hasErrors())
        assertTrue(reporter.errors.any { it.message.contains("expects 1 arguments") })
    }

    @Test
    fun `reports invalid SI argument type`() {
        val reporter = analyzeExpression("SI(\"Kecepatan\")")

        assertTrue(reporter.hasErrors())
        assertTrue(reporter.errors.any { it.message.contains("only accepts physical quantities") })
    }

    @Test
    fun `accepts standalone unit declaration with built in units`() {
        val reporter = analyzeProgram("unit velocity = m / s")

        assertFalse(reporter.hasErrors())
    }
}
