package lang.ast

interface StmtVisitor<R> {

    fun visitExprStmt(exprStmt: ExprStmt): R

    fun visitVarDeclStmt(varDeclStmt: VarDeclStmt): R

}