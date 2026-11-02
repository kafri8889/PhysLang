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

    @Deprecated("Not used in v2") Val,
    @Deprecated("Not used in v2") Var,
    Form,
    In,
    If,
    Else,
    Less,
    Greater,
    GreaterEqual,
    LessEqual,
    NotEqual,
    Not,
    Null,
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