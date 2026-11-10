package lang.core

import lang.lexer.TokenType

val keywords = buildMap {
//    put("val", TokenType.Val)
//    put("var", TokenType.Var)
    put("form", TokenType.Form)
    put("in", TokenType.In)
    put("if", TokenType.If)
    put("else", TokenType.Else)
    put("true", TokenType.True)
    put("false", TokenType.False)
    put("print", TokenType.Print)
    put("unit", TokenType.Unit)
    put("null", TokenType.Null)
    put("within", TokenType.Within)
    put("between", TokenType.Between)
    put("and", TokenType.And)
    put("is", TokenType.Is)
}
