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

        return parts.joinToString("*")
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

        val zero: PhysicsValue
            get() = PhysicsValue(0.0, emptyDimensions)

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
            "N" to PhysicsValue(1.0, intArrayOf(1,1,-2,0,0,0,0), 1.0),   // Newton
            "Pa" to PhysicsValue(1.0, intArrayOf(1,-1,-2,0,0,0,0), 1.0), // Pascal
            "J" to PhysicsValue(1.0, intArrayOf(1,2,-2,0,0,0,0), 1.0),   // Joule
            "W" to PhysicsValue(1.0, intArrayOf(1,2,-3,0,0,0,0), 1.0),   // Watt

            "kN" to PhysicsValue(1.0, intArrayOf(1,1,-2,0,0,0,0), 1000.0),
            "kPa" to PhysicsValue(1.0, intArrayOf(1,-1,-2,0,0,0,0), 1000.0),
            "MPa" to PhysicsValue(1.0, intArrayOf(1,-1,-2,0,0,0,0), 1e6),
            "bar" to PhysicsValue(1.0, intArrayOf(1,-1,-2,0,0,0,0), 100000.0),
            "atm" to PhysicsValue(1.0, intArrayOf(1,-1,-2,0,0,0,0), 101325.0),

            "kJ" to PhysicsValue(1.0, intArrayOf(1,2,-2,0,0,0,0), 1000.0),
            "MJ" to PhysicsValue(1.0, intArrayOf(1,2,-2,0,0,0,0), 1e6),
            "cal" to PhysicsValue(1.0, intArrayOf(1,2,-2,0,0,0,0), 4.184),
            "kcal" to PhysicsValue(1.0, intArrayOf(1,2,-2,0,0,0,0), 4184.0),
            "eV" to PhysicsValue(1.0, intArrayOf(1,2,-2,0,0,0,0), 1.602176634e-19),
            "kWh" to PhysicsValue(1.0, intArrayOf(1,2,-2,0,0,0,0), 3.6e6),

            "kW" to PhysicsValue(1.0, intArrayOf(1,2,-3,0,0,0,0), 1000.0),
            "MW" to PhysicsValue(1.0, intArrayOf(1,2,-3,0,0,0,0), 1e6),
            "mW" to PhysicsValue(1.0, intArrayOf(1,2,-3,0,0,0,0), 1e-3),

            // ======================
            // Electromagnetism
            // ======================
            "C" to PhysicsValue(1.0, intArrayOf(0,0,1,1,0,0,0), 1.0),    // Coulomb
            "V" to PhysicsValue(1.0, intArrayOf(1,2,-3,-1,0,0,0), 1.0),  // Volt
            "Ω" to PhysicsValue(1.0, intArrayOf(1,2,-3,-2,0,0,0), 1.0),  // Ohm
            "ohm" to PhysicsValue(1.0, intArrayOf(1,2,-3,-2,0,0,0), 1.0), // Ohm alias
            "F" to PhysicsValue(1.0, intArrayOf(-1,-2,4,2,0,0,0), 1.0),  // Farad
            "H" to PhysicsValue(1.0, intArrayOf(1,2,-2,-2,0,0,0), 1.0),  // Henry
            "T" to PhysicsValue(1.0, intArrayOf(1,0,-2,-1,0,0,0), 1.0),  // Tesla
            "Wb" to PhysicsValue(1.0, intArrayOf(1,2,-2,-1,0,0,0), 1.0), // Weber
            "S" to PhysicsValue(1.0, intArrayOf(-1,-2,3,2,0,0,0), 1.0),  // Siemens

            "mA" to PhysicsValue(1.0, intArrayOf(0,0,0,1,0,0,0), 1e-3),
            "µA" to PhysicsValue(1.0, intArrayOf(0,0,0,1,0,0,0), 1e-6),
            "mV" to PhysicsValue(1.0, intArrayOf(1,2,-3,-1,0,0,0), 1e-3),
            "kV" to PhysicsValue(1.0, intArrayOf(1,2,-3,-1,0,0,0), 1000.0),
            "kΩ" to PhysicsValue(1.0, intArrayOf(1,2,-3,-2,0,0,0), 1000.0),
            "MΩ" to PhysicsValue(1.0, intArrayOf(1,2,-3,-2,0,0,0), 1e6),
            "µF" to PhysicsValue(1.0, intArrayOf(-1,-2,4,2,0,0,0), 1e-6),
            "nF" to PhysicsValue(1.0, intArrayOf(-1,-2,4,2,0,0,0), 1e-9),
            "pF" to PhysicsValue(1.0, intArrayOf(-1,-2,4,2,0,0,0), 1e-12),
            "mH" to PhysicsValue(1.0, intArrayOf(1,2,-2,-2,0,0,0), 1e-3),
            "µH" to PhysicsValue(1.0, intArrayOf(1,2,-2,-2,0,0,0), 1e-6),

            // ======================
            // Angle (Dimensionless)
            // ======================
            "rad" to PhysicsValue(1.0, emptyDimensions, 1.0),
            "deg" to PhysicsValue(1.0, emptyDimensions, Math.PI / 180.0)
        )
    }
}

data class StringValue(val value: String) : RuntimeValue {
    override fun toString(): String = value
}