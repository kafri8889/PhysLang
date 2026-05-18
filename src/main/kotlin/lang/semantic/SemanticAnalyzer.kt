package lang.semantic

import lang.ast.*
import lang.core.Environment
import lang.error.Diagnostic
import lang.error.DiagnosticType
import lang.error.ErrorReporter
import lang.lexer.TokenType

class SemanticAnalyzer(
    private val environment: Environment = Environment(),
    private val reporter: ErrorReporter = ErrorReporter()
): ExprVisitor<SemanticType>, StmtVisitor<Unit> {

    private fun dimensionEquals(left: IntArray, right: IntArray): Boolean {
        return if (right.all { it == 0 }) true // Scalar
        else left.contentEquals(right)
    }

    override fun visitBinaryExpr(expr: BinaryExpr): SemanticType {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)

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

                SemanticType.Unknown
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
        val right = expr.right.accept(this)

        when (right) {
            is SemanticType.PhysicsValue -> return right
            else -> {
                reporter.report(
                    Diagnostic(
                        message = "Operation (\"${expr.operator}\") can't be applied to ${right}!",
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
        val unitName = when (val unit = expr.unit) {
            is UnitNode.BaseUnit -> unit.name
            else -> ""
        }

        var unitScale = environment.getUnit(unitName)

        if (unitScale == null) {
            unitScale = PhysicsValue.unitRegistry[unitName]
        }

        if (unitScale == null) {
            reporter.report(
                Diagnostic(
                    message = "Unknown physic unit: $unitName",
                    type = DiagnosticType.ERROR,
                    line = expr.value.value.valueToken.line,
                    column = expr.value.value.valueToken.startColumn until expr.value.value.valueToken.endColumn
                )
            )

            return SemanticType.Unknown
        }

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

    override fun visitVariableExpr(expr: VariableExpr): SemanticType {
        val value = environment.getVar(expr.nameToken.value)
        if (value != null) return value.toSemanticType()

        val customUnit = environment.getUnit(expr.nameToken.value)
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
        expr.value.accept(this)

        if (environment.getVar(expr.nameToken.value) == null) {
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

        return SemanticType.AssignExpr
    }

    override fun visitStringLiteralExpr(expr: StringLiteralExpr): SemanticType {
        return SemanticType.String
    }

    override fun visitCallExpr(expr: CallExpr): SemanticType {
        val callee = expr.callee.accept(this)
        val args = expr.arguments.map { it.accept(this) }

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

        // todo: check for sqrt func

        return SemanticType.NativeFunction(callee.function)
    }

    override fun visitExprStmt(exprStmt: ExprStmt) {
        exprStmt.expr?.accept(this)
    }

    override fun visitVarDeclStmt(varDeclStmt: VarDeclStmt) {
        if (environment.getVar(varDeclStmt.name) != null) {
            reporter.report(
                Diagnostic(
                    message = "Conflicting declaration: ${varDeclStmt.name} already defined!",
                    type = DiagnosticType.ERROR,
                    line = varDeclStmt.nameToken.line,
                    column = varDeclStmt.nameToken.startColumn until varDeclStmt.nameToken.endColumn
                )
            )
        }

        varDeclStmt.initializer?.accept(this)
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
        }

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
            else -> SemanticType.Unknown
        }
    }
}
