package lang.lexer

data class Token(
    val value: String,
    val tokenType: TokenType,
    val line: Int,
    val startColumn: Int,
    val endColumn: Int,
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

    Comment,
    Comma,
    Slash,
    Print,
    Unit,
    Eol,
    Eof;
}