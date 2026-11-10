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

    override fun visitIdentifierExpr(expr: IdentifierExpr): String {
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

    override fun visitConversionExpr(expr: ConversionExpr): String {
        TODO("Not yet implemented")
    }

    override fun visitIfExpr(expr: IfExpr): String {
        val condition = when (val condition = expr.condition) {
            is Condition.Comparison -> {
                val tolerance = condition.tolerance?.let { " within ${it.accept(this)}" } ?: ""
                "${condition.left.accept(this)} ${condition.operatorToken.value} ${condition.right.accept(this)}$tolerance"
            }
            is Condition.Between -> "${condition.value.accept(this)} between ${condition.lower.accept(this)} and ${condition.upper.accept(this)}"
            is Condition.DimensionCheck -> "${condition.value.accept(this)} is ${condition.target.accept(this)}"
        }

        fun branch(branch: IfBranch?): String = when (branch) {
            is IfBranch.ExprBranch -> branch.expr.accept(this)
            is IfBranch.BlockBranch -> "{...}"
            null -> ""
        }

        return "(if ($condition) ${branch(expr.thenBranch)} ${branch(expr.elseBranch)})".replace(" )", ")")
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