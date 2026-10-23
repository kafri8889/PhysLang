package lang.core

import lang.ast.PhysicsValue
import lang.ast.RuntimeValue

class Environment(private val parent: Environment? = null) {

    private val symbolTable = mutableMapOf<String, RuntimeValue?>()
    private val unitTable = mutableMapOf<String, PhysicsValue>()

    init {
        // Register native built-in functions
        if (parent == null) {
            symbolTable["SI"] = RuntimeValue.NativeFunction("SI", SIFunction())
            symbolTable["sqrt"] = RuntimeValue.NativeFunction("sqrt", SqrtFunction())
        }
    }

    /**
     * Check if this environment has variable [variable]
     */
    fun hasVar(variable: String): Boolean {
        return symbolTable.containsKey(variable) || parent?.hasVar(variable) ?: false
    }

    fun putVar(variable: String, value: RuntimeValue?) {
        symbolTable[variable] = value
    }

    fun getVar(variable: String, recursive: Boolean = true): RuntimeValue? {
        if (symbolTable.containsKey(variable)) {
            return symbolTable[variable]
        }

        return if (recursive) parent?.getVar(variable, true) else null
    }

    fun putUnit(unit: String, value: PhysicsValue) {
        unitTable[unit] = value
    }

    fun getUnit(unit: String): PhysicsValue? {
        return unitTable[unit]
    }

}