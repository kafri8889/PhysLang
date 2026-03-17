package lang.ast

sealed interface LiteralValue {

    data class IntVal(val value: Int) : LiteralValue

    data class DoubleVal(val value: Double) : LiteralValue

    data class StringVal(val value: String) : LiteralValue

    data class BoolVal(val value: Boolean) : LiteralValue

}

data class LiteralExpr(
    val value: LiteralValue
) : Expr() {

    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitLiteralExpr(this)
    }

}
