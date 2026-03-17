package lang.core

class Environment {

    private val symbolTable = mutableMapOf<String, PhysicsValue?>()

    fun put(variable: String, value: PhysicsValue?) {
        symbolTable[variable] = value
    }

    fun get(variable: String): PhysicsValue? {
        return symbolTable[variable]
    }

}