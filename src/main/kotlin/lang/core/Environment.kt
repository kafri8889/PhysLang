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

    fun putVar(variable: String, value: RuntimeValue?) {
        symbolTable[variable] = value
    }

    fun getVar(variable: String): RuntimeValue? {
        return symbolTable[variable]
    }

    fun putUnit(unit: String, value: PhysicsValue) {
        unitTable[unit] = value
    }

    fun getUnit(unit: String): PhysicsValue? {
        return unitTable[unit]
    }

}