package lang.core

import lang.ast.PhysicsValue
import lang.ast.RuntimeValue
import lang.ast.visitors.PhysicsEvaluator

class SIFunction : Callable {
    override fun arity(): Int {
        return 1 // The SI() function only takes exactly 1 argument
    }

    override fun call(evaluator: PhysicsEvaluator, arguments: List<RuntimeValue>): RuntimeValue {
        val argument = arguments[0]

        if (argument !is PhysicsValue) {
            throw Exception("Type Error: The 'SI()' function only accepts physical quantities, not strings or raw numbers.")
        }

        // Unpack and normalize to base SI units
        return PhysicsValue(
            value = argument.scaledValue, // Multiply the raw value by its internal scale
            dimensions = argument.dimensions, // Keep the dimensions intact
            scale = 1.0, // Reset the scale to standard SI (1.0)
            unitName = "" // Clear the custom name so toString() generates the SI derived units
        )
    }
}