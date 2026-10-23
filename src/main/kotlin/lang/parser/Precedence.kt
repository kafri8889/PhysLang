package lang.parser

import lang.lexer.TokenType

class Precedence {

    companion object {

        fun fromToken(tokenType: TokenType?): Int {
            return when (tokenType) {
                TokenType.OpenParenthesis -> 6
                TokenType.Assign -> 1
                TokenType.PlusAssign -> 1
                TokenType.MinusAssign -> 1
                TokenType.MultiplyAssign -> 1
                TokenType.DivideAssign -> 1
                TokenType.Plus -> 2
                TokenType.Minus -> 2
                TokenType.Multiply -> 3
                TokenType.Divide -> 3
                TokenType.Power -> 4
                TokenType.Unary -> 5
                else -> 0
            }
        }
    }
}
