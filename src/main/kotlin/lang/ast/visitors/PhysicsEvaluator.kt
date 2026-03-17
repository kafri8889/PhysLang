package lang.ast.visitors

import lang.ast.*
import lang.core.Environment
import lang.lexer.TokenType

class PhysicsEvaluator(
    private val environment: Environment = Environment()
): ExprVisitor<RuntimeValue>, StmtVisitor<Unit> {

    private fun dimensionEquals(left: IntArray, right: IntArray): Boolean {
        return if (right.all { it == 0 }) true // Scalar
        else left.contentEquals(right)
    }

    override fun visitBinaryExpr(expr: BinaryExpr): RuntimeValue {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)

        return when (expr.operator) {
            TokenType.Plus -> {
                if (left is PhysicsValue && right is PhysicsValue) {
                    if (!dimensionEquals(left.dimensions, right.dimensions)) {
                        throw Exception("Dimensions mismatch: Cant sum different dimensions.")
                    }

                    val total = left.scaledValue + right.scaledValue
                    val newValue = total / left.scale

                    return PhysicsValue(
                        value = newValue,
                        dimensions = left.dimensions,
                        scale = left.scale,
                        unitName = left.unitName
                    )
                }

                // String + String, String + PV (String Concatenation)
                // Example: "Result: " + 5 kg -> "Result: 5.0 kg"
                if (left is StringValue || right is StringValue) {
                    return StringValue(left.toString() + right.toString())
                }

                throw Exception("Operation (\"+\") not supported yet!")
            }
            TokenType.Minus -> {
                if (left is PhysicsValue && right is PhysicsValue) {
                    if (!dimensionEquals(left.dimensions, right.dimensions)) {
                        throw Exception("Dimensions mismatch: Cant sub different dimensions.")
                    }

                    val total = left.scaledValue - right.scaledValue
                    val newValue = total / left.scale

                    return PhysicsValue(
                        value = newValue,
                        dimensions = left.dimensions,
                        scale = left.scale,
                        unitName = left.unitName
                    )
                }

                throw Exception("Operation (\"-\") not supported yet!")
            }
            TokenType.Multiply -> {
                if (left !is PhysicsValue || right !is PhysicsValue) {
                    throw Exception("The '*' operation can only be used for numbers or physical units!")
                }

                val value = left.scaledValue * right.scaledValue
                val newDimension = IntArray(7) { i ->
                    left.dimensions[i] + right.dimensions[i]
                }

                PhysicsValue(value, newDimension)
            }
            TokenType.Divide -> {
                if (left !is PhysicsValue || right !is PhysicsValue) {
                    throw Exception("The '/' operation can only be used for numbers or physical units!")
                }

                val value = left.scaledValue / right.scaledValue
                val newDimension = IntArray(7) { i ->
                    left.dimensions[i] - right.dimensions[i]
                }

                return PhysicsValue(value, newDimension)
            }
            else -> throw Exception("Operator invalid or not supported: ${expr.operator}")
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
            else -> throw Exception("Operation (\"${expr.operator}\") can't be applied to ${right}!")
        }
    }

    override fun visitLiteralExpr(literal: LiteralExpr): RuntimeValue {
        val value = when (val literal = literal.value) {
            is LiteralValue.IntVal -> literal.value.toDouble()
            is LiteralValue.DoubleVal -> literal.value
            else -> throw Exception("Invalid literal: $literal")
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

        if (unitScale == null) {
            throw Exception("Unknown unit: $unitName")
        }

        val physicsValue = expr.value.accept(this)

        if (physicsValue is StringValue) throw IllegalArgumentException("Cannot construct physics value from string, use numbers!")
        if (physicsValue is PhysicsValue) return PhysicsValue(
            value = physicsValue.value,
            scale = unitScale.scale,
            dimensions = unitScale.dimensions,
            unitName = unitName
        )

        throw Exception("Cannot construct physics value from $physicsValue, use numbers!")
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

        throw Exception("Variable or unit '${expr.name}' is not defined!")
    }

    override fun visitAssignExpr(expr: AssignExpr): RuntimeValue {
        val value = expr.value.accept(this)

        environment.putVar(expr.name, value)

        return value
    }

    override fun visitStringLiteralExpr(expr: StringLiteralExpr): RuntimeValue {
        return StringValue(expr.value)
    }

    override fun visitExprStmt(exprStmt: ExprStmt) {
        val value = exprStmt.expr?.accept(this)

        if (exprStmt.expr is VariableExpr) {
            println("$value")
        }
    }

    override fun visitVarDeclStmt(varDeclStmt: VarDeclStmt) {
        val value = varDeclStmt.initializer?.accept(this)

        if (environment.getVar(varDeclStmt.name) != null) throw Exception("Conflicting declaration: ${varDeclStmt.name} already defined!")

        environment.putVar(varDeclStmt.name, value)
    }

    override fun visitPrintStmt(printStmt: PrintStmt) {
        val value = printStmt.expr?.accept(this)

        println(value)
    }

    override fun visitUnitDeclStmt(unitDeclStmt: UnitDeclStmt) {
        val value = unitDeclStmt.initializer.accept(this)

        if (value is StringValue) throw Exception("Cant create new unit from string!")
        if (value is PhysicsValue) environment.putUnit(unitDeclStmt.name, value)
    }
}