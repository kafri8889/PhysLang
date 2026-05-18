package lang.cli

import lang.ast.visitors.PhysicsEvaluator
import lang.core.Environment
import lang.error.ErrorReporter
import lang.lexer.Lexer
import lang.parser.Parser
import lang.semantic.SemanticAnalyzer
import java.io.File

class PhysRunner(
    private val environment: Environment,
    private val reporter: ErrorReporter
) {
    fun runFile(file: File) {
        val source = file.readText()
        execute(source)
    }

    fun runScript(script: String) {
        execute(script)
    }

    private fun execute(source: String) {
        try {
            val tokens = Lexer(source).lex()
            val parser = Parser(tokens, reporter)
            val statements = parser.parseProgram()

            if (reporter.hasErrors()) {
                reporter.printAll(source)
                return
            }

            val analyzer = SemanticAnalyzer(
                environment = environment,
                reporter = reporter
            )

            statements.forEach { stmt ->
                stmt.accept(analyzer)
            }

            if (reporter.hasErrors()) {
                reporter.printAll(source)
                return
            }

            val evaluator = PhysicsEvaluator(environment)

            statements.forEach { stmt ->
                stmt.accept(evaluator)
            }
        } catch (e: Exception) {
            println("RUNTIME ERROR: ${e.message}")
        }
    }

}