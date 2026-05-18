package lang.ast.visitors

import lang.ast.*
import lang.core.Environment
import lang.lexer.TokenType
import kotlin.math.pow

class PhysicsEvaluator(
    private val environment: Environment = Environment()
): ExprVisitor<RuntimeValue>, StmtVisitor<Unit> {

    override fun visitBinaryExpr(expr: BinaryExpr): RuntimeValue {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)

        return when (expr.operator) {
            TokenType.Plus -> {
                when {
                    left is PhysicsValue && right is PhysicsValue -> {
                        val total = left.scaledValue + right.scaledValue
                        val newValue = total / left.scale

                        PhysicsValue(
                            value = newValue,
                            dimensions = left.dimensions,
                            scale = left.scale,
                            unitName = left.unitName
                        )
                    }

                    left is StringValue || right is StringValue -> {
                        StringValue(left.toString() + right.toString())
                    }

                    else -> PhysicsValue.zero
                }
            }
            TokenType.Minus -> {
                left as PhysicsValue
                right as PhysicsValue

                val total = left.scaledValue - right.scaledValue
                val newValue = total / left.scale

                PhysicsValue(
                    value = newValue,
                    dimensions = left.dimensions,
                    scale = left.scale,
                    unitName = left.unitName
                )
            }
            TokenType.Multiply -> {
                left as PhysicsValue
                right as PhysicsValue

                val value = left.scaledValue * right.scaledValue
                val newDimension = IntArray(7) { i ->
                    left.dimensions[i] + right.dimensions[i]
                }

                PhysicsValue(value, newDimension)
            }
            TokenType.Divide -> {
                left as PhysicsValue
                right as PhysicsValue

                val value = left.scaledValue / right.scaledValue
                val newDimension = IntArray(7) { i ->
                    left.dimensions[i] - right.dimensions[i]
                }

                PhysicsValue(value, newDimension)
            }

            TokenType.Power -> {
                left as PhysicsValue
                right as PhysicsValue

                val exponent = right.scaledValue.toInt()
                val value = left.scaledValue.pow(right.scaledValue)

                // Contoh: (m^1 / s^1) ^ 2 -> m^(1*2) / s^(1*2) -> m^2 / s^2
                val newDimension = IntArray(7) { i ->
                    left.dimensions[i] * exponent
                }

                PhysicsValue(value, newDimension)

            }
            else -> PhysicsValue.zero
        }
    }

    override fun visitUnaryExpr(expr: UnaryExpr): RuntimeValue {
        val right = expr.right.accept(this)

        when (right) {
            is PhysicsValue -> return PhysicsValue(
                value = if (expr.operator == TokenType.Minus) -right.value else right.value,
                dimensions = right.dimensions,
                scale = right.scale,
                unitName = right.unitName
            )
            else -> return PhysicsValue.zero
        }
    }

    override fun visitLiteralExpr(literal: LiteralExpr): RuntimeValue {
        val value = when (val literal = literal.value) {
            is LiteralValue.IntVal -> literal.value.toDouble()
            is LiteralValue.DoubleVal -> literal.value
            else -> 0.0
        }

        return PhysicsValue(
            value = value,
            dimensions = IntArray(7),
            scale = 1.0
        )
    }

    override fun visitQuantityExpr(expr: QuantityExpr): RuntimeValue {
        val unitName = when (val unit = expr.unit) {
            is UnitNode.BaseUnit -> unit.name
            else -> ""
        }

        var unitScale = environment.getUnit(unitName)

        if (unitScale == null) {
            unitScale = PhysicsValue.unitRegistry[unitName]
        }

        val physicsValue = expr.value.accept(this)
        physicsValue as PhysicsValue
        val baseUnit = unitScale ?: PhysicsValue.zero

        return PhysicsValue(
            value = physicsValue.value,
            scale = baseUnit.value * baseUnit.scale,
            dimensions = baseUnit.dimensions,
            unitName = unitName
        )
    }

    override fun visitVariableExpr(expr: VariableExpr): RuntimeValue {
        val value = environment.getVar(expr.name)
        if (value != null) return value

        val customUnit = environment.getUnit(expr.name)
        if (customUnit is PhysicsValue) {
            return PhysicsValue(
                value = 1.0,
                dimensions = customUnit.dimensions,
                scale = customUnit.value * customUnit.scale,
                unitName = expr.name
            )
        }

        val builtinUnit = PhysicsValue.unitRegistry[expr.name]
        if (builtinUnit != null) {
            return PhysicsValue(
                value = 1.0,
                dimensions = builtinUnit.dimensions,
                scale = builtinUnit.scale,
                unitName = expr.name
            )
        }

        return PhysicsValue.zero
    }

    override fun visitAssignExpr(expr: AssignExpr): RuntimeValue {
        val value = expr.value.accept(this)

        environment.putVar(expr.name, value)

        return value
    }

    override fun visitStringLiteralExpr(expr: StringLiteralExpr): RuntimeValue {
        return StringValue(expr.value)
    }

    override fun visitCallExpr(expr: CallExpr): RuntimeValue {
        val callee = expr.callee.accept(this)
        val args = expr.arguments.map { it.accept(this) }

        callee as RuntimeValue.NativeFunction
        return callee.callable.call(this, args)
    }

    override fun visitExprStmt(exprStmt: ExprStmt) {
        val value = exprStmt.expr?.accept(this)

        if (exprStmt.expr is VariableExpr) {
            println("$value")
        }
    }

    override fun visitVarDeclStmt(varDeclStmt: VarDeclStmt) {
        val value = varDeclStmt.initializer?.accept(this)

        environment.putVar(varDeclStmt.name, value)
    }

    override fun visitPrintStmt(printStmt: PrintStmt) {
        val value = printStmt.expr?.accept(this)

        println(value)
    }

    override fun visitUnitDeclStmt(unitDeclStmt: UnitDeclStmt) {
        val value = unitDeclStmt.initializer.accept(this)

        if (value is PhysicsValue) environment.putUnit(unitDeclStmt.name, value)
    }
}
