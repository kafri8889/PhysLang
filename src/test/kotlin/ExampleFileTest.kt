import lang.ast.visitors.PhysicsEvaluator
import lang.core.Environment
import lang.error.ErrorReporter
import lang.lexer.Lexer
import lang.parser.Parser
import lang.semantic.SemanticAnalyzer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class ExampleFileTest {

    private val assetsDirectory = File("assets")

    @Test
    fun `all valid example files execute through parser semantic analyzer and evaluator`() {
        val exampleFiles = assetsDirectory
            .listFiles { file -> file.isFile && file.extension == "phys" && !file.name.contains("error") }
            ?.sortedBy { it.name }
            ?: emptyList()

        assertTrue(exampleFiles.isNotEmpty(), "Expected at least one valid example file in assets.")

        for (file in exampleFiles) {
            val result = executeExampleFile(file)

            assertFalse(result.hasErrors, "Example ${file.name} should run without semantic errors:\n${result.output}")
            assertTrue(result.output.isNotBlank(), "Example ${file.name} should produce visible output.")
        }
    }

    @Test
    fun `intentional error example files stop at semantic analysis`() {
        val errorFiles = assetsDirectory
            .listFiles { file -> file.isFile && file.extension == "phys" && file.name.contains("error") }
            ?.sortedBy { it.name }
            ?: emptyList()

        assertTrue(errorFiles.isNotEmpty(), "Expected at least one intentional error example file in assets.")

        for (file in errorFiles) {
            val result = executeExampleFile(file)

            assertTrue(result.hasErrors, "Example ${file.name} should report semantic errors.")
            assertTrue(result.output.contains("ERROR:"), "Example ${file.name} should print diagnostic output.")
        }
    }

    private fun executeExampleFile(file: File): ExampleExecutionResult {
        val source = file.readText()
        val environment = Environment()
        val reporter = ErrorReporter()
        val parser = Parser(Lexer(source).lex(), reporter)
        val statements = parser.parseProgram()
        val output = captureStdout {
            if (reporter.hasErrors()) {
                reporter.printAll(source)
                return@captureStdout
            }

            val evaluator = PhysicsEvaluator(environment)

            for (statement in statements) {
                reporter.clear()
                statement.accept(SemanticAnalyzer(environment, reporter))

                if (reporter.hasErrors()) {
                    reporter.printAll(source)
                    return@captureStdout
                }

                statement.accept(evaluator)
            }
        }

        return ExampleExecutionResult(
            output = output.trim().replace("\r\n", "\n"),
            hasErrors = reporter.hasErrors()
        )
    }

    private fun captureStdout(block: () -> Unit): String {
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()

        try {
            System.setOut(PrintStream(outputStream))
            block()
        } finally {
            System.setOut(originalOut)
        }

        return outputStream.toString()
    }

    private data class ExampleExecutionResult(
        val output: String,
        val hasErrors: Boolean
    )
}
