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
data class IdentifierExpr(
    val nameToken: Token
): Expr() {
    val name: String get() = nameToken.value

    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitIdentifierExpr(this)
    }
}

/**
 * Assign expression to variable, if variable not exist, create new variable
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

/**
 * Expression for unit conversion
 *
 * Example: `10 kg in g`
 *
 * @param left Expression representing the value to be converted.
 * @param unitToken Token representing the target unit of conversion.
 */
data class ConversionExpr(
    val left: Expr,
    val unitToken: Token
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitConversionExpr(this)
    }
}

/**
 * Branch of an [IfExpr], either a single expression or a block.
 */
sealed interface IfBranch {

    data class ExprBranch(val expr: Expr) : IfBranch

    /**
     * When the `if` is used as a value, the block value is its last expression.
     */
    data class BlockBranch(val block: BlockStmt) : IfBranch
}

/**
 * Conditional branching on a physical relation, usable as statement or expression.
 *
 * Example: `a = if (v < 1 m/s) 2 N else { 3 N }`
 *
 * @property ifToken `if` token
 * @property condition Physical relation to test
 * @property thenBranch Branch executed when the condition holds
 * @property elseBranch Optional else branch, an `else if` is an [IfBranch.ExprBranch] holding another [IfExpr]
 * @property isExpression True when the value is used, so `else` is required and branches must have the same type
 */
data class IfExpr(
    val ifToken: Token,
    val condition: Condition,
    val thenBranch: IfBranch,
    val elseBranch: IfBranch?,
    val isExpression: Boolean
): Expr() {
    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitIfExpr(this)
    }
}
