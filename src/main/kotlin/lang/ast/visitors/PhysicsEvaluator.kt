package lang.ast.visitors

import lang.ast.*
import lang.core.Environment
import lang.lexer.TokenType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

class PhysicsEvaluator(
    private val environment: Environment = Environment()
): ExprVisitor<RuntimeValue>, StmtVisitor<Unit> {

    // Current active environment, when evaluator enter block expression
    // Change the environment to new environment (in block)
    private var currentEnvironment = environment

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
        when (val right = expr.right.accept(this)) {
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
            is LiteralValue.NullVal -> return RuntimeValue.NullValue
            else -> 0.0
        }

        return PhysicsValue(
            value = value,
            dimensions = IntArray(7),
            scale = 1.0
        )
    }

    override fun visitQuantityExpr(expr: QuantityExpr): RuntimeValue {
        val unit = currentEnvironment.resolveUnitNode(expr.unit)
            ?: throw RuntimeException("Unknown unit in quantity: ${expr.unit}")
        val numeric = (expr.value.accept(this) as PhysicsValue).value

        // The literal carries only the number; the unit scale is applied exactly once.
        return PhysicsValue(
            value = numeric,
            scale = unit.scale,
            dimensions = unit.dimensions,
            unitName = unit.unitName
        )
    }

    override fun visitIdentifierExpr(expr: IdentifierExpr): RuntimeValue {
        val value = currentEnvironment.getVar(expr.name, true)
        if (value != null) return value

        val hasVar = currentEnvironment.hasVar(expr.name)
        if (hasVar) return RuntimeValue.NullValue

        val customUnit = currentEnvironment.getUnit(expr.name)
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

        return RuntimeValue.NullValue
    }

    override fun visitAssignExpr(expr: AssignExpr): RuntimeValue {
        val value = expr.value.accept(this)

        if (!currentEnvironment.assignVar(expr.name, value)) {
            currentEnvironment.putVar(expr.name, value)
        }

        return value
    }

    override fun visitStringLiteralExpr(expr: StringLiteralExpr): RuntimeValue {
        return StringValue(expr.value)
    }

    override fun visitCallExpr(expr: CallExpr): RuntimeValue {
        val callee = expr.callee.accept(this)
        val args = expr.arguments.map { it.accept(this) }

        val (name, callable) = when (callee) {
            is RuntimeValue.NativeFunction -> callee.name to callee.callable
            is RuntimeValue.Formula -> callee.name to callee
            else -> throw RuntimeException("Value is not a function or formula and cannot be called")
        }

        if (args.size != callable.arity()) {
            throw RuntimeException("'$name' expects ${callable.arity()} arguments, but provided ${args.size}")
        }

        return callable.call(this, args)
    }

    /**
     * Runs [block] with [environment] as the active scope, restoring the previous scope afterwards.
     */
    fun <T> withEnvironment(environment: Environment, block: () -> T): T {
        val previousEnvironment = currentEnvironment
        currentEnvironment = environment
        try {
            return block()
        } finally {
            currentEnvironment = previousEnvironment
        }
    }

    override fun visitConversionExpr(expr: ConversionExpr): RuntimeValue {
        val left = expr.left.accept(this) as PhysicsValue
        val unitName = expr.unitToken.value
        val targetUnit = currentEnvironment.getUnit(unitName)!!

        val targetScale = targetUnit.value * targetUnit.scale
        val convertedValue = left.scaledValue / targetScale

        return PhysicsValue(
            value = convertedValue,
            dimensions = left.dimensions,
            scale = targetScale,
            unitName = unitName
        )
    }

    override fun visitIfExpr(expr: IfExpr): RuntimeValue {
        val branch = if (evaluateCondition(expr.condition)) expr.thenBranch else expr.elseBranch

        return branch?.let { evaluateBranch(it, expr.isExpression) } ?: RuntimeValue.NullValue
    }

    // Relative tolerance for '==' and '!=' without 'within', absorbs floating point rounding
    private val equalityRelativeTolerance = 1e-9

    private fun evaluateCondition(condition: Condition): Boolean {
        return when (condition) {
            is Condition.Comparison -> {
                val operator = condition.operatorToken.value
                val left = condition.left.accept(this).asQuantity(operator).scaledValue
                val right = condition.right.accept(this).asQuantity(operator).scaledValue

                when (condition.operator) {
                    TokenType.Less -> left < right
                    TokenType.LessEqual -> left <= right
                    TokenType.Greater -> left > right
                    TokenType.GreaterEqual -> left >= right
                    TokenType.Equals -> quantitiesEqual(left, right, condition.tolerance)
                    TokenType.NotEqual -> !quantitiesEqual(left, right, condition.tolerance)
                    else -> throw RuntimeException("Unsupported comparison operator '$operator'")
                }
            }
            is Condition.Between -> {
                val value = condition.value.accept(this).asQuantity("between")
                val lower = condition.lower.accept(this).asQuantity("between")
                val upper = condition.upper.accept(this).asQuantity("between")

                if (lower.scaledValue > upper.scaledValue) {
                    throw RuntimeException("Invalid range: lower bound $lower is greater than upper bound $upper")
                }

                value.scaledValue >= lower.scaledValue && value.scaledValue <= upper.scaledValue
            }
            is Condition.DimensionCheck -> {
                val value = condition.value.accept(this).asQuantity("is")
                val target = condition.target.accept(this).asQuantity("is")

                value.dimensions.contentEquals(target.dimensions)
            }
        }
    }

    private fun quantitiesEqual(left: Double, right: Double, tolerance: Expr?): Boolean {
        if (tolerance == null) {
            if (left == right) return true
            if (left.isInfinite() || right.isInfinite()) return false

            return abs(left - right) <= equalityRelativeTolerance * max(abs(left), abs(right))
        }

        val margin = tolerance.accept(this).asQuantity("within")
        if (margin.scaledValue < 0) {
            throw RuntimeException("Tolerance after 'within' must not be negative, got $margin")
        }

        return abs(left - right) <= margin.scaledValue
    }

    /**
     * Statement blocks run like a normal block, expression blocks return their last expression.
     */
    private fun evaluateBranch(branch: IfBranch, isExpression: Boolean): RuntimeValue {
        return when (branch) {
            is IfBranch.ExprBranch -> branch.expr.accept(this)
            is IfBranch.BlockBranch -> {
                if (!isExpression) {
                    branch.block.accept(this)
                    return RuntimeValue.NullValue
                }

                withEnvironment(Environment(currentEnvironment)) {
                    val statements = branch.block.statements
                    statements.dropLast(1).forEach { it.accept(this) }

                    when (val last = statements.lastOrNull()) {
                        is ExprStmt -> last.expr?.accept(this) ?: RuntimeValue.NullValue
                        null -> RuntimeValue.NullValue
                        else -> {
                            last.accept(this)
                            RuntimeValue.NullValue
                        }
                    }
                }
            }
        }
    }

    private fun RuntimeValue.asQuantity(operator: String): PhysicsValue {
        return this as? PhysicsValue
            ?: throw RuntimeException("Operator '$operator' requires physics quantities, got $this")
    }

    override fun visitExprStmt(exprStmt: ExprStmt) {
        val value = exprStmt.expr?.accept(this)

        if (exprStmt.expr is IdentifierExpr) {
            println("$value")
        }
    }

    override fun visitVarDeclStmt(varDeclStmt: VarDeclStmt) {
        val value = varDeclStmt.initializer?.accept(this)

        currentEnvironment.putVar(varDeclStmt.name, value)
    }

    override fun visitPrintStmt(printStmt: PrintStmt) {
        val value = printStmt.expr?.accept(this)

        println(value)
    }

    override fun visitUnitDeclStmt(unitDeclStmt: UnitDeclStmt) {
        val value = unitDeclStmt.initializer.accept(this)

        if (value is PhysicsValue) currentEnvironment.putUnit(unitDeclStmt.name, value)
    }

    override fun visitBlockStmt(blockStmt: BlockStmt) {

        val previousEnvironment = currentEnvironment

        try {
            currentEnvironment = Environment(previousEnvironment)

            for (stmt in blockStmt.statements) {
                stmt.accept(this)
            }
        } finally {
            currentEnvironment = previousEnvironment
        }
    }

    override fun visitFormDeclStmt(formDeclStmt: FormDeclStmt) {
        currentEnvironment.putVar(
            formDeclStmt.name,
            RuntimeValue.Formula(formDeclStmt.name, formDeclStmt.parameterNames, formDeclStmt.body, currentEnvironment)
        )
    }
}
