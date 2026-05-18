package lang.semantic

import lang.ast.RuntimeValue

open class SemanticType {

    object Unknown: SemanticType()
    object String: SemanticType()
    object VariableExpr: SemanticType()
    object AssignExpr: SemanticType()
    class NativeFunction(val function: RuntimeValue.NativeFunction): SemanticType()
    class Dimensions(val dim: IntArray): SemanticType()
    class PhysicsValue(val value: lang.ast.PhysicsValue): SemanticType()

}