package lang.error

/**
 * A class that reports errors
 */
class ErrorReporter {

    val errors = mutableListOf<Diagnostic>()

    fun hasErrors(): Boolean = errors.any { it.type == DiagnosticType.ERROR }

    fun clear() {
        errors.clear()
    }

    fun report(diagnostic: Diagnostic) {
        errors.add(diagnostic)
    }

    fun printAll(source: String) {
        if (errors.isEmpty()) return

        val lines = source.lines()

        for (diagnostic in errors) {
            val lineIndex = diagnostic.line
            val lineText = lines.getOrNull(lineIndex) ?: ""

            val start = diagnostic.column.first.coerceAtLeast(0)
            val endExclusive = (diagnostic.column.last + 1)
                .coerceAtLeast(start + 1)

            // caret marker: "   ^^^"
            val caretCount = (endExclusive - start).coerceAtLeast(1)
            val pointer = " ".repeat(start) + "^".repeat(caretCount)

            println("${diagnostic.type}: ${diagnostic.message}")
            println(" --> line ${lineIndex + 1}, column ${start + 1}")
            println(lineText)
            println(pointer)
            println()
        }
    }

}