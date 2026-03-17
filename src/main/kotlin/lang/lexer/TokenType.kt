package lang.lexer

data class Token(
    val value: String,
    val tokenType: TokenType
)

enum class TokenType {
    NumberLiteral,
    StringLiteral,
    Identifier,

    // Operators
    Plus,
    Minus,
    Multiply,
    Divide,
    Power,
    Unary,

    OpenParenthesis,
    CloseParenthesis,
    OpenBracket,
    CloseBracket,

    Val,
    Var,
    Assign,
    PlusAssign,
    MinusAssign,
    MultiplyAssign,
    DivideAssign,
    Equals,
    True,
    False,

    Print,
    Unit,
    Eof;
}