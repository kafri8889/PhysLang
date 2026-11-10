package lang.semantic

import lang.ast.*
import lang.core.Environment
import lang.error.Diagnostic
import lang.error.DiagnosticType
import lang.error.ErrorReporter
import lang.lexer.Token
import lang.lexer.TokenType

class SemanticAnalyzer(
    private val environment: Environment = Environment(),
    private val reporter: ErrorReporter = ErrorReporter()
): ExprVisitor<SemanticType>, StmtVisitor<Unit> {

    // Current active environment, when evaluator enter block expression
    // Change the environment to new environment (in block)
    private var currentEnvironment = environment

    private fun dimensionEquals(left: IntArray, right: IntArray): Boolean {
        return right.all { it == 0 } || left.contentEquals(right) // Scalar
    }

    override fun visitBinaryExpr(expr: BinaryExpr): SemanticType {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)

        if (left is SemanticType.Null || right is SemanticType.Null) {
            reporter.report(
                Diagnostic(
                    message = "Operator '${expr.operatorToken.value}' cannot be applied to null operand ().",
                    type = DiagnosticType.ERROR,
                    line = expr.operatorToken.line,
                    column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                )
            )

            return SemanticType.Unknown
        }

        return when (expr.operator) {
            TokenType.Plus,
            TokenType.Minus -> {
                if (left is SemanticType.PhysicsValue && right is SemanticType.PhysicsValue) {
                    if (!dimensionEquals(left.value.dimensions, right.value.dimensions)) {
                        reporter.report(
                            Diagnostic(
                                message = "Dimension mismatch: cannot operate different dimensions.",
                                type = DiagnosticType.ERROR,
                                line = expr.operatorToken.line,
                                column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                            )
                        )
                        return SemanticType.Unknown
                    }

                    return physicsType(left.value.dimensions)
                }

                if (left is SemanticType.String || right is SemanticType.String) {
                    if (expr.operator == TokenType.Plus) {
                        return SemanticType.String
                    }

                    reporter.report(
                        Diagnostic(
                            message = "Cannot use '-' with string",
                            type = DiagnosticType.ERROR,
                            line = expr.operatorToken.line,
                            column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                        )
                    )

                    return SemanticType.Unknown
                }

                if (left is SemanticType.Unknown || right is SemanticType.Unknown) {
                    return SemanticType.Unknown
                }

                reporter.report(
                    Diagnostic(
                        message = "Operator '${expr.operatorToken.value}' cannot be applied to operands of type '$left' and '$right'.",
                        type = DiagnosticType.ERROR,
                        line = expr.operatorToken.line,
                        column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                    )
                )

                return SemanticType.Unknown
            }
            TokenType.Multiply -> {
                if (left !is SemanticType.PhysicsValue || right !is SemanticType.PhysicsValue) {
                    reporter.report(
                        Diagnostic(
                            message = "The '*' operation can only be used for numbers or physical units!",
                            type = DiagnosticType.ERROR,
                            line = expr.operatorToken.line,
                            column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                        )
                    )
                    return SemanticType.Unknown
                }

                val newDimensions = IntArray(7) { i ->
                    left.value.dimensions[i] + right.value.dimensions[i]
                }

                physicsType(newDimensions)
            }
            TokenType.Divide -> {
                if (left !is SemanticType.PhysicsValue || right !is SemanticType.PhysicsValue) {
                    reporter.report(
                        Diagnostic(
                            message = "The '/' operation can only be used for numbers or physical units!",
                            type = DiagnosticType.ERROR,
                            line = expr.operatorToken.line,
                            column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                        )
                    )
                    return SemanticType.Unknown
                }

                val newDimensions = IntArray(7) { i ->
                    left.value.dimensions[i] - right.value.dimensions[i]
                }

                physicsType(newDimensions)
            }
            TokenType.Power -> {
                if (left !is SemanticType.PhysicsValue || right !is SemanticType.PhysicsValue) {
                    reporter.report(
                        Diagnostic(
                            message = "The '^' operation can only be used for numbers or physical units!",
                            type = DiagnosticType.ERROR,
                            line = expr.operatorToken.line,
                            column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                        )
                    )

                    return SemanticType.Unknown
                }

                if (right.value.dimensions.any { it != 0 }) {
                    reporter.report(
                        Diagnostic(
                            message = "the '^' operation can only be used for numbers!",
                            type = DiagnosticType.ERROR,
                            line = expr.operatorToken.line,
                            column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                        )
                    )

                    return SemanticType.Unknown
                }

                if (right.value.scaledValue % 1.0 != 0.0) {
                    reporter.report(
                        Diagnostic(
                            message = "The '^' operation can only be used for integers!",
                            type = DiagnosticType.ERROR,
                            line = expr.operatorToken.line,
                            column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                        )
                    )

                    return SemanticType.Unknown
                }

                val exponent = right.value.scaledValue.toInt()
                val newDimensions = IntArray(7) { i ->
                    left.value.dimensions[i] * exponent
                }

                physicsType(newDimensions)
            }
            else -> {
                reporter.report(
                    Diagnostic(
                        message = "Operator invalid or not supported: ${expr.operator}",
                        type = DiagnosticType.ERROR,
                        line = expr.operatorToken.line,
                        column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                    )
                )

                SemanticType.Unknown
            }
        }
    }

    override fun visitUnaryExpr(expr: UnaryExpr): SemanticType {
        when (val right = expr.right.accept(this)) {
            is SemanticType.PhysicsValue -> return right
            else -> {
                val message = if (right is SemanticType.Null) {
                    "Operation (\"${expr.operator.name}\") can't be applied to null value!"
                } else "Operation (\"${expr.operator.name}\") can't be applied to ${right}!"

                reporter.report(
                    Diagnostic(
                        message = message,
                        type = DiagnosticType.ERROR,
                        line = expr.operatorToken.line,
                        column = expr.operatorToken.startColumn until expr.operatorToken.endColumn
                    )
                )

                return SemanticType.Unknown
            }
        }
    }

    override fun visitLiteralExpr(literal: LiteralExpr): SemanticType {
        val value = when (val literal = literal.value) {
            is LiteralValue.IntVal -> literal.value.toDouble()
            is LiteralValue.DoubleVal -> literal.value
            is LiteralValue.NullVal -> return SemanticType.Null
            else -> {
                reporter.report(
                    Diagnostic(
                        message = "Invalid literal: $literal",
                        type = DiagnosticType.ERROR,
                        line = literal.valueToken.line,
                        column = literal.valueToken.startColumn until literal.valueToken.endColumn
                    )
                )

                return SemanticType.Unknown
            }
        }

        return SemanticType.PhysicsValue(PhysicsValue.zero.copy(value = value))
    }

    override fun visitQuantityExpr(expr: QuantityExpr): SemanticType {
        val unitScale = currentEnvironment.resolveUnitNode(expr.unit)

        if (unitScale == null) {
            reporter.report(
                Diagnostic(
                    message = "Unknown physic unit: ${describeUnit(expr.unit)}",
                    type = DiagnosticType.ERROR,
                    line = expr.value.value.valueToken.line,
                    column = expr.value.value.valueToken.startColumn until expr.value.value.valueToken.endColumn
                )
            )

            return SemanticType.Unknown
        }

        val unitName = unitScale.unitName

        val physicsValue = expr.value.accept(this)

        if (physicsValue is SemanticType.String) {
            reporter.report(
                Diagnostic(
                    message = "Cannot construct physics value from string, use numbers!",
                    type = DiagnosticType.ERROR,
                    line = expr.value.value.valueToken.line,
                    column = expr.value.value.valueToken.startColumn until expr.value.value.valueToken.endColumn
                )
            )

            return SemanticType.Unknown
        }

        if (physicsValue is SemanticType.PhysicsValue) {
            return SemanticType.PhysicsValue(
                PhysicsValue(
                    value = 0.0,
                    dimensions = unitScale.dimensions,
                    scale = unitScale.scale,
                    unitName = unitName
                )
            )
        }

        reporter.report(
            Diagnostic(
                message = "Cannot construct physics value from $physicsValue, use numbers!",
                type = DiagnosticType.ERROR,
                line = expr.value.value.valueToken.line,
                column = expr.value.value.valueToken.startColumn until expr.value.value.valueToken.endColumn
            )
        )

        return SemanticType.Unknown
    }

    override fun visitIdentifierExpr(expr: IdentifierExpr): SemanticType {
        val value = currentEnvironment.getVar(expr.nameToken.value, true)
        if (value != null) return value.toSemanticType()

        val hasVar = currentEnvironment.hasVar(expr.nameToken.value)
        if (hasVar) return SemanticType.Null

        val customUnit = currentEnvironment.getUnit(expr.nameToken.value)
        if (customUnit is PhysicsValue) {
            return SemanticType.PhysicsValue(customUnit)
        }

        val builtinUnit = PhysicsValue.unitRegistry[expr.nameToken.value]
        if (builtinUnit != null) {
            return SemanticType.PhysicsValue(builtinUnit)
        }

        reporter.report(
            Diagnostic(
                message = "Variable or unit '${expr.nameToken.value}' is not defined!",
                type = DiagnosticType.ERROR,
                line = expr.nameToken.line,
                column = expr.nameToken.startColumn until expr.nameToken.endColumn
            )
        )

        return SemanticType.Unknown
    }

    override fun visitAssignExpr(expr: AssignExpr): SemanticType {
        val value = expr.value.accept(this)

        if (!currentEnvironment.hasVar(expr.nameToken.value)) {
            if (value !is SemanticType.Unknown) {
                when (value) {
                    is SemanticType.PhysicsValue -> currentEnvironment.putVar(expr.nameToken.value, value.value)
                    is SemanticType.String -> currentEnvironment.putVar(expr.nameToken.value, StringValue(""))
                    is SemanticType.Null -> currentEnvironment.putVar(expr.nameToken.value, null)
                    else -> currentEnvironment.putVar(expr.nameToken.value, null)
                }

                return value
            }

            reporter.report(
                Diagnostic(
                    message = "Variable '${expr.nameToken.value}' is not defined!",
                    type = DiagnosticType.ERROR,
                    line = expr.nameToken.line,
                    column = expr.nameToken.startColumn until expr.nameToken.endColumn
                )
            )

            return SemanticType.Unknown
        }

        when (value) {
            is SemanticType.PhysicsValue -> currentEnvironment.putVar(expr.nameToken.value, value.value)
            is SemanticType.String -> currentEnvironment.putVar(expr.nameToken.value, StringValue(""))
            is SemanticType.Null -> currentEnvironment.putVar(expr.nameToken.value, null)
            else -> currentEnvironment.putVar(expr.nameToken.value, null)
        }

        return value
    }

    override fun visitStringLiteralExpr(expr: StringLiteralExpr): SemanticType {
        return SemanticType.String
    }

    override fun visitCallExpr(expr: CallExpr): SemanticType {
        val callee = expr.callee.accept(this)
        val args = expr.arguments.map { it.accept(this) }

        if (callee is SemanticType.Unknown) {
            // The undefined callee has already been reported.
            return SemanticType.Unknown
        }

        if (callee is SemanticType.Formula) {
            return analyzeFormulaCall(callee.formula, args, expr)
        }

        if (callee !is SemanticType.NativeFunction) {
            reporter.report(
                Diagnostic(
                    message = "Error: This variable is not a function and cannot be called!",
                    type = DiagnosticType.ERROR,
                    line = expr.parenToken.line,
                    column = expr.parenToken.startColumn until expr.parenToken.endColumn
                )
            )

            return SemanticType.Unknown
        }

        if (args.size != callee.function.callable.arity()) {
            reporter.report(
                Diagnostic(
                    message = "Error: Function '${callee.function.name}' expects ${callee.function.callable.arity()} arguments, but you provided ${args.size}.",
                    type = DiagnosticType.ERROR,
                    line = expr.parenToken.line,
                    column = expr.parenToken.startColumn until expr.parenToken.endColumn
                )
            )

            return SemanticType.Unknown
        }

        if (callee.function.name == "SI") {
            if (args.firstOrNull() !is SemanticType.PhysicsValue) {
                reporter.report(
                    Diagnostic(
                        message = "Type Error: The 'SI()' function only accepts physical quantities, not strings or raw numbers.",
                        type = DiagnosticType.ERROR,
                        line = expr.parenToken.line,
                        column = expr.parenToken.startColumn until expr.parenToken.endColumn
                    )
                )

                return SemanticType.Unknown
            }

            return args[0]
        }

        if (callee.function.name == "sqrt") {
            val argument = args.firstOrNull()

            if (argument is SemanticType.Unknown) {
                return SemanticType.Unknown
            }

            if (argument !is SemanticType.PhysicsValue) {
                reporter.report(
                    Diagnostic(
                        message = "Type Error: The 'sqrt()' function only accepts physical quantities, got ${argument?.displayName()}.",
                        type = DiagnosticType.ERROR,
                        line = expr.parenToken.line,
                        column = expr.parenToken.startColumn until expr.parenToken.endColumn
                    )
                )

                return SemanticType.Unknown
            }

            // The square root halves every dimension power, so each power must be even (e.g. m^2 -> m, but not m^3)
            if (argument.value.dimensions.any { it % 2 != 0 }) {
                reporter.report(
                    Diagnostic(
                        message = "Dimension Error: 'sqrt()' requires even dimension powers, got '${argument.value.dimensionsToString()}'.",
                        type = DiagnosticType.ERROR,
                        line = expr.parenToken.line,
                        column = expr.parenToken.startColumn until expr.parenToken.endColumn
                    )
                )

                return SemanticType.Unknown
            }

            return physicsType(IntArray(7) { i -> argument.value.dimensions[i] / 2 })
        }

        return SemanticType.NativeFunction(callee.function)
    }

    override fun visitConversionExpr(expr: ConversionExpr): SemanticType {
        val left = expr.left.accept(this)
        val unitName = expr.unitToken.value

        if (currentEnvironment.hasUnit(unitName)) {
            val targetUnit = currentEnvironment.getUnit(unitName)!!

            if (left is SemanticType.PhysicsValue) {
                if (targetUnit.dimensions.contentEquals(left.value.dimensions)) {
                    return SemanticType.PhysicsValue(left.value)
                }

                // Dimensi tidak cocok
                val sourceUnitName = currentEnvironment.getUnitByDimensions(left.value.dimensions)?.first
                    ?: left.value.dimensionsToString()

                reporter.report(
                    Diagnostic(
                        message = "Cannot convert '$sourceUnitName' to '$unitName' (dimensional mismatch)!",
                        type = DiagnosticType.ERROR,
                        line = expr.unitToken.line,
                        column = expr.unitToken.startColumn until expr.unitToken.endColumn
                    )
                )

                return SemanticType.Unknown
            }

            // Kalau left bukan PhysicsValue (misal String, Function, Null, dsb)
            if (left !is SemanticType.Unknown) {
                reporter.report(
                    Diagnostic(
                        message = "Cannot convert ${left.displayName()} to unit '$unitName'. Operator 'in' requires a physics quantity on the left side.",
                        type = DiagnosticType.ERROR,
                        line = expr.unitToken.line,
                        column = expr.unitToken.startColumn until expr.unitToken.endColumn
                    )
                )
            }

            return SemanticType.Unknown
        }

        reporter.report(
            Diagnostic(
                message = "Unit $unitName is not defined!",
                type = DiagnosticType.ERROR,
                line = expr.unitToken.line,
                column = expr.unitToken.startColumn until expr.unitToken.endColumn
            )
        )

        return SemanticType.Unknown
    }

    override fun visitIfExpr(expr: IfExpr): SemanticType {
        val staticResult = analyzeCondition(expr.condition)

        if (staticResult != null) {
            // Dimensional narrowing: the dimensions are known statically, so only the selected branch is analyzed
            val selectedBranch = if (staticResult) expr.thenBranch else expr.elseBranch
            val selectedType = selectedBranch?.let { analyzeBranch(it, expr) }

            return if (expr.isExpression) selectedType ?: SemanticType.Unknown else SemanticType.Unknown
        }

        // Every branch is analyzed, even the one that will not be executed
        val thenType = analyzeBranch(expr.thenBranch, expr)
        val elseType = expr.elseBranch?.let { analyzeBranch(it, expr) }

        if (!expr.isExpression) {
            return SemanticType.Unknown
        }

        return unifyBranchTypes(thenType, elseType ?: SemanticType.Unknown, expr.ifToken)
    }

    /**
     * Validates the physical relation of an if condition.
     *
     * @return The static result of an `is` check, or null when the condition is only known at runtime
     */
    private fun analyzeCondition(condition: Condition): Boolean? {
        when (condition) {
            is Condition.Comparison -> {
                val operatorToken = condition.operatorToken
                val operands = requireQuantities(
                    listOf(condition.left.accept(this), condition.right.accept(this)),
                    operatorToken
                )

                if (operands != null) {
                    val (left, right) = operands

                    if (!left.dimensions.contentEquals(right.dimensions)) {
                        reportAt(
                            operatorToken,
                            "Dimension mismatch in comparison: cannot compare '${dimensionName(left.dimensions)}' with '${dimensionName(right.dimensions)}'."
                        )
                    }
                }

                val toleranceExpr = condition.tolerance ?: return null
                val tolerance = requireQuantities(listOf(toleranceExpr.accept(this)), operatorToken, "within")
                    ?.first()
                    ?: return null

                val expected = operands?.first() ?: return null
                if (!tolerance.dimensions.contentEquals(expected.dimensions)) {
                    reportAt(
                        operatorToken,
                        "Tolerance dimension mismatch: 'within' expects '${dimensionName(expected.dimensions)}', got '${dimensionName(tolerance.dimensions)}'."
                    )
                }

                return null
            }
            is Condition.Between -> {
                val operands = requireQuantities(
                    listOf(condition.value.accept(this), condition.lower.accept(this), condition.upper.accept(this)),
                    condition.betweenToken
                ) ?: return null

                val (value, lower, upper) = operands

                if (!lower.dimensions.contentEquals(value.dimensions) || !upper.dimensions.contentEquals(value.dimensions)) {
                    reportAt(
                        condition.betweenToken,
                        "Dimension mismatch in range: cannot check '${dimensionName(value.dimensions)}' between '${dimensionName(lower.dimensions)}' and '${dimensionName(upper.dimensions)}'."
                    )
                }

                return null
            }
            is Condition.DimensionCheck -> {
                val value = condition.value.accept(this)

                if (!isUnitExpression(condition.target)) {
                    reportAt(
                        condition.isToken,
                        "The right side of 'is' must be a unit expression such as N, m/s or kg*m/s^2."
                    )
                    return null
                }

                val target = condition.target.accept(this)

                val operands = requireQuantities(listOf(value, target), condition.isToken) ?: return null

                return operands[0].dimensions.contentEquals(operands[1].dimensions)
            }
        }
    }

    /**
     * Ensures every operand of a condition is a physics quantity.
     *
     * @return The quantities, or null when an operand is invalid or already reported as unknown
     */
    private fun requireQuantities(
        operands: List<SemanticType>,
        operatorToken: Token,
        operatorName: String = operatorToken.value
    ): List<PhysicsValue>? {
        if (operands.any { it is SemanticType.Null }) {
            reportAt(operatorToken, "Operator '$operatorName' cannot be applied to null operand.")
            return null
        }

        if (operands.any { it is SemanticType.Unknown }) {
            return null
        }

        if (operands.any { it !is SemanticType.PhysicsValue }) {
            reportAt(
                operatorToken,
                "Comparison '$operatorName' requires physics quantities, got ${operands.joinToString(" and ") { it.displayName() }}."
            )
            return null
        }

        return operands.map { (it as SemanticType.PhysicsValue).value }
    }

    /**
     * A pure unit expression only contains units combined with `*`, `/` and integer `^`, e.g. `kg*m/s^2`.
     */
    private fun isUnitExpression(expr: Expr): Boolean {
        return when (expr) {
            is IdentifierExpr -> !currentEnvironment.hasVar(expr.name) && currentEnvironment.hasUnit(expr.name)
            is LiteralExpr -> (expr.value as? LiteralValue.IntVal)?.value == 1 // `1` is dimensionless, as in 1/s
            is BinaryExpr -> when (expr.operator) {
                TokenType.Multiply,
                TokenType.Divide -> isUnitExpression(expr.left) && isUnitExpression(expr.right)
                TokenType.Power -> isUnitExpression(expr.left) && isIntegerLiteral(expr.right)
                else -> false
            }
            else -> false
        }
    }

    private fun isIntegerLiteral(expr: Expr): Boolean {
        return when (expr) {
            is LiteralExpr -> expr.value is LiteralValue.IntVal
            is UnaryExpr -> expr.operator == TokenType.Minus && isIntegerLiteral(expr.right)
            else -> false
        }
    }

    /**
     * Analyzes a branch in its own scope, the type of a block is the type of its last expression.
     */
    private fun analyzeBranch(branch: IfBranch, expr: IfExpr): SemanticType {
        return when (branch) {
            is IfBranch.ExprBranch -> branch.expr.accept(this)
            is IfBranch.BlockBranch -> {
                val previousEnvironment = currentEnvironment

                try {
                    currentEnvironment = Environment(previousEnvironment)

                    var lastType: SemanticType? = null

                    for (stmt in branch.block.statements) {
                        lastType = if (stmt is ExprStmt) {
                            stmt.expr?.accept(this)
                        } else {
                            stmt.accept(this)
                            null
                        }
                    }

                    if (expr.isExpression && lastType == null) {
                        reportAt(expr.ifToken, "A block used as an if value must end with an expression.")
                        return SemanticType.Unknown
                    }

                    lastType ?: SemanticType.Unknown
                } finally {
                    currentEnvironment = previousEnvironment
                }
            }
        }
    }

    /**
     * All branches of an if used as a value must produce the same type and dimensions.
     */
    private fun unifyBranchTypes(thenType: SemanticType, elseType: SemanticType, ifToken: Token): SemanticType {
        if (thenType is SemanticType.Unknown || elseType is SemanticType.Unknown) return SemanticType.Unknown
        if (thenType is SemanticType.Null) return elseType
        if (elseType is SemanticType.Null) return thenType

        if (thenType is SemanticType.PhysicsValue && elseType is SemanticType.PhysicsValue) {
            if (thenType.value.dimensions.contentEquals(elseType.value.dimensions)) {
                return thenType
            }

            reportAt(
                ifToken,
                "Dimension mismatch between if branches: '${dimensionName(thenType.value.dimensions)}' vs '${dimensionName(elseType.value.dimensions)}'."
            )
            return SemanticType.Unknown
        }

        if (thenType is SemanticType.String && elseType is SemanticType.String) {
            return SemanticType.String
        }

        reportAt(ifToken, "Incompatible if branch types: ${thenType.displayName()} and ${elseType.displayName()}.")
        return SemanticType.Unknown
    }

    private fun dimensionName(dimensions: IntArray): String {
        if (dimensions.all { it == 0 }) return "dimensionless"

        return currentEnvironment.getUnitByDimensions(dimensions)?.first
            ?: PhysicsValue(0.0, dimensions).dimensionsToString()
    }

    private fun reportAt(token: Token, message: String) {
        reporter.report(
            Diagnostic(
                message = message,
                type = DiagnosticType.ERROR,
                line = token.line,
                column = token.startColumn until token.endColumn
            )
        )
    }

    override fun visitExprStmt(exprStmt: ExprStmt) {
        exprStmt.expr?.accept(this)
    }

    override fun visitVarDeclStmt(varDeclStmt: VarDeclStmt) {
        if (currentEnvironment.getVar(varDeclStmt.name) != null) {
            reporter.report(
                Diagnostic(
                    message = "Conflicting declaration: ${varDeclStmt.name} already defined!",
                    type = DiagnosticType.ERROR,
                    line = varDeclStmt.nameToken.line,
                    column = varDeclStmt.nameToken.startColumn until varDeclStmt.nameToken.endColumn
                )
            )

            return
        }

        val exprSemanticType = varDeclStmt.initializer?.accept(this)

        if (exprSemanticType is SemanticType.Unknown) {
            return
        }

        when (exprSemanticType) {
            is SemanticType.PhysicsValue -> currentEnvironment.putVar(varDeclStmt.name, exprSemanticType.value)
            is SemanticType.Null, null -> currentEnvironment.putVar(varDeclStmt.name, null)
            else -> currentEnvironment.putVar(varDeclStmt.name, null)
        }
    }

    override fun visitPrintStmt(printStmt: PrintStmt) {
        printStmt.expr?.accept(this)
    }

    override fun visitUnitDeclStmt(unitDeclStmt: UnitDeclStmt) {
        val value = unitDeclStmt.initializer.accept(this)

        if (value is SemanticType.String) {
            reporter.report(
                Diagnostic(
                    message = "Cant create new unit from string!",
                    type = DiagnosticType.ERROR,
                    line = unitDeclStmt.nameToken.line,
                    column = unitDeclStmt.nameToken.startColumn until unitDeclStmt.nameToken.endColumn
                )
            )
            return
        }

        if (value is SemanticType.PhysicsValue) {
            currentEnvironment.putUnit(unitDeclStmt.nameToken.value, value.value)
        }
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
        if (currentEnvironment.getVar(formDeclStmt.name, false) != null) {
            reporter.report(
                Diagnostic(
                    message = "Conflicting declaration: ${formDeclStmt.name} already defined!",
                    type = DiagnosticType.ERROR,
                    line = formDeclStmt.nameToken.line,
                    column = formDeclStmt.nameToken.startColumn until formDeclStmt.nameToken.endColumn
                )
            )
            return
        }

        // The body is type-checked per call site, using the actual argument types.
        currentEnvironment.putVar(
            formDeclStmt.name,
            RuntimeValue.Formula(formDeclStmt.name, formDeclStmt.parameterNames, formDeclStmt.body, currentEnvironment)
        )
    }

    private val formulasInAnalysis = mutableSetOf<RuntimeValue.Formula>()

    /**
     * Infers the result type of a formula call by analyzing its body with the
     * parameters bound to the argument types.
     */
    private fun analyzeFormulaCall(
        formula: RuntimeValue.Formula,
        args: List<SemanticType>,
        expr: CallExpr
    ): SemanticType {
        fun error(message: String): SemanticType {
            reporter.report(
                Diagnostic(
                    message = message,
                    type = DiagnosticType.ERROR,
                    line = expr.parenToken.line,
                    column = expr.parenToken.startColumn until expr.parenToken.endColumn
                )
            )
            return SemanticType.Unknown
        }

        if (args.size != formula.arity()) {
            return error("Formula '${formula.name}' expects ${formula.arity()} arguments, but you provided ${args.size}.")
        }

        if (formula in formulasInAnalysis) {
            return error("Formula '${formula.name}' is recursive; recursive formulas are not supported.")
        }

        val local = Environment(formula.closure)
        formula.parameters.forEachIndexed { i, parameter ->
            val placeholder: RuntimeValue? = when (val arg = args[i]) {
                is SemanticType.PhysicsValue -> arg.value
                is SemanticType.String -> StringValue("")
                is SemanticType.Formula -> arg.formula
                is SemanticType.NativeFunction -> arg.function
                is SemanticType.Null -> null
                else -> return SemanticType.Unknown // already reported
            }
            local.putVar(parameter, placeholder)
        }

        val previousEnvironment = currentEnvironment
        formulasInAnalysis.add(formula)
        try {
            currentEnvironment = local
            return formula.body.accept(this)
        } finally {
            currentEnvironment = previousEnvironment
            formulasInAnalysis.remove(formula)
        }
    }

    private fun describeUnit(unit: UnitNode): kotlin.String = when (unit) {
        is UnitNode.BaseUnit -> unit.name
        is UnitNode.PowerUnit -> "${describeUnit(unit.base)}^${unit.exponent}"
        is UnitNode.CompositeUnit -> "${describeUnit(unit.left)}${if (unit.operator == TokenType.Divide) "/" else "*"}${describeUnit(unit.right)}"
    }

    private fun physicsType(dimensions: IntArray): SemanticType.PhysicsValue {
        return SemanticType.PhysicsValue(
            PhysicsValue(
                value = 0.0,
                dimensions = dimensions
            )
        )
    }

    private fun RuntimeValue.toSemanticType(): SemanticType {
        return when (this) {
            is PhysicsValue -> SemanticType.PhysicsValue(this)
            is StringValue -> SemanticType.String
            is RuntimeValue.NativeFunction -> SemanticType.NativeFunction(this)
            is RuntimeValue.Formula -> SemanticType.Formula(this)
            is RuntimeValue.NullValue -> SemanticType.Null
        }
    }
}
