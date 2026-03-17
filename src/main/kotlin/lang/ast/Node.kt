package lang.ast

import lang.lexer.TokenType

interface Node {
    fun <R> accept(exprVisitor: ExprVisitor<R>): R
}

sealed interface UnitNode {

    // Untuk satuan dasar, misal: "kg", "m", "s"
    data class BaseUnit(val name: String) : UnitNode

    // Untuk satuan turunan, misal: "kg * m" atau "m / s"
    data class CompositeUnit(
        val left: UnitNode,
        val operator: TokenType, // ONLY Token.Multiply or Token.Divide
        val right: UnitNode
    ) : UnitNode

    // Untuk pangkat, misal "s^2"
    data class PowerUnit(
        val base: UnitNode,
        val exponent: Int
    ) : UnitNode
}