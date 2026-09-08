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
