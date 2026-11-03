package lang.ast

import lang.lexer.Token

interface Stmt {
    fun <R> accept(stmtVisitor: StmtVisitor<R>): R
}

/**
 * Declare new variable
 *
 * Example: `var mass = 10 kg`
 *
 * @property nameToken Variable name as token
 * @property initializer Expression yg diassign to variable
 * @author Anaf
 */
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

data class BlockStmt(val statements: List<Stmt>): Stmt {
    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitBlockStmt(this)
    }
}

data class PrintStmt(val expr: Expr?): Stmt {
    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitPrintStmt(this)
    }
}

/**
 * Declare a single-line physics formula.
 *
 * Example: `form Force(m, a) = m * a`
 *
 * @property nameToken Formula name as token
 * @property parameters Parameter name tokens, in declaration order
 * @property body Expression evaluated when the formula is called
 */
data class FormDeclStmt(
    val nameToken: Token,
    val parameters: List<Token>,
    val body: Expr
): Stmt {
    val name: String get() = nameToken.value
    val parameterNames: List<String> get() = parameters.map { it.value }

    override fun <R> accept(stmtVisitor: StmtVisitor<R>): R {
        return stmtVisitor.visitFormDeclStmt(this)
    }
}
