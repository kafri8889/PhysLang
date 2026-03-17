package lang

import lang.ast.visitors.PhysicsEvaluator
import lang.lexer.Lexer
import lang.parser.Parser

class PhysCli {

    fun run() {
        val evaluator = PhysicsEvaluator()

        println()
        println("PhysLang CLI")

        while (true) {
            print(">>> ")
            val input = readln()
            if (input.equals("exit", true)) break

            val tokens = Lexer(input).lex()
//            println(tokens)

            val parser = Parser(tokens)
            val parser2 = Parser(tokens)
            val parser3 = Parser(tokens)
//            println(parser.parseExpression(0)?.accept(MathASTPrinter()).toString())
//            println(parser2.parseExpression(0)?.accept(evaluator).toString())

            for (stmt in parser.parseProgram()) {
                stmt.accept(evaluator)
            }
        }
    }

}