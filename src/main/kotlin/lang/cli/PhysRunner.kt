package lang.cli

import lang.ast.visitors.PhysicsEvaluator
import lang.lexer.Lexer
import lang.parser.Parser
import java.io.File

class PhysRunner() {
    private val evaluator = PhysicsEvaluator()

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
            val parser = Parser(tokens)
            val statements = parser.parseProgram()

            for (stmt in statements) {
                stmt.accept(evaluator)
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

}