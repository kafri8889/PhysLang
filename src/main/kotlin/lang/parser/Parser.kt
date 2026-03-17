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
                        value = LiteralExpr(
                            value = if (token.value.contains('.')) LiteralValue.DoubleVal(token.value.toDouble())
                            else LiteralValue.IntVal(token.value.toInt())
                        ),
                        unit = UnitNode.BaseUnit(unit)
                    )
                } else {
                    // Throw exception?
                    LiteralExpr(
                        value = if (token.value.contains('.')) LiteralValue.DoubleVal(token.value.toDouble())
                        else LiteralValue.IntVal(token.value.toInt())
                    )
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
            TokenType.StringLiteral -> {
                StringLiteralExpr(token.value)
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
            TokenType.PlusAssign,
            TokenType.MinusAssign,
            TokenType.MultiplyAssign,
            TokenType.DivideAssign -> {
                if (left is VariableExpr) {
                    val right = parseExpression(Precedence.fromToken(token.tokenType))
                    if (right == null) return null

                    val binaryExpr = BinaryExpr(
                        left = left,
                        operator = when (token.tokenType) {
                            TokenType.PlusAssign -> TokenType.Plus
                            TokenType.MinusAssign -> TokenType.Minus
                            TokenType.MultiplyAssign -> TokenType.Multiply
                            TokenType.DivideAssign -> TokenType.Divide
                            else -> throw IllegalStateException("Unexpected token type ${token.tokenType}")
                        },
                        right = right
                    )

                    AssignExpr(left.name, binaryExpr)
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
            TokenType.Print -> parsePrintStmt()
            TokenType.Unit -> parseUnitDecl()
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

    fun parseUnitDecl(): Stmt {

        advance() // consume 'unit'

        val name = advance() // identifier

        if (name?.tokenType != TokenType.Identifier) error("Expected unit name")

        advance() // consume '='

        val initializer = parseExpression(0)

        if (initializer == null) throw IllegalStateException("Expected expression: Can't create custom unit with empty expression")

        return UnitDeclStmt(name.value, initializer)
    }

    fun parsePrintStmt(): Stmt {
        advance() // consume "print"
        val expr = parseExpression(0)
        return PrintStmt(expr)
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