package lang.parser

import lang.lexer.TokenType

class Precedence {

    companion object {

        fun fromToken(tokenType: TokenType?): Int {
            return when (tokenType) {
                TokenType.OpenParenthesis -> 100
                TokenType.Assign -> 5
                TokenType.PlusAssign -> 5
                TokenType.MinusAssign -> 5
                TokenType.MultiplyAssign -> 5
                TokenType.DivideAssign -> 5
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
