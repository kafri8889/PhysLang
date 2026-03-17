package lang.parser

import lang.ast.*
import lang.lexer.Token
import lang.lexer.TokenType

class Parser(
    private val tokens: List<Token>,
) {

    private var currentTokenIndex = 0

    fun advance(): Token? {
        val tok = peek()
        currentTokenIndex++
        return tok
    }

    fun peek(): Token? {
        return tokens.getOrNull(currentTokenIndex)
    }

    fun nud(token: Token): Expr? {
        return when (token.tokenType) {
            TokenType.NumberLiteral -> {
                if (peek()?.tokenType == TokenType.Identifier) {
                    val unit = advance()!!.value

                    QuantityExpr(
                        value = LiteralExpr(LiteralValue.IntVal(token.value.toInt())),
                        unit = UnitNode.BaseUnit(unit)
                    )
                } else {
                    // Throw exception?
                    LiteralExpr(LiteralValue.IntVal(token.value.toInt()))
                }
            }
            TokenType.Minus -> {
                val right = parseExpression(Precedence.fromToken(TokenType.Unary))
                if (right == null) return null

                UnaryExpr(token.tokenType, right)
            }
            TokenType.OpenParenthesis -> {
                val innerExpr = parseExpression(0)

                advance()

                innerExpr
            }
            TokenType.Identifier -> {
                VariableExpr(token.value)
            }
            else -> null
        }
    }

    fun led(token: Token, left: Expr): Expr? {
        return when (token.tokenType) {
            TokenType.Assign -> {
                if (left is VariableExpr) {
                    val right = parseExpression(Precedence.fromToken(token.tokenType))
                    if (right == null) return null

                    AssignExpr(left.name, right)
                } else throw IllegalStateException("Unexpected token type ${token.tokenType}")
            }

            TokenType.Plus,
            TokenType.Minus,
            TokenType.Multiply,
            TokenType.Divide,
            TokenType.Power -> {
                val right = parseExpression(Precedence.fromToken(token.tokenType))
                if (right == null) return null

                BinaryExpr(left, token.tokenType, right)
            }

            else -> left
        }
    }

    fun parseProgram(): List<Stmt> {

        val statements = mutableListOf<Stmt>()

        while (peek() != null) {
            statements.add(parseStatement())
        }

        return statements
    }

    fun parseStatement(): Stmt {

        val token = peek()

        return when (token?.tokenType) {

            TokenType.Var -> parseVarDecl()

            else -> parseExprStmt()
        }
    }

    fun parseVarDecl(): Stmt {

        advance() // consume 'var'

        val name = advance() // identifier

        if (name?.tokenType != TokenType.Identifier) error("Expected variable name")

        advance() // consume '='

        val initializer = parseExpression(0)

        return VarDeclStmt(name.value, initializer)
    }

    /**
     * Independent expression
     */
    fun parseExprStmt(): Stmt {

        val expr = parseExpression(0)

        return ExprStmt(expr)
    }

    fun parseExpression(rbp: Int = 0): Expr? {

        val token = advance() ?: return null
        var left = nud(token) ?: return null
//        println("left: $left")

        while (rbp < Precedence.fromToken(peek()?.tokenType)) {

            val operator = advance() ?: break
            left = led(operator, left) ?: return null
        }

        return left
    }
}