package lang.ast

import lang.lexer.Token

sealed interface LiteralValue {

    val valueToken: Token

    data class IntVal(override val valueToken: Token) : LiteralValue {
        val value: Int get() = valueToken.value.toInt()
    }

    data class DoubleVal(override val valueToken: Token) : LiteralValue {
        val value: Double get() = valueToken.value.toDouble()
    }

    data class StringVal(override val valueToken: Token) : LiteralValue {
        val value: String get() = valueToken.value
    }

    data class BoolVal(override val valueToken: Token) : LiteralValue {
        val value: Boolean get() = valueToken.value.toBoolean()
    }

}

data class LiteralExpr(
    val value: LiteralValue
) : Expr() {

    override fun <R> accept(exprVisitor: ExprVisitor<R>): R {
        return exprVisitor.visitLiteralExpr(this)
    }

}
