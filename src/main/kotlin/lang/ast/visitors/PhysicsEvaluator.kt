package lang.ast.visitors

import lang.ast.*
import lang.core.Environment
import lang.core.PhysicsValue
import lang.core.PhysicsValue.Companion.emptyDimensions
import lang.lexer.TokenType

class PhysicsEvaluator(
    private val environment: Environment = Environment()
): ExprVisitor<PhysicsValue>, StmtVisitor<Unit> {

    private fun dimensionEquals(left: IntArray, right: IntArray): Boolean {
        return if (right.all { it == 0 }) true // Scalar
        else left.contentEquals(right)
    }

    override fun visitBinaryExpr(expr: BinaryExpr): PhysicsValue {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)

        return when (expr.operator) {
            TokenType.Plus -> {
                if (!dimensionEquals(left.dimensions, right.dimensions)) throw Exception("Dimensions mismatch!")

                PhysicsValue(left.scaledValue + right.scaledValue, left.dimensions)
            }
            TokenType.Minus -> {
                if (!dimensionEquals(left.dimensions, right.dimensions)) throw Exception("Dimensions mismatch!")

                PhysicsValue(left.scaledValue - right.scaledValue, left.dimensions)
            }
            TokenType.Multiply -> {
                val value = left.scaledValue * right.scaledValue
                val newDimension = IntArray(7) { i ->
                    left.dimensions[i] + right.dimensions[i]
                }

                PhysicsValue(value, newDimension)
            }
            TokenType.Divide -> {
                val value = left.scaledValue / right.scaledValue
                val newDimension = IntArray(7) { i ->
                    left.dimensions[i] - right.dimensions[i]
                }

                PhysicsValue(value, newDimension)
            }
            else -> PhysicsValue(0.0, emptyDimensions)
        }
    }

    override fun visitUnaryExpr(expr: UnaryExpr): PhysicsValue {
        val right = expr.right.accept(this)

        return PhysicsValue(
            value = if (expr.operator == TokenType.Minus) -right.value else right.value,
            dimensions = right.dimensions
        )
    }

    override fun visitLiteralExpr(literal: LiteralExpr): PhysicsValue {
        return PhysicsValue(
            dimensions = emptyDimensions,
            value = when (val literalValue = literal.value) {
                is LiteralValue.IntVal -> literalValue.value.toDouble()
                else -> 0.0
            }
        )
    }

    override fun visitQuantityExpr(expr: QuantityExpr): PhysicsValue {
        val unitScale = when (val unit = expr.unit) {
            is UnitNode.BaseUnit -> PhysicsValue.unitRegistry[unit.name]
            else -> PhysicsValue(0.0, emptyDimensions)
        }

        return PhysicsValue(
            value = expr.value.accept(this).value,
            scale = unitScale?.scale ?: 1.0,
            dimensions = unitScale?.dimensions ?: emptyDimensions
        )
    }

    override fun visitVariableExpr(expr: VariableExpr): PhysicsValue {
        val value = environment.get(expr.name)

        if (value == null) throw Exception("Variable ${expr.name} not found!")

        return value
    }

    override fun visitAssignExpr(expr: AssignExpr): PhysicsValue {
        val value = expr.value.accept(this)

        environment.put(expr.name, value)

        return value
    }

    override fun visitExprStmt(exprStmt: ExprStmt) {
        val value = exprStmt.expr?.accept(this)

        if (exprStmt.expr !is AssignExpr) {
            println("$value")
        }
    }

    override fun visitVarDeclStmt(varDeclStmt: VarDeclStmt) {
        val value = varDeclStmt.initializer?.accept(this)

        environment.put(varDeclStmt.name, value)
    }
}