package lang.ast.visitors

import lang.ast.*

class ASTPrinter : ExprVisitor<String> {

    override fun visitBinaryExpr(expr: BinaryExpr): String {
        return parenthesize(
            expr.operator.toString(),
            expr.left,
            expr.right
        )
    }

    override fun visitUnaryExpr(expr: UnaryExpr): String {
        return parenthesize(
            expr.operator.toString(),
            expr.right
        )
    }

    override fun visitLiteralExpr(literal: LiteralExpr): String {
        return literal.value.toString()
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

    override fun visitCallExpr(expr: CallExpr): String {
        return "implement this call expr later"
    }

    private fun parenthesize(
        name: String,
        vararg exprs: Expr
    ): String {

        val builder = StringBuilder()

        builder.append("(")
        builder.append(name)

        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }

        builder.append(")")

        return builder.toString()
    }
}