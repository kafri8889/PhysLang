package lang.ast

import lang.core.Callable

/**
 * Data type for runtime
 */
sealed interface RuntimeValue {
    data class NativeFunction(val name: String, val callable: Callable) : RuntimeValue
}

data class PhysicsValue(
    val value: Double,
    val dimensions: IntArray,
    val scale: Double = 1.0,
    val unitName: String? = null
): RuntimeValue {
    val scaledValue: Double
        get() = value * scale

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhysicsValue

        if (value != other.value) return false
        if (!dimensions.contentEquals(other.dimensions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + dimensions.contentHashCode()
        return result
    }

    fun dimensionsToString(): String {
        val symbols = listOf("kg", "m", "s", "A", "K", "mol", "cd")

        val parts = mutableListOf<String>()

        for (i in dimensions.indices) {
            val exp = dimensions[i]
            if (exp != 0) {
                if (exp == 1) {
                    parts.add(symbols[i])
                } else {
                    parts.add("${symbols[i]}^$exp")
                }
            }
        }

        return parts.joinToString("·")
    }

    override fun toString(): String {
        if (!unitName.isNullOrEmpty()) {
            return "$value $unitName"
        }

        val matchedUnit = unitRegistry.entries.find {
            it.value.dimensions.contentEquals(this.dimensions)
        }

        return if (matchedUnit != null) {
            val converted = value / matchedUnit.value.scale
            "$converted ${matchedUnit.key}"
        } else {
            "$value ${dimensionsToString()}"
        }
    }

    companion object {

        val emptyDimensions: IntArray
            get() = IntArray(7)

        // [M, L, T, I, Θ, N, J]
        // Mass, Length, Time, Current, Temperature, Amount, Luminous
        val unitRegistry = mapOf(

            // ======================
            // Base SI Units
            // ======================
            "kg"  to PhysicsValue(1.0, intArrayOf(1,0,0,0,0,0,0), 1.0),
            "m"   to PhysicsValue(1.0, intArrayOf(0,1,0,0,0,0,0), 1.0),
            "s"   to PhysicsValue(1.0, intArrayOf(0,0,1,0,0,0,0), 1.0),
            "A"   to PhysicsValue(1.0, intArrayOf(0,0,0,1,0,0,0), 1.0),
            "K"   to PhysicsValue(1.0, intArrayOf(0,0,0,0,1,0,0), 1.0),
            "mol" to PhysicsValue(1.0, intArrayOf(0,0,0,0,0,1,0), 1.0),
            "cd"  to PhysicsValue(1.0, intArrayOf(0,0,0,0,0,0,1), 1.0),

            // ======================
            // Mass
            // ======================
            "g"  to PhysicsValue(1.0, intArrayOf(1,0,0,0,0,0,0), 1e-3),
            "mg" to PhysicsValue(1.0, intArrayOf(1,0,0,0,0,0,0), 1e-6),
            "t"  to PhysicsValue(1.0, intArrayOf(1,0,0,0,0,0,0), 1000.0),

            // ======================
            // Length
            // ======================
            "km" to PhysicsValue(1.0, intArrayOf(0,1,0,0,0,0,0), 1000.0),
            "cm" to PhysicsValue(1.0, intArrayOf(0,1,0,0,0,0,0), 1e-2),
            "mm" to PhysicsValue(1.0, intArrayOf(0,1,0,0,0,0,0), 1e-3),
            "µm" to PhysicsValue(1.0, intArrayOf(0,1,0,0,0,0,0), 1e-6),
            "nm" to PhysicsValue(1.0, intArrayOf(0,1,0,0,0,0,0), 1e-9),

            // ======================
            // Time
            // ======================
            "ms"  to PhysicsValue(1.0, intArrayOf(0,0,1,0,0,0,0), 1e-3),
            "min" to PhysicsValue(1.0, intArrayOf(0,0,1,0,0,0,0), 60.0),
            "h"   to PhysicsValue(1.0, intArrayOf(0,0,1,0,0,0,0), 3600.0),

            // ======================
            // Frequency
            // ======================
            "Hz" to PhysicsValue(1.0, intArrayOf(0,0,-1,0,0,0,0), 1.0),

            // ======================
            // Mechanics
            // ======================
            "N" to PhysicsValue(1.0, intArrayOf(1,1,-2,0,0,0,0), 1.0),  // Newton
            "Pa" to PhysicsValue(1.0, intArrayOf(1,-1,-2,0,0,0,0), 1.0), // Pascal
            "J" to PhysicsValue(1.0, intArrayOf(1,2,-2,0,0,0,0), 1.0),  // Joule
            "W" to PhysicsValue(1.0, intArrayOf(1,2,-3,0,0,0,0), 1.0),  // Watt

            // ======================
            // Electricity
            // ======================
            "C" to PhysicsValue(1.0, intArrayOf(0,0,1,1,0,0,0), 1.0),   // Coulomb
            "V" to PhysicsValue(1.0, intArrayOf(1,2,-3,-1,0,0,0), 1.0), // Volt
            "Ω" to PhysicsValue(1.0, intArrayOf(1,2,-3,-2,0,0,0), 1.0)  // Ohm
        )
    }
}

data class StringValue(val value: String) : RuntimeValue {
    override fun toString(): String = value
}