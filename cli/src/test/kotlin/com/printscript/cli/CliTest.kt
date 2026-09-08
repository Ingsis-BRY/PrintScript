package com.printscript.cli

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.report.Diagnostic
import com.printscript.report.ErrorRenderer
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CliTest {
    private val at = Position(1, 1)

    private val divisionByZero = Diagnostic.DivisionByZero(Span.at(at))

    private fun statement(): Statement =
        Statement.CallStatement("println", Expression.NumberLiteral(1.0, at, at), at, at)

    private val anyFile: Path = Path.of("ignored-by-the-fake.ps")

    private class FakeStatements(
        results: List<Result<Statement>>,
    ) : StatementSource {
        private val pending = ArrayDeque(results)

        var closed = false
            private set

        override fun hasNext(): Boolean = pending.isNotEmpty()

        override fun next(): Result<Statement> = pending.removeFirst()

        override fun close() {
            closed = true
        }
    }

    private class RecordingProgram(
        private val failAt: Int?,
        private val error: Diagnostic,
    ) : Program {
        val executed = mutableListOf<Statement>()

        override fun execute(statement: Statement): Result<Unit> {
            executed.add(statement)

            return if (executed.size == failAt) Failure(error) else Success(Unit)
        }
    }

    private class RecordingFormatting(
        private val result: Result<Unit>,
        private val out: StringBuilder,
    ) : Formatting {
        var closed = false
            private set

        override fun format(): Result<Unit> {
            out.append("formatted")

            return result
        }

        override fun close() {
            closed = true
        }
    }

    private class RecordingAnalyzing(
        private val result: Result<Unit>,
    ) : Analyzing {
        var closed = false
            private set

        var analyzed = false
            private set

        override fun analyze(): Result<Unit> {
            analyzed = true
            return result
        }

        override fun close() {
            closed = true
        }
    }

    private inner class Run(
        results: List<Result<Statement>>,
        failAt: Int? = null,
        formatting: Result<Unit> = Success(Unit),
        analyzing: Result<Unit> = Success(Unit),
    ) {
        val progress = StringBuilder()
        val errors = StringBuilder()
        val formatted = StringBuilder()
        val program = RecordingProgram(failAt, divisionByZero)
        val analyzing = RecordingAnalyzing(analyzing)
        val statements = FakeStatements(results)
        val formatting = RecordingFormatting(formatting, formatted)

        val cli =
            Cli(
                newStatements = { statements },
                newProgram = { program },
                newFormatting = { this.formatting },
                newAnalyzing = { this.analyzing },
                renderer = ErrorRenderer(),
                progress = ProgressPrinter(progress),
                errors = errors,
            )
    }

    private fun succeeding(count: Int): List<Result<Statement>> =
        List(count) { Success(statement()) }

    @Test
    fun `execution runs every statement in source order`() {
        val run = Run(succeeding(3))

        val result = run.cli.run(Operation.EXECUTION, anyFile)

        assertIs<Success<Unit>>(result)
        assertEquals(3, run.program.executed.size)
    }

    @Test
    fun `execution stops at the first statement that fails`() {
        val run = Run(succeeding(3), failAt = 2)

        val result = run.cli.run(Operation.EXECUTION, anyFile)

        assertIs<Failure>(result)
        assertEquals(2, run.program.executed.size, "the third statement should never run")
        assertContains(run.errors.toString(), "Division by zero.")
    }

    @Test
    fun `a failure from the source stops the run and reaches the renderer`() {
        val run = Run(listOf(Success(statement()), Failure(divisionByZero)))

        val result = run.cli.run(Operation.EXECUTION, anyFile)

        assertIs<Failure>(result)
        assertEquals(1, run.program.executed.size)
        assertEquals("(1:1)-(1:1) Division by zero.", run.errors.toString().trim())
    }

    @Test
    fun `progress is reported once per statement`() {
        val run = Run(succeeding(3))

        run.cli.run(Operation.EXECUTION, anyFile)

        assertEquals(3, run.progress.lines().count { it.isNotBlank() })
    }

    @Test
    fun `validation walks the statements without running any`() {
        val run = Run(succeeding(3))

        val result = run.cli.run(Operation.VALIDATION, anyFile)

        assertIs<Success<Unit>>(result)
        assertTrue(run.program.executed.isEmpty(), "validation must not execute")
        assertEquals(3, run.progress.lines().count { it.isNotBlank() })
    }

    @Test
    fun `validation reports the first failure and stops`() {
        val run = Run(listOf(Success(statement()), Failure(divisionByZero), Success(statement())))

        val result = run.cli.run(Operation.VALIDATION, anyFile)

        assertIs<Failure>(result)
        assertContains(run.errors.toString(), "Division by zero.")
    }

    @Test
    fun `the source is closed when the run succeeds`() {
        val run = Run(succeeding(2))

        run.cli.run(Operation.EXECUTION, anyFile)

        assertTrue(run.statements.closed)
    }

    @Test
    fun `the source is closed when the run stops at an error`() {
        val run = Run(succeeding(3), failAt = 1)

        assertIs<Failure>(run.cli.run(Operation.EXECUTION, anyFile))

        assertTrue(run.statements.closed, "un error no puede dejar el archivo abierto")
    }

    @Test
    fun `formatting rewrites the source without parsing a statement`() {
        val run = Run(succeeding(3))

        val result = run.cli.run(Operation.FORMATTING, anyFile)

        assertIs<Success<Unit>>(result)
        assertEquals("formatted", run.formatted.toString())
        assertTrue(run.program.executed.isEmpty(), "formatting must not execute")
    }

    @Test
    fun `formatting reports no progress, since it never sees a statement`() {
        val run = Run(succeeding(3))

        run.cli.run(Operation.FORMATTING, anyFile)

        assertTrue(run.progress.isEmpty())
    }

    @Test
    fun `a failure while formatting reaches the renderer`() {
        val run = Run(succeeding(1), formatting = Failure(divisionByZero))

        val result = run.cli.run(Operation.FORMATTING, anyFile)

        assertIs<Failure>(result)
        assertEquals("(1:1)-(1:1) Division by zero.", run.errors.toString().trim())
    }

    @Test
    fun `the formatting is closed when it succeeds`() {
        val run = Run(succeeding(1))

        run.cli.run(Operation.FORMATTING, anyFile)

        assertTrue(run.formatting.closed)
    }

    @Test
    fun `the formatting is closed when it fails`() {
        val run = Run(succeeding(1), formatting = Failure(divisionByZero))

        assertIs<Failure>(run.cli.run(Operation.FORMATTING, anyFile))

        assertTrue(run.formatting.closed, "un error no puede dejar el archivo abierto")
    }

    @Test
    fun `formatting does not open the statement source`() {
        val run = Run(succeeding(3))

        run.cli.run(Operation.FORMATTING, anyFile)

        assertFalse(run.statements.closed, "the statement source was never opened")
    }

    @Test
    fun `analyzing runs the analyzer`() {
        val run = Run(succeeding(3))

        val result = run.cli.run(Operation.ANALYZING, anyFile)

        assertIs<Success<Unit>>(result)
        assertTrue(run.analyzing.analyzed)
    }

    @Test
    fun `analyzing closes the analyzer when it succeeds`() {
        val run = Run(succeeding(1))

        run.cli.run(Operation.ANALYZING, anyFile)

        assertTrue(run.analyzing.closed)
    }

    @Test
    fun `a failure while analyzing reaches the renderer`() {
        val run = Run(succeeding(1), analyzing = Failure(divisionByZero))

        val result = run.cli.run(Operation.ANALYZING, anyFile)

        assertIs<Failure>(result)
        assertEquals(
            "(1:1)-(1:1) Division by zero.",
            run.errors.toString().trim(),
        )
        assertTrue(run.analyzing.closed)
    }
}
