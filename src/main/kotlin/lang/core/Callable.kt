package lang.core

import lang.ast.RuntimeValue
import lang.ast.visitors.PhysicsEvaluator

// Interface universal untuk semua fungsi
interface Callable {
    // Defines how many arguments this function expects
    fun arity(): Int

    // Executes the function logic
    fun call(evaluator: PhysicsEvaluator, arguments: List<RuntimeValue>): RuntimeValue
}

