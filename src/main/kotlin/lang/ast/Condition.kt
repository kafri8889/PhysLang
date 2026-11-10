package lang.ast

import lang.lexer.Token
import lang.lexer.TokenType

/**
 * Physical relation used as the condition of an `if`.
 *
 * PhysLang has no truthiness: a condition is always a relation between
 * physical quantities, never a plain value.
 */
sealed class Condition {

    /**
     * Compare two quantities with the same dimensions.
     *
     * Example: `F > maxLoad`, `g == 9.81 m/s^2 within 0.05 m/s^2`
     *
     * @property left Left operand
     * @property operatorToken One of `<`, `<=`, `>`, `>=`, `==`, `!=`
     * @property right Right operand
     * @property tolerance Optional measurement tolerance, only valid for `==` and `!=`
     */
    data class Comparison(
        val left: Expr,
        val operatorToken: Token,
        val right: Expr,
        val tolerance: Expr? = null
    ): Condition() {
        val operator: TokenType get() = operatorToken.tokenType
    }

    /**
     * Inclusive range check, all three quantities must have the same dimensions.
     *
     * Example: `T between 273 K and 373 K`
     *
     * @property value Quantity being checked
     * @property betweenToken `between` token
     * @property lower Lower bound (inclusive)
     * @property upper Upper bound (inclusive)
     */
    data class Between(
        val value: Expr,
        val betweenToken: Token,
        val lower: Expr,
        val upper: Expr
    ): Condition()

    /**
     * Check whether a quantity has the same dimensions as a unit expression.
     *
     * Example: `q is N`, `v is m/s`
     *
     * @property value Quantity being checked
     * @property isToken `is` token
     * @property target Pure unit expression (units combined with `*`, `/`, `^`)
     */
    data class DimensionCheck(
        val value: Expr,
        val isToken: Token,
        val target: Expr
    ): Condition()
}
