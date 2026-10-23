package lang.ast

import lang.lexer.Token
import lang.lexer.TokenType

sealed class Expr: Node {

}

data class StringLiteralExpr(val value: String): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitStringLiteralExpr(this)
    }
}

data class BinaryExpr(
    val left: Expr,
    val operatorToken: Token,
    val right: Expr
): Expr() {
    val operator: TokenType get() = operatorToken.tokenType

    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitBinaryExpr(this)
    }
}

data class UnaryExpr(
    val operatorToken: Token,
    val right: Expr
): Expr() {
    val operator: TokenType get() = operatorToken.tokenType

    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitUnaryExpr(this)
    }
}

/**
 * Represent value like "3 kg", "2 m/s", etc.
 */
data class QuantityExpr(
    val value: LiteralExpr,     // Angkanya (misal: 10)
    val unit: UnitNode          // Satuannya (misal: "kg" atau "m/s")
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitQuantityExpr(this)
    }
}

/**
 *
 *
 * Example: `mass`
 *
 * @property nameToken Variable name as token
 * @author Anaf
 */
data class VariableExpr(
    val nameToken: Token
): Expr() {
    val name: String get() = nameToken.value

    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitVariableExpr(this)
    }
}

/**
 * Assign expression to variable
 *
 * Example: `mass = 10 kg`
 *
 * @property nameToken Variable name as token
 * @property value Expression
 * @author Anaf
 */
data class AssignExpr(
    val nameToken: Token,
    val value: Expr
): Expr() {
    val name: String get() = nameToken.value

    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitAssignExpr(this)
    }
}

/**
 * Call function
 *
 * Example: `SI(10)`
 *
 * @property callee Function name
 * @property parenToken Parenthesis token
 * @property arguments Arguments
 */
data class CallExpr(
    val callee: Expr, // Siapa yang dipanggil (Contoh: Identifier "SI")
    val parenToken: Token,
    val arguments: List<Expr>
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitCallExpr(this)
    }
}
