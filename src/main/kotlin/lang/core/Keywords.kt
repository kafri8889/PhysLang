package lang.core

import lang.lexer.TokenType

val keywords = buildMap {
    put("val", TokenType.Val)
    put("var", TokenType.Var)
    put("true", TokenType.True)
    put("false", TokenType.False)
    put("=", TokenType.Assign)
    put("==", TokenType.Equals)
}
