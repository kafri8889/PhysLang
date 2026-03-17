package lang.ast

import lang.lexer.TokenType

sealed class Expr: Node {

}

class BinaryExpr(
    val left: Expr,
    val operator: TokenType,
    val right: Expr
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitBinaryExpr(this)
    }
}

class UnaryExpr(
    val operator: TokenType,
    val right: Expr
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitUnaryExpr(this)
    }

    override fun toString(): String {
        return "UnaryExpr(operator=$operator, right=$right)"
    }
}

/**
 * Represent value like "3 kg", "2 m/s", etc.
 */
class QuantityExpr(
    val value: LiteralExpr,     // Angkanya (misal: 10)
    val unit: UnitNode          // Satuannya (misal: "kg" atau "m/s")
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitQuantityExpr(this)
    }

    override fun toString(): String {
        return "QuantityExpr($value $unit)"
    }
}

class VariableExpr(
    val name: String
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitVariableExpr(this)
    }
}

/**
 * Assign expression to variable
 * @property name Variable name
 * @property expr Expression
 * @author Anaf
 */
class AssignExpr(
    val name: String,
    val value: Expr
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitAssignExpr(this)
    }
}
