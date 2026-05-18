package lang.error

/**
 * Represents a diagnostic message, like a compiler error or a warning.
 */
data class Diagnostic(
    val type: DiagnosticType,
    val message: String,
    val line: Int,
    val column: IntRange,
)

enum class DiagnosticType {
    ERROR,
    WARNING,
    INFO,
}
