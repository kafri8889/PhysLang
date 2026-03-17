import lang.cli.PhysCli
import lang.cli.PhysRunner
import java.io.File

fun main() {
    val cli = PhysCli()
    val stream = PhysRunner()

    stream.runFile(File("D:\\DOCUMENTS_V3\\Java Kotlin Project\\PhysLang\\assets\\example1.phys"))
    cli.run()
}