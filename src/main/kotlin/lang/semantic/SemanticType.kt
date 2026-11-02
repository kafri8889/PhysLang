package lang.semantic

import lang.ast.RuntimeValue

open class SemanticType {

    data object Unknown: SemanticType()
    data object String: SemanticType()
    data object Boolean: SemanticType()
    data object Null: SemanticType()
    class NativeFunction(val function: RuntimeValue.NativeFunction): SemanticType()
    class Dimensions(val dim: IntArray): SemanticType()
    class PhysicsValue(val value: lang.ast.PhysicsValue): SemanticType()

}

fun SemanticType.displayName(): String = when (this) {
    is SemanticType.PhysicsValue -> "physics quantity"
    is SemanticType.String -> "string"
    is SemanticType.Null -> "null"
    is SemanticType.NativeFunction -> "function"
    is SemanticType.Dimensions -> "dimensions"
    is SemanticType.Unknown -> "unknown value"
    else -> "expression"
}