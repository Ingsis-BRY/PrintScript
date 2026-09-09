package com.printscript.cli

import com.printscript.report.ErrorRenderer
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import java.nio.file.Path

enum class Operation {
    VALIDATION,
    EXECUTION,
    FORMATTING,
    ANALYZING,
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
    private val newFormatting: (Path) -> Formatting,
    private val newAnalyzer: () -> Analyzer,
    private val renderer: ErrorRenderer,
    private val progress: ProgressPrinter,
    private val errors: Appendable,
    private val findings: Appendable,
) {
    fun run(
        operation: Operation,
        file: Path,
    ): Result<Unit> =
        when (operation) {
            Operation.VALIDATION -> overStatements(file, ::validate)
            Operation.EXECUTION -> overStatements(file, ::execute)
            Operation.FORMATTING -> format(file)
            Operation.ANALYZING -> overStatements(file, ::analyze)
        }

    private fun overStatements(
        file: Path,
        walk: (StatementSource) -> Result<Unit>,
    ): Result<Unit> = newStatements(file).use(walk)

    private fun format(file: Path): Result<Unit> {
        val result = newFormatting(file).use { it.format() }

        return if (result is Failure) report(result) else result
    }

    private fun validate(statements: StatementSource): Result<Unit> {
        while (statements.hasNext()) {
            when (val parsed = statements.next()) {
                is Failure -> return report(parsed)
                is Success -> progress.statementParsed(parsed.value.start)
            }
        }

        return Success(Unit)
    }

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

    private fun analyze(statements: StatementSource): Result<Unit> {
        val analyzer = newAnalyzer()

        while (statements.hasNext()) {
            when (val parsed = statements.next()) {
                is Failure -> return report(parsed)

                is Success -> {
                    progress.statementParsed(parsed.value.start)

                    analyzer.findings(parsed.value).forEach { findings.appendLine(it) }
                }
            }
        }

        return Success(Unit)
    }

    private fun report(failure: Failure): Failure {
        errors.appendLine(renderer.render(failure.error))
        return failure
    }
}
