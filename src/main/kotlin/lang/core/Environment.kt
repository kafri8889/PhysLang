package lang.core

import lang.ast.PhysicsValue
import lang.ast.RuntimeValue

class Environment(private val parent: Environment? = null) {

    private val symbolTable = mutableMapOf<String, RuntimeValue?>()
    private val unitRegistry = UnitRegistry()

    init {
        // Register native built-in functions
        if (parent == null) {
            symbolTable["SI"] = RuntimeValue.NativeFunction("SI", SIFunction())
            symbolTable["sqrt"] = RuntimeValue.NativeFunction("sqrt", SqrtFunction())
        }
    }

    // Variable management

    /**
     * Check if this environment has variable [variable]
     */
    fun hasVar(variable: String): Boolean {
        return symbolTable.containsKey(variable) || parent?.hasVar(variable) ?: false
    }

    fun putVar(variable: String, value: RuntimeValue?) {
        symbolTable[variable] = value
    }

    fun assignVar(variable: String, value: RuntimeValue?): Boolean {
        if (symbolTable.containsKey(variable)) {
            symbolTable[variable] = value
            return true
        }

        return parent?.assignVar(variable, value) ?: false
    }

    fun getVar(variable: String, recursive: Boolean = true): RuntimeValue? {
        if (symbolTable.containsKey(variable)) {
            return symbolTable[variable]
        }

        return if (recursive) parent?.getVar(variable, true) else null
    }

    // Unit management

    fun putUnit(unit: String, value: PhysicsValue) {
        unitRegistry.putUnit(unit, value)
    }

    /**
     * Only retrieves units registered in unitTable
     */
    fun getRegisteredUnit(unit: String, recursive: Boolean = true): PhysicsValue? {
        return unitRegistry.getRegisteredUnit(unit)
            ?: if (recursive) parent?.getRegisteredUnit(unit, true) else null
    }

    /**
     * Retrieves both built-in and registered units
     */
    fun getUnit(unit: String, recursive: Boolean = true): PhysicsValue? {
        return getRegisteredUnit(unit, recursive)
            ?: unitRegistry.getBuiltInUnit(unit)
    }

    fun hasRegisteredUnit(unit: String, recursive: Boolean = true): Boolean {
        return unitRegistry.hasRegisteredUnit(unit) ||
                (recursive && parent?.hasRegisteredUnit(unit, true) == true)
    }

    fun hasBuiltInUnit(unit: String): Boolean {
        return unitRegistry.hasBuiltInUnit(unit)
    }

    /**
     * Checks whether a unit exists in either source
     */
    fun hasUnit(unit: String, recursive: Boolean = true): Boolean {
        return getUnit(unit, recursive) != null
    }

    fun hasRegisteredUnit(unit: String): Boolean {
        return unitRegistry.hasRegisteredUnit(unit)
    }

    fun getUnitByDimensions(dimensions: IntArray, recursive: Boolean = true): Pair<String, PhysicsValue>? {
        return unitRegistry.getUnitByDimensions(dimensions, recursive)
    }

    fun formatPhysicsValue(value: PhysicsValue): String {
        val targetUnit = value.unitName?.let { unit ->
            getUnit(unit)?.let { unit to it }
        } ?: getUnitByDimensions(value.dimensions)

        if (targetUnit != null) {
            val (unitName, unitDefinition) = targetUnit
            val convertedValue = value.scaledValue / unitDefinition.scale

            return "$convertedValue $unitName"
        }

        return "${value.scaledValue} ${value.dimensionsToString()}"
    }

    private inner class UnitRegistry {
        private val unitTable = mutableMapOf<String, PhysicsValue>()

        fun putUnit(unit: String, value: PhysicsValue) {
            unitTable[unit] = value
        }

        fun getBuiltInUnit(unit: String): PhysicsValue? {
            return PhysicsValue.unitRegistry[unit]
        }

        fun getRegisteredUnit(unit: String): PhysicsValue? {
            return unitTable[unit]
        }

        fun getUnit(unit: String): PhysicsValue? {
            return getBuiltInUnit(unit) ?: getRegisteredUnit(unit)
        }

        fun hasUnit(unit: String): Boolean {
            return hasBuiltInUnit(unit) || hasRegisteredUnit(unit)
        }

        fun hasRegisteredUnit(unit: String): Boolean {
            return unit in unitTable
        }

        fun hasBuiltInUnit(unit: String): Boolean {
            return PhysicsValue.unitRegistry.containsKey(unit)
        }

        fun getRegisteredUnitByDimensions(
            dimensions: IntArray
        ): Pair<String, PhysicsValue>? {
            return unitTable.entries.find {
                it.value.dimensions.contentEquals(dimensions)
            }?.let {
                it.key to it.value
            }
        }

        fun getBuiltInUnitByDimensions(
            dimensions: IntArray
        ): Pair<String, PhysicsValue>? {
            return PhysicsValue.unitRegistry.entries.find {
                it.value.dimensions.contentEquals(dimensions)
            }?.let {
                it.key to it.value
            }
        }

        fun getUnitByDimensions(
            dimensions: IntArray,
            recursive: Boolean = true
        ): Pair<String, PhysicsValue>? {

            val registeredUnit = unitRegistry.getRegisteredUnitByDimensions(dimensions)

            if (registeredUnit != null) return registeredUnit

            if (recursive) {
                val parentUnit = parent?.getUnitByDimensions(dimensions, true)
                if (parentUnit != null) return parentUnit
            }

            return unitRegistry.getBuiltInUnitByDimensions(dimensions)
        }
    }
}