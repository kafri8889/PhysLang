package lang.cli

import lang.ast.visitors.PhysicsEvaluator
import lang.core.Environment
import lang.error.ErrorReporter
import lang.lexer.Lexer
import lang.parser.Parser
import lang.semantic.SemanticAnalyzer

class PhysCli(
    private val environment: Environment,
    private val reporter: ErrorReporter,
    private val args: Array<String> = emptyArray()
) {

    fun run() {
        println()
        println("PhysLang CLI")

        while (true) {
            reporter.clear()

            print(">>> ")
            val input = readlnOrNull() ?: break
            if (input.equals("exit", true)) break

            val tokens = Lexer(input).lex()
//            println(tokens)

            val parser = Parser(tokens, reporter)

//            val parser2 = Parser(tokens)
//            val parser3 = Parser(tokens)
//            println(parser.parseExpression(0)?.accept(MathASTPrinter()).toString())
//            println(parser2.parseExpression(0)?.accept(evaluator).toString())

            val statements = parser.parseProgram()

            if (reporter.hasErrors()) {
                reporter.printAll(input)
                continue
            }

            val analyzer = SemanticAnalyzer(
                environment = environment,
                reporter = reporter
            )

            statements.forEach { stmt ->
                stmt.accept(analyzer)
            }

            if (reporter.hasErrors()) {
                reporter.printAll(input)
                continue
            }

            val evaluator = PhysicsEvaluator(environment)

            try {
                statements.forEach { stmt ->
                    stmt.accept(evaluator)
                }
            } catch (e: Exception) {
                println("RUNTIME ERROR: ${e.message}")
            }
        }
    }

}