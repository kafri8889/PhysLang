package lang.lexer

import lang.core.keywords
import lang.error.Diagnostic
import lang.error.DiagnosticType
import lang.error.ErrorReporter

class Lexer(
    private val source: String,
    private val reporter: ErrorReporter = ErrorReporter()
) {
    private val tokens = mutableListOf<Token>()

    private var currentGlobalPosition = 0
    private var currentPositionInLine = 0
    private var currentLine = 0

    fun advance(): Char? {
        val ch = peek() ?: return null
        currentGlobalPosition++

        if (ch == '\n') {
            currentLine++
            currentPositionInLine = 0
        } else {
            currentPositionInLine++
        }

        return ch
    }

    fun peek(): Char? {
        return source.getOrNull(currentGlobalPosition)
    }

    fun lookahead(n: Int): Char? {
        return source.getOrNull(currentGlobalPosition + n)
    }

    private fun parseNumber(): String {
        var numberString = ""
        while (peek()?.isDigit() == true) {
            numberString += advance()
        }
        if (peek() == '.' && lookahead(1)?.isDigit() == true) {
            numberString += advance()
            while (peek()?.isDigit() == true) {
                numberString += advance()
            }
        }
        return numberString
    }

    fun parseIdentifier(): Pair<String, TokenType> {
        var iden = ""

        do {
            iden += advance()
        } while (peek()?.isLetterOrDigit() == true)

        val tokenType = keywords[iden] ?: TokenType.Identifier

        return iden to tokenType
    }

    fun parseStringLiteral(startCol: Int): String {
        var content = ""

        advance() // consume '"'

        while (peek() != '"') {
            content += advance()
        }

        if (peek() == null) {
            reporter.report(
                Diagnostic(
                    message = "Unterminated string literal",
                    type = DiagnosticType.ERROR,
                    line = currentLine,
                    column = startCol until currentPositionInLine
                )
            )
            return content
        }

        advance() // consume '"'

        return content
    }

    fun parseComment(): String {
        var content = ""

        advance() // consume '/'
        advance() // consume '/'

        while (peek() != '\n' && peek() != '\u0000' && peek() != null) {
            content += advance()
        }

        return content
    }

    fun lex(): List<Token> {
        while (currentGlobalPosition < source.length) {
            var tokenType: TokenType? = null
            var value: String? = null
            val startCol = currentPositionInLine

            when {
                peek() == '\n' -> {
                    tokenType = TokenType.Eol
                    value = "\\n"
                    advance()
                }
                peek() == '\r' -> advance()
                peek()?.isWhitespace() == true -> advance()
                peek()?.isLetter() == true -> {
                    val (s, t) = parseIdentifier()

                    tokenType = t
                    value = s
                }
                peek()?.isDigit() ?: false -> {
                    tokenType = TokenType.NumberLiteral
                    value = parseNumber()
                }
                peek() == '"' -> {
                    tokenType = TokenType.StringLiteral
                    value = parseStringLiteral(startCol)
                }
                peek() == '^' -> {
                    tokenType = TokenType.Power
                    value = advance().toString()
                }
                peek() == '+' -> {
                    if (lookahead(1) == '=') {
                        tokenType = TokenType.PlusAssign
                        value = "+="

                        advance()
                        advance()
                    } else {
                        tokenType = TokenType.Plus
                        value = advance().toString()
                    }
                }
                peek() == '-' -> {
                    if (lookahead(1) == '=') {
                        tokenType = TokenType.MinusAssign
                        value = "-="

                        advance()
                        advance()
                    } else {
                        tokenType = TokenType.Minus
                        value = advance().toString()
                    }
                }
                peek() == '*' -> {
                    if (lookahead(1) == '=') {
                        tokenType = TokenType.MultiplyAssign
                        value = "*="

                        advance()
                        advance()
                    } else {
                        tokenType = TokenType.Multiply
                        value = advance().toString()
                    }
                }
                peek() == '/' -> {
                    if (lookahead(1) == '=') {
                        tokenType = TokenType.DivideAssign
                        value = "/="

                        advance()
                        advance()
                    } else if (lookahead(1) == '/') {
                        tokenType = TokenType.Comment
                        value = parseComment()
                    } else {
                        tokenType = TokenType.Divide
                        value = advance().toString()
                    }
                }
                peek() == '(' -> {
                    tokenType = TokenType.OpenParenthesis
                    value = advance().toString()
                }
                peek() == ')' -> {
                    tokenType = TokenType.CloseParenthesis
                    value = advance().toString()
                }
                peek() == '{' -> {
                    tokenType = TokenType.OpenBracket
                    value = advance().toString()
                }
                peek() == '}' -> {
                    tokenType = TokenType.CloseBracket
                    value = advance().toString()
                }
                peek() == ',' -> {
                    tokenType = TokenType.Comma
                    value = advance().toString()
                }
                peek() == '=' -> {
                    if (lookahead(1) == '=') {
                        tokenType = TokenType.Equals
                        value = "=="

                        advance()
                        advance()
                    } else {
                        tokenType = TokenType.Assign
                        value = advance().toString()
                    }
                }
                else -> advance()
            }

            val endCol = currentPositionInLine

            if (value != null && tokenType != null) tokens.add(Token(value, tokenType, currentLine, startCol, endCol))
        }

        tokens.add(Token("", TokenType.Eof, currentLine, currentPositionInLine, currentPositionInLine))

        return tokens
    }

}