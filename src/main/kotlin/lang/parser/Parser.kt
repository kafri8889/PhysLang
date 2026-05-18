package lang.parser

import lang.ast.*
import lang.error.Diagnostic
import lang.error.DiagnosticType
import lang.error.ErrorReporter
import lang.lexer.Token
import lang.lexer.TokenType

class Parser(
    private val tokens: List<Token>,
    private val reporter: ErrorReporter
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
                            value = if (token.value.contains('.')) LiteralValue.DoubleVal(token)
                            else LiteralValue.IntVal(token)
                        ),
                        unit = UnitNode.BaseUnit(unit)
                    )
                } else {
                    // Throw exception?
                    LiteralExpr(
                        value = if (token.value.contains('.')) LiteralValue.DoubleVal(token)
                        else LiteralValue.IntVal(token)
                    )
                }
            }
            TokenType.Minus -> {
                val right = parseExpression(Precedence.fromToken(TokenType.Unary)) ?: return null

                UnaryExpr(token, right)
            }
            TokenType.OpenParenthesis -> {
                val innerExpr = parseExpression(0)

                advance()

                innerExpr
            }
            TokenType.Identifier -> {
                VariableExpr(token)
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
                    val right = parseExpression(Precedence.fromToken(token.tokenType)) ?: return null

                    AssignExpr(left.nameToken, right)
                } else {
                    reporter.report(
                        Diagnostic(
                            message = "Unexpected token type in assign: ${token.tokenType}",
                            type = DiagnosticType.ERROR,
                            line = token.line,
                            column = token.startColumn until token.endColumn
                        )
                    )

                    null
                }
            }
            TokenType.PlusAssign,
            TokenType.MinusAssign,
            TokenType.MultiplyAssign,
            TokenType.DivideAssign -> {
                if (left is VariableExpr) {
                    val right = parseExpression(Precedence.fromToken(token.tokenType)) ?: return null

                    val binaryExpr = BinaryExpr(
                        left = left,
                        operatorToken = when (token.tokenType) {
                            TokenType.PlusAssign,
                            TokenType.MinusAssign,
                            TokenType.MultiplyAssign,
                            TokenType.DivideAssign -> token
                            else -> {
                                reporter.report(
                                    Diagnostic(
                                        message = "Unexpected token type in binary exp opr: ${token.tokenType}",
                                        type = DiagnosticType.ERROR,
                                        line = token.line,
                                        column = token.startColumn until token.endColumn
                                    )
                                )

                                return null
                            }
                        },
                        right = right
                    )

                    AssignExpr(left.nameToken, binaryExpr)
                } else {
                    reporter.report(
                        Diagnostic(
                            message = "Unexpected token type in divide assign: ${token.tokenType}",
                            type = DiagnosticType.ERROR,
                            line = token.line,
                            column = token.startColumn until token.endColumn
                        )
                    )

                    null
                }
            }

            TokenType.OpenParenthesis -> {
                val arguments = mutableListOf<Expr>()

                // check if there are arguments
                if (peek()?.tokenType != TokenType.CloseParenthesis) {
                    do {
                        // Parse argument
                        val arg: Expr? = parseExpression(0)

                        if (arg == null) {
                            reporter.report(
                                Diagnostic(
                                    message = "Expected expression in function arguments",
                                    type = DiagnosticType.ERROR,
                                    line = token.line,
                                    column = token.startColumn until token.endColumn
                                )
                            )

                            return null
                        }

                        arguments.add(arg)

                        if (peek()?.tokenType == TokenType.Comma) {
                            advance()
                        } else break
                    } while (peek() != null && peek()?.tokenType != TokenType.CloseParenthesis)
                }

                val closeToken = advance()
                if (closeToken?.tokenType != TokenType.CloseParenthesis) {
                    reporter.report(
                        Diagnostic(
                            message = "Expected ')' after function arguments, but got ${closeToken?.tokenType}",
                            type = DiagnosticType.ERROR,
                            line = token.line,
                            column = token.startColumn until token.endColumn
                        )
                    )

                    return null
                }

                CallExpr(callee = left, parenToken = token, arguments = arguments)
            }

            TokenType.Plus,
            TokenType.Minus,
            TokenType.Multiply,
            TokenType.Divide,
            TokenType.Power -> {
                val right = parseExpression(Precedence.fromToken(token.tokenType)) ?: return null

                BinaryExpr(left, token, right)
            }

            else -> left
        }
    }

    private fun synchronize() {
        while (peek() != null &&
            peek()?.tokenType != TokenType.Eol &&
            peek()?.tokenType != TokenType.Eof
        ) {
            advance()
        }

        if (peek()?.tokenType == TokenType.Eol) {
            advance()
        }
    }

    fun parseProgram(): List<Stmt> {
        val statements = mutableListOf<Stmt>()

        while (peek() != null && peek()?.tokenType != TokenType.Eof) {
            if (peek()?.tokenType == TokenType.Eol || peek()?.tokenType == TokenType.Comment) {
                advance()
                continue
            }

            val stmt = parseStatement()

            if (stmt != null) {
                statements.add(stmt)
            } else {
                synchronize()
            }
        }

        return statements
    }

    fun parseStatement(): Stmt? {

        val token = peek()

        return when (token?.tokenType) {
            TokenType.Var -> parseVarDecl()
            TokenType.Print -> parsePrintStmt()
            TokenType.Unit -> parseUnitDecl()
            else -> parseExprStmt()
        }
    }

    fun parseVarDecl(): Stmt? {

        val varName = advance() // consume 'var'

        val name = advance()
        if (name?.tokenType != TokenType.Identifier) {
            reporter.report(
                Diagnostic(
                    message = "Expected variable name after 'var'",
                    type = DiagnosticType.ERROR,
                    line = varName?.line ?: 0,
                    column = (varName?.startColumn ?: 0) until (varName?.endColumn ?: 0)
                )
            )
            return null
        }

        val assign = advance()
        if (assign?.tokenType != TokenType.Assign) {
            reporter.report(
                Diagnostic(
                    message = "Expected '=' after var name",
                    type = DiagnosticType.ERROR,
                    line = name.line,
                    column = name.startColumn until name.endColumn
                )
            )
            return null
        }

        val initializer = parseExpression(0)
        if (initializer == null) {
            reporter.report(
                Diagnostic(
                    message = "Expected expression after '=' in var declaration",
                    type = DiagnosticType.ERROR,
                    line = assign.line,
                    column = assign.startColumn until assign.endColumn
                )
            )
            return null
        }

        return VarDeclStmt(name, initializer)
    }

    fun parseUnitDecl(): Stmt? {
        val unitToken = advance() // consume 'unit'

        val name = advance()
        if (name?.tokenType != TokenType.Identifier) {
            reporter.report(
                Diagnostic(
                    message = "Expected unit name after 'unit'",
                    type = DiagnosticType.ERROR,
                    line = unitToken?.line ?: 0,
                    column = (unitToken?.startColumn ?: 0) until (unitToken?.endColumn ?: 0)
                )
            )
            return null
        }

        val assign = advance()
        if (assign?.tokenType != TokenType.Assign) {
            reporter.report(
                Diagnostic(
                    message = "Expected '=' after unit name",
                    type = DiagnosticType.ERROR,
                    line = name.line,
                    column = name.startColumn until name.endColumn
                )
            )
            return null
        }

        val initializer = parseExpression(0)
        if (initializer == null) {
            reporter.report(
                Diagnostic(
                    message = "Expected expression after '=' in unit declaration",
                    type = DiagnosticType.ERROR,
                    line = assign.line,
                    column = assign.startColumn until assign.endColumn
                )
            )
            return null
        }

        return UnitDeclStmt(name, initializer)
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
        var left = nud(token)
//        println("left: $left")

        if (left == null) {
            reporter.report(
                Diagnostic(
                    message = "Unexpected token '${token.value}'",
                    type = DiagnosticType.ERROR,
                    line = token.line,
                    column = token.startColumn until token.endColumn
                )
            )

            return null
        }

        while (rbp < Precedence.fromToken(peek()?.tokenType)) {

            val operator = advance() ?: break

            if (left == null) {
                reporter.report(
                    Diagnostic(
                        message = "Unexpected token '${token.value}'",
                        type = DiagnosticType.ERROR,
                        line = token.line,
                        column = token.startColumn until token.endColumn
                    )
                )

                return null
            }

            left = led(operator, left) ?: return null
        }

        return left
    }
}
