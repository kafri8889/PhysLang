package lang.ast.visitors

import lang.ast.*
import lang.lexer.TokenType

class MathASTPrinter : ExprVisitor<String> {

    private fun operatorCharFrom(tokenType: TokenType): Char? {
        return when (tokenType) {
            TokenType.Plus -> '+'
            TokenType.Minus -> '-'
            TokenType.Multiply -> '*'
            TokenType.Divide -> '/'
            TokenType.Power -> '^'
            else -> null
        }
    }

    override fun visitBinaryExpr(expr: BinaryExpr): String {
        return parenthesize(
            operatorCharFrom(expr.operator).toString(),
            expr.left,
            expr.right
        )
    }

    override fun visitUnaryExpr(expr: UnaryExpr): String {
        return parenthesize(
            operatorCharFrom(expr.operator).toString(),
            expr.right
        )
    }

    override fun visitLiteralExpr(literal: LiteralExpr): String {
        return when (val value = literal.value) {
            is LiteralValue.IntVal -> value.value.toString()
            is LiteralValue.BoolVal -> value.value.toString()
            is LiteralValue.StringVal -> value.value
        }
    }

    override fun visitQuantityExpr(expr: QuantityExpr): String {
        return "(${expr.value.accept(this)} ${expr.unit})"
    }

    override fun visitVariableExpr(expr: VariableExpr): String {
        return expr.name
    }

    override fun visitAssignExpr(expr: AssignExpr): String {
        return parenthesize(
            expr.name,
            expr.value
        )
    }

    override fun visitStringLiteralExpr(expr: StringLiteralExpr): String {
        return expr.value
    }

    private fun parenthesize(
        operator: String,
        vararg exprs: Expr
    ): String {

        val builder = StringBuilder()

        builder.append("(")
        builder.append(operator)

        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }

        builder.append(")")

        return builder.toString()
    }
}