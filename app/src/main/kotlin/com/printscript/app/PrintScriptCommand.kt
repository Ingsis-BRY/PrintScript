package com.printscript.app

import com.printscript.cli.Operation
import com.printscript.interpreter.OutputEmitter
import com.printscript.report.Failure
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.concurrent.Callable

/**
 * un Appendable que descarta todo, para apagar el avance del parseo sin que el
 * CLI tenga que saber si alguien lo esta mirando
 */
internal val Discarded =
    object : Appendable {
        override fun append(value: CharSequence?): Appendable = this

        override fun append(
            value: CharSequence?,
            startIndex: Int,
            endIndex: Int,
        ): Appendable = this

        override fun append(value: Char): Appendable = this
    }

/**
 * la linea de comandos: traduce argv a una corrida y su resultado a un codigo de
 * salida. no arma el grafo, se lo pide a [PrintScript].
 *
 * recibe sus dos sinks por constructor, como todo lo demas, asi que un test la
 * ejercita entera sin tocar la consola ni matar la JVM: devuelve el codigo en
 * lugar de llamar a exitProcess, que es lo unico que queda en main.
 *
 * los campos son var y lateinit porque picocli los asigna por reflexion y no
 * pueden ser final. es el precio de la libreria, y no se contagia: el resto del
 * codigo sigue siendo inmutable.
 */
@Command(
    name = "printscript",
    description = ["Corre o valida un programa PrintScript."],
)
class PrintScriptCommand(
    private val output: OutputEmitter,
    private val errors: Appendable,
) : Callable<Int> {
    @Parameters(
        index = "0",
        paramLabel = "OPERATION",
        description = ["validation o execution"],
    )
    lateinit var operation: Operation

    @Parameters(
        index = "1",
        paramLabel = "FILE",
        description = ["el archivo a procesar"],
    )
    lateinit var file: Path

    @Parameters(
        index = "2",
        arity = "0..1",
        paramLabel = "VERSION",
        description = ["version del lenguaje (por defecto 1.0)"],
    )
    var version: String? = null

    @Option(
        names = ["--verbose", "-v"],
        description = ["muestra el avance del parseo por stderr"],
    )
    var verbose: Boolean = false

    // declarada a mano en lugar de mixinStandardHelpOptions, que agrega tambien
    // -V/--version con el sentido de "la version de la herramienta" y chocaria
    // con la version del lenguaje que este CLI ya recibe
    @Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = ["muestra esta ayuda"],
    )
    var help: Boolean = false

    override fun call(): Int {
        val requested = version ?: PrintScript.DEFAULT_VERSION

        if (!PrintScript.supports(requested)) {
            errors.appendLine("Unsupported version: $requested")
            return CommandLine.ExitCode.USAGE
        }

        val cli =
            PrintScript(
                output = output,
                progress = if (verbose) errors else Discarded,
                errors = errors,
            ).cli()

        // el archivo se abre recien cuando la corrida arranca, asi que no
        // alcanza con validar el Path: que no exista o no se pueda leer es un
        // mal uso del CLI, no una falla del programa
        return try {
            if (cli.run(operation, file) is Failure) {
                CommandLine.ExitCode.SOFTWARE
            } else {
                CommandLine.ExitCode.OK
            }
        } catch (error: NoSuchFileException) {
            errors.appendLine("cannot read $file: no such file")
            CommandLine.ExitCode.USAGE
        } catch (error: IOException) {
            errors.appendLine("cannot read $file: ${error.message ?: "I/O error"}")
            CommandLine.ExitCode.USAGE
        }
    }
}
