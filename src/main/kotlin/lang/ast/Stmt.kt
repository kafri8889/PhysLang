package lang.ast

interface Stmt {
    fun <R> accept(stmtVisitor: StmtVisitor<R>): R
}

data class VarDeclStmt(
    val name: String,
    val initializer: Expr?
) : Stmt {
    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitVarDeclStmt(this)
    }
}

data class ExprStmt(val expr: Expr?) : Stmt {
    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitExprStmt(this)
    }
}
