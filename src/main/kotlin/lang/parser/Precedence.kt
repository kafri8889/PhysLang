package lang.parser

import lang.lexer.TokenType

class Precedence {

    companion object {

        fun fromToken(tokenType: TokenType?): Int {
            return when (tokenType) {
                TokenType.Assign -> 5
                TokenType.Plus -> 10
                TokenType.Minus -> 10
                TokenType.Multiply -> 20
                TokenType.Divide -> 20
                TokenType.Power -> 30
                TokenType.Unary -> 40
                else -> 0
            }
        }
    }
}
