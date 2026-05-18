import lang.ast.visitors.PhysicsEvaluator
import lang.cli.PhysCli
import lang.cli.PhysRunner
import lang.core.Environment
import lang.error.ErrorReporter
import lang.lexer.Lexer
import lang.parser.Parser
import lang.semantic.SemanticAnalyzer
import java.io.File

fun main() {
    ExampleRunner().run()
}

class PhysLang {

    val errorReporter = ErrorReporter()
    val environment = Environment()
    val runner = PhysRunner(environment, errorReporter)
    val cli = PhysCli(environment, errorReporter)

    fun runFile(file: File) {
        errorReporter.clear()
        runner.runFile(file)
    }

    fun runScript(script: String) {
        errorReporter.clear()
        runner.runScript(script)
    }

    fun run() {
        errorReporter.clear()
        cli.run()
    }
}

class ExampleRunner(
    private val assetsDirectory: File = File("assets")
) {

    fun run() {
        println()
        println("PhysLang Runner")
        println("=======================")
        println()
        println("1. Open interactive CLI")
        println("2. Run custom script from file path")
        println("3. List feature syntax files")
        println("4. List example code files")
        println()
        print("Choose menu number: ")

        when (readlnOrNull()?.trim()) {
            "1" -> PhysLang().run()
            "2" -> runCustomScript()
            "3" -> chooseAndRunFromDirectory(File(assetsDirectory, "fitur"), "Feature Syntax Files")
            "4" -> chooseAndRunFromDirectory(assetsDirectory, "Example Code Files")
            else -> println("Invalid menu number.")
        }
    }

    private fun runCustomScript() {
        println()
        print("Enter .phys file path: ")

        val rawPath = readlnOrNull()?.trim()?.trim('"')
        if (rawPath.isNullOrBlank()) {
            println("File path is empty.")
            return
        }

        val file = File(rawPath)
        if (!file.exists() || !file.isFile) {
            println("File not found: $rawPath")
            return
        }

        runScriptFile(file, "Custom Script")
    }

    private fun chooseAndRunFromDirectory(directory: File, title: String) {
        val files = directory
            .listFiles { file -> file.isFile && file.extension == "phys" }
            ?.sortedBy { it.name }
            ?: emptyList()

        if (files.isEmpty()) {
            println("No .phys files found in ${directory.path}")
            return
        }

        println()
        println(title)
        println("-".repeat(title.length))
        println()

        files.forEachIndexed { index, file ->
            println("${index + 1}. ${file.nameWithoutExtension}")
        }

        println("0. Back")
        println()
        print("Choose file number: ")

        val selectedNumber = readlnOrNull()?.trim()?.toIntOrNull()
        if (selectedNumber == 0) return

        val selectedFile = selectedNumber
            ?.takeIf { it in 1..files.size }
            ?.let { files[it - 1] }

        if (selectedFile == null) {
            println("Invalid file number.")
            return
        }

        runScriptFile(selectedFile, title)
    }

    private fun runScriptFile(file: File, label: String) {
        val source = file.readText()

        println()
        println("$label: ${file.name}")
        println("Source")
        println("------")
        println(source.trim())
        println()
        println("Output")
        println("------")

        executeSequentially(source)
    }

    private fun executeSequentially(source: String) {
        val environment = Environment()
        val reporter = ErrorReporter()
        val parser = Parser(Lexer(source).lex(), reporter)
        val statements = parser.parseProgram()

        if (reporter.hasErrors()) {
            reporter.printAll(source)
            return
        }

        val evaluator = PhysicsEvaluator(environment)

        for (statement in statements) {
            reporter.clear()

            statement.accept(SemanticAnalyzer(environment, reporter))

            if (reporter.hasErrors()) {
                reporter.printAll(source)
                return
            }

            statement.accept(evaluator)
        }
    }
}
