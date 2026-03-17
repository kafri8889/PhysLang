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

    fun parseNumber(): String {
        var number = ""

        do {
            number += advance()
        } while (peek()?.isDigit() == true)

        return number
    }

    fun parseIdentifier(): Pair<String, TokenType> {
        var iden = ""

        do {
            iden += advance()
        } while (peek()?.isLetterOrDigit() == true)

        val tokenType = keywords[iden] ?: TokenType.Identifier

        return iden to tokenType
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
                peek() == '^' -> {
                    tokenType = TokenType.Power
                    value = advance().toString()
                }
                peek() == '+' -> {
                    tokenType = TokenType.Plus
                    value = advance().toString()
                }
                peek() == '-' -> {
                    tokenType = TokenType.Minus
                    value = advance().toString()
                }
                peek() == '*' -> {
                    tokenType = TokenType.Multiply
                    value = advance().toString()
                }
                peek() == '/' -> {
                    tokenType = TokenType.Divide
                    value = advance().toString()
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