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

    private fun isSeparator(token: TokenType?): Boolean {
        return token == TokenType.Eol || token == TokenType.Eof || token == TokenType.Comment
    }

    /**
     * Skip eol and comment
     */
    private fun skipSeparators() {
        while (isSeparator(peek()?.tokenType)) {
            advance()
        }
    }

    /**
     * Get current token and advance to the next token
     */
    fun advance(): Token? {
        val tok = peek()
        currentTokenIndex++
        return tok
    }

    /**
     * Get current token without consume it
     */
    fun peek(): Token? {
        return tokens.getOrNull(currentTokenIndex)
    }

    /**
     * Intip next token without consume it
     */
    fun lookup(): Token? {
        return tokens.getOrNull(currentTokenIndex + 1)
    }

    /**
     * Consume currnet token (peek and advance if current token match [expected])
     */
    fun consume(
        expected: TokenType,
        message: (Token?) -> String
    ): Token? {
        val token = peek()

        if (token?.tokenType == expected) {
            return advance()
        }

        reporter.report(
            Diagnostic(
                type = DiagnosticType.ERROR,
                message = message(token),
                line = token?.line ?: 0,
                column = token?.let {
                    it.startColumn..it.endColumn
                } ?: (0..0)
            )
        )

        return token
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
                        unit = parseUnitPower(UnitNode.BaseUnit(unit))
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
                IdentifierExpr(token)
            }
            TokenType.StringLiteral -> {
                StringLiteralExpr(token.value)
            }
            TokenType.Null -> {
                LiteralExpr(LiteralValue.NullVal(token))
            }
            else -> null
        }
    }

    /**
     * @param token current token (after [left])
     * @param left left expression
     */
    fun led(token: Token, left: Expr): Expr? {
        return when (token.tokenType) {
            TokenType.Assign -> {
                if (left is IdentifierExpr) {
                    val right = parseExpression(Precedence.fromToken(token.tokenType) - 1) ?: return null

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
            TokenType.In -> {
                val unitToken = consume(TokenType.Identifier) { "Expected unit name, got ${it?.tokenType}" }

                unitToken?.let { ConversionExpr(left, it) }
            }
            TokenType.PlusAssign,
            TokenType.MinusAssign,
            TokenType.MultiplyAssign,
            TokenType.DivideAssign -> {
                if (left is IdentifierExpr) {
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

            else -> {
                reporter.report(
                    Diagnostic(
                        message = "Unexpected token '${token.value}' in expression",
                        type = DiagnosticType.ERROR,
                        line = token.line,
                        column = token.startColumn until token.endColumn
                    )
                )
                null
            }
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
            skipSeparators()

            if (peek()?.tokenType == TokenType.Eof) break

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
            TokenType.Form -> parseFormStmt()
            TokenType.OpenBracket -> parseBlockStmt()
            else -> parseExprStmt()
        }
    }

    fun parseBlockStmt(): Stmt? {
        if (consume(TokenType.OpenBracket, { "Expected '{' to start block, got ${it?.tokenType}." }) == null) {
            return null
        }

        val statements = mutableListOf<Stmt>()

        skipSeparators()

        while (
            peek() != null &&
            peek()?.tokenType != TokenType.CloseBracket &&
            peek()?.tokenType != TokenType.Eof
        ) {
            val statement = parseStatement()

            skipSeparators()

            if (statement != null) {
                statements.add(statement)
            } else {
                // Prevent an infinite loop if parsing failed
                advance()
            }

            skipSeparators()
        }

        if (consume(TokenType.CloseBracket, { "Expected '}' to close block, got ${it?.tokenType}." }) == null) {
            return null
        }

        return BlockStmt(statements)
    }

    @Deprecated("Not supported")
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

        if (peek()?.tokenType == TokenType.Assign) {
            val assign = advance()!!

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

        val currTok = peek()

        if (isSeparator(currTok?.tokenType)) {
            // Advance separator
            advance()

            return VarDeclStmt(name, null)
        }

        return null
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

    /**
     * Binds an integer exponent directly to a quantity unit, so `4 m^2` means
     * 4 square meters instead of (4 m)^2.
     */
    private fun parseUnitPower(base: UnitNode): UnitNode {
        if (peek()?.tokenType != TokenType.Power) return base

        val negative = lookup()?.tokenType == TokenType.Minus
        val exponentToken = tokens.getOrNull(currentTokenIndex + if (negative) 2 else 1)

        if (exponentToken?.tokenType != TokenType.NumberLiteral || exponentToken.value.contains('.')) {
            return base
        }

        advance() // '^'
        if (negative) advance() // '-'
        advance() // exponent

        val exponent = exponentToken.value.toInt()
        return UnitNode.PowerUnit(base, if (negative) -exponent else exponent)
    }

    private fun reportAt(token: Token?, message: String) {
        reporter.report(
            Diagnostic(
                type = DiagnosticType.ERROR,
                message = message,
                line = token?.line ?: 0,
                column = token?.let { it.startColumn until it.endColumn } ?: (0..0)
            )
        )
    }

    /**
     * `form Name(p1, p2, ...) = expression`
     */
    fun parseFormStmt(): Stmt? {
        val formToken = advance() // consume 'form'

        val name = peek()
        if (name?.tokenType != TokenType.Identifier) {
            reportAt(name ?: formToken, "Expected formula name after 'form', got '${name?.value}'")
            return null
        }
        advance()

        if (peek()?.tokenType != TokenType.OpenParenthesis) {
            reportAt(peek() ?: name, "Expected '(' after formula name '${name.value}', got '${peek()?.value}'")
            return null
        }
        advance()

        val parameters = mutableListOf<Token>()
        if (peek()?.tokenType != TokenType.CloseParenthesis) {
            while (true) {
                val parameter = peek()
                if (parameter?.tokenType != TokenType.Identifier) {
                    reportAt(parameter ?: name, "Expected parameter name in formula '${name.value}', got '${parameter?.value}'")
                    return null
                }
                advance()

                if (parameters.any { it.value == parameter.value }) {
                    reportAt(parameter, "Duplicate parameter '${parameter.value}' in formula '${name.value}'")
                    return null
                }
                parameters.add(parameter)

                if (peek()?.tokenType == TokenType.Comma) advance() else break
            }
        }

        if (peek()?.tokenType != TokenType.CloseParenthesis) {
            reportAt(peek() ?: name, "Expected ')' to close parameters of formula '${name.value}', got '${peek()?.value}'")
            return null
        }
        advance()

        if (peek()?.tokenType != TokenType.Assign) {
            reportAt(peek() ?: name, "Expected '=' after parameters of formula '${name.value}', got '${peek()?.value}'")
            return null
        }
        val assign = advance()

        val body = parseExpression(0)
        if (body == null) {
            reportAt(assign, "Expected expression after '=' in formula '${name.value}'")
            return null
        }

        if (peek() != null && peek()?.tokenType != TokenType.Eol && peek()?.tokenType != TokenType.Eof && peek()?.tokenType != TokenType.Comment) {
            reportAt(peek(), "Unexpected token '${peek()?.value}' after formula body")
            return null
        }

        return FormDeclStmt(name, parameters, body)
    }

    fun parsePrintStmt(): Stmt {
        advance() // consume "print"
        val expr = parseExpression(0)
        return PrintStmt(expr)
    }

    /**
     * Independent expression
     */
    fun parseExprStmt(): Stmt? {
        val expr = parseExpression(0) ?: return null

        // Check apakah after expression masih ada token liar di baris yang sama
        if (peek() != null && peek()?.tokenType != TokenType.Eol && peek()?.tokenType != TokenType.Eof) {
            reporter.report(
                Diagnostic(
                    message = "Unexpected token '${peek()?.value}' after expression",
                    type = DiagnosticType.ERROR,
                    line = peek()?.line ?: 0,
                    column = (peek()?.startColumn ?: 0) until (peek()?.endColumn ?: 0)
                )
            )
            return null
        }

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
