package com.printscript.cli

import com.printscript.report.ErrorRenderer
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import java.nio.file.Path

/**
* the operation the CLI runs over the source.
*/
enum class Operation {
    VALIDATION,
    EXECUTION,
}

/**
* drives the pipeline from a source file to its result. the only component
* with effects: it prints output, reports errors and decides when to stop.
*
* the source is consumed one statement at a time, so a file of any size is
* never loaded whole.
*/
class Cli(
    private val newStatements: (Path) -> StatementSource,
    private val newProgram: () -> Program,
    private val renderer: ErrorRenderer,
    private val progress: ProgressPrinter,
    private val errors: Appendable,
) {
    /**
     * runs [operation] over [file], stopping at the first error
     */
    fun run(
        operation: Operation,
        file: Path,
    ): Result<Unit> =
        newStatements(file).use { statements ->
            when (operation) {
                Operation.VALIDATION -> validate(statements)
                Operation.EXECUTION -> execute(statements)
            }
        }

    /**
     * walks the source parsing every statement, without running any of them
     */
    private fun validate(statements: StatementSource): Result<Unit> {
        while (statements.hasNext()) {
            when (val parsed = statements.next()) {
                is Failure -> return report(parsed)
                is Success -> progress.statementParsed(parsed.value.start)
            }
        }

        return Success(Unit)
    }

    /**
     * runs each statement as it comes out of the source
     */
    private fun execute(statements: StatementSource): Result<Unit> {
        val program = newProgram()

        while (statements.hasNext()) {
            when (val parsed = statements.next()) {
                is Failure -> return report(parsed)

                is Success -> {
                    progress.statementParsed(parsed.value.start)

                    val executed = program.execute(parsed.value)
                    if (executed is Failure) {
                        return report(executed)
                    }
                }
            }
        }

        return Success(Unit)
    }

    /**
     * shows the error and hands it back, so the caller stops on it
     */
    private fun report(failure: Failure): Failure {
        errors.appendLine(renderer.render(failure.error))
        return failure
    }
}
