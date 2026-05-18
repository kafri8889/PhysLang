package lang.ast

interface ExprVisitor<R> {

    fun visitBinaryExpr(expr: BinaryExpr): R

    fun visitUnaryExpr(expr: UnaryExpr): R

    fun visitLiteralExpr(literal: LiteralExpr): R

    fun visitQuantityExpr(expr: QuantityExpr): R

    fun visitVariableExpr(expr: VariableExpr): R

    fun visitAssignExpr(expr: AssignExpr): R

    fun visitStringLiteralExpr(expr: StringLiteralExpr): R

    fun visitCallExpr(expr: CallExpr): R

}