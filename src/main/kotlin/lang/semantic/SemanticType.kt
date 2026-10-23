package lang.semantic

import lang.ast.RuntimeValue

open class SemanticType {

    data object Unknown: SemanticType()
    data object String: SemanticType()
    data object VariableExpr: SemanticType()
    data object AssignExpr: SemanticType()
    data object NullValue: SemanticType()
    class NativeFunction(val function: RuntimeValue.NativeFunction): SemanticType()
    class Dimensions(val dim: IntArray): SemanticType()
    class PhysicsValue(val value: lang.ast.PhysicsValue): SemanticType()

}