package com.printscript.app

import com.printscript.cli.Operation
import com.printscript.formatter.ConfigError
import com.printscript.interpreter.EnvironmentSource
import com.printscript.interpreter.InputProvider
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
    description = ["Corre, valida o formatea un programa PrintScript."],
)
class PrintScriptCommand(
    private val output: OutputEmitter,
    private val input: InputProvider,
    private val environment: EnvironmentSource,
    private val out: Appendable,
    private val errors: Appendable,
) : Callable<Int> {
    @Parameters(
        index = "0",
        paramLabel = "OPERATION",
        description = ["validation, execution, formatting o analyzing"],
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
        description = ["version del lenguaje: 1.0 o 1.1 (por defecto 1.0)"],
    )
    var version: String? = null

    @Option(
        names = ["--config", "-c"],
        paramLabel = "CONFIG",
        description = ["archivo de reglas para formatting o analyzing, en JSON"],
    )
    var config: Path? = null

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
        val requested = version ?: Dialect.DEFAULT_VERSION

        val dialect =
            Dialect.of(requested)
                ?: return unsupported(requested)

        val cli =
            PrintScript(
                dialect = dialect,
                output = output,
                input = input,
                environment = environment,
                out = out,
                progress = if (verbose) errors else Discarded,
                errors = errors,
                config = config,
            ).cli()

        return try {
            if (cli.run(operation, file) is Failure) {
                CommandLine.ExitCode.SOFTWARE
            } else {
                CommandLine.ExitCode.OK
            }
        } catch (error: ConfigError) {
            errors.appendLine(error.message)
            CommandLine.ExitCode.USAGE
        } catch (error: NoSuchFileException) {
            errors.appendLine("cannot read $file: no such file")
            CommandLine.ExitCode.USAGE
        } catch (error: IOException) {
            errors.appendLine("cannot read $file: ${error.message ?: "I/O error"}")
            CommandLine.ExitCode.USAGE
        }
    }

    private fun unsupported(requested: String): Int {
        errors.appendLine(
            "Unsupported version: $requested. Known versions: " +
                Dialect.versions().joinToString(", ") + ".",
        )

        return CommandLine.ExitCode.USAGE
    }
}
