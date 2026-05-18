package lang.core

import lang.ast.PhysicsValue
import lang.ast.RuntimeValue
import lang.ast.visitors.PhysicsEvaluator
import kotlin.math.sqrt

class SIFunction : Callable {
    override fun arity(): Int {
        return 1 // The SI() function only takes exactly 1 argument
    }

    override fun call(evaluator: PhysicsEvaluator, arguments: List<RuntimeValue>): RuntimeValue {
        val argument = arguments[0] as PhysicsValue

        return PhysicsValue(
            value = argument.scaledValue, // Multiply the raw value by its internal scale
            dimensions = argument.dimensions, // Keep the dimensions intact
            scale = 1.0, // Reset the scale to standard SI (1.0)
            unitName = "" // Clear the custom name so toString() generates the SI derived units
        )
    }
}

class SqrtFunction : Callable {
    override fun arity(): Int {
        return 1
    }

    override fun call(evaluator: PhysicsEvaluator, arguments: List<RuntimeValue>): RuntimeValue {
        val argument = arguments[0] as PhysicsValue

        return PhysicsValue(
            value = sqrt(argument.scaledValue),
            dimensions = argument.dimensions.map { it / 2 }.toIntArray(),
            scale = 1.0,
            unitName = ""
        )
    }
}
