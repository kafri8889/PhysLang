package lang.lexer

import lang.core.keywords

class Lexer(private val source: String) {
    private val tokens = mutableListOf<Token>()

    private var currentGlobalPosition = 0
    private var currentPositionInLine = 0
    private var currentLine = 0

    fun advance(): Char? {
        val tok = peek()
        currentGlobalPosition++
        return tok
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

    fun parseStringLiteral(): String {
        var content = ""

        advance() // consume '"'

        while (peek() != '"') {
            content += advance()
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

            when {
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
                    value = parseStringLiteral()
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

            if (value != null && tokenType != null) tokens.add(Token(value, tokenType))
        }

        return tokens
    }

}