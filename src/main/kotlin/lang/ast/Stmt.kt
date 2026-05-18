package lang.ast

import lang.lexer.Token

interface Stmt {
    fun <R> accept(stmtVisitor: StmtVisitor<R>): R
}

data class VarDeclStmt(
    val nameToken: Token,
    val initializer: Expr?
) : Stmt {
    val name: String get() = nameToken.value

    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitVarDeclStmt(this)
    }
}

data class ExprStmt(val expr: Expr?): Stmt {
    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitExprStmt(this)
    }
}

/**
 * Create new unit (typealias)
 */
data class UnitDeclStmt(
    val nameToken: Token,
    val initializer: Expr
): Stmt {
    val name: String get() = nameToken.value

    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitUnitDeclStmt(this)
    }
}

data class PrintStmt(val expr: Expr?): Stmt {
    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitPrintStmt(this)
    }
}
