package com.printscript.cli

import com.printscript.common.Failure
import com.printscript.common.Result
import com.printscript.common.Success
import com.printscript.interpreter.ConsoleOutput
import com.printscript.interpreter.Interpreter
import com.printscript.interpreter.OutputEmitter
import com.printscript.lexer.Lexer
import com.printscript.lexer.StreamSourceReader
import com.printscript.pipeline.StatementStream
import com.printscript.report.ErrorRenderer
import java.nio.file.Files
import java.nio.file.Path

/**
* the operation the CLI runs over the source.
*/
enum class Operation {
    VALIDATION,
    EXECUTION
}

/**
* drives the pipeline from a source file to its result. the only component
* with effects: it reads the file, prints output, reports errors and decides
* when to stop.
*
* the source is consumed one statement at a time, so a file of any size is
* never loaded whole.
*/
class Cli(
    private val output: OutputEmitter = ConsoleOutput(),
    private val progress: ProgressPrinter = ProgressPrinter(),
    private val renderer: ErrorRenderer = ErrorRenderer(),
    private val errors: Appendable = System.err
) {

    /**
    * runs [operation] over [file], stopping at the first error.
    * [version] is optional; an unsupported one is rejected before the file is
    * read, since it is a misuse of the CLI rather than a fault in the source.
    */
    fun run(operation: Operation, file: Path, version: String? = null): Result<Unit> {
        requireSupported(version)

        return Files.newBufferedReader(file).use { reader ->
            val stream = StatementStream(Lexer(StreamSourceReader(reader)))

            when (operation) {
                Operation.VALIDATION -> validate(stream)
                Operation.EXECUTION -> execute(stream)
            }
        }
    }

    /**
    * walks the stream parsing every statement, without running any of them
    */
    private fun validate(stream: StatementStream): Result<Unit> {
        while (stream.hasNext()) {
            when (val parsed = stream.next()) {
                is Failure -> return report(parsed)
                is Success -> progress.statementParsed(parsed.value.start)
            }
        }

        return Success(Unit)
    }

    /**
    * runs each statement as it comes out of the stream
    */
    private fun execute(stream: StatementStream): Result<Unit> {
        val interpreter = Interpreter(output = output)

        while (stream.hasNext()) {
            when (val parsed = stream.next()) {
                is Failure -> return report(parsed)

                is Success -> {
                    progress.statementParsed(parsed.value.start)

                    val executed = interpreter.execute(parsed.value)
                    if (executed is Failure) {
                        return report(executed)
                    }
                }
            }
        }

        return Success(Unit)
    }

    /**
    * rejects a version the CLI does not support; a missing one takes the default
    */
    private fun requireSupported(version: String?) {
        require(version == null || version == SUPPORTED_VERSION) {
            "Unsupported version: $version"
        }
    }

    /**
    * shows the error and hands it back, so the caller stops on it
    */
    private fun report(failure: Failure): Failure {
        errors.appendLine(renderer.render(failure.error))
        return failure
    }

    private companion object {
        const val SUPPORTED_VERSION = "1.0"
    }
}
