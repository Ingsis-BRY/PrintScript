package com.printscript.cli

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.pipeline.StatementParser
import com.printscript.pipeline.StatementStream
import com.printscript.pipeline.TokenSource
import com.printscript.report.Diagnostic
import com.printscript.report.ErrorRenderer
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.token.Token
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CliTest {
    private val at = Position(1, 1)

    private val divisionByZero = Diagnostic.DivisionByZero(Span.at(at))

    private fun statement(): Statement =
        Statement.CallStatement("println", Expression.NumberLiteral(1.0, at, at), at, at)

    private fun anyFile(): Path {
        val file = Files.createTempFile("printscript", ".ps")
        file.toFile().deleteOnExit()
        Files.writeString(file, "ignored by the fake stream")
        return file
    }

    private fun streamOf(results: List<Result<Statement>>): StatementStream {
        val pending = ArrayDeque(results)

        return StatementStream(
            source =
                TokenSource {
                    results.asSequence().map { Success(Token.SemicolonToken(";", at, at)) }
                },
            parser = StatementParser { pending.removeFirst() },
        )
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

    private inner class Run(
        results: List<Result<Statement>>,
        failAt: Int? = null,
    ) {
        val progress = StringBuilder()
        val errors = StringBuilder()
        val program = RecordingProgram(failAt, divisionByZero)

        val cli =
            Cli(
                newStream = { streamOf(results) },
                newProgram = { program },
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

        val result = run.cli.run(Operation.EXECUTION, anyFile())

        assertIs<Success<Unit>>(result)
        assertEquals(3, run.program.executed.size)
    }

    @Test
    fun `execution stops at the first statement that fails`() {
        val run = Run(succeeding(3), failAt = 2)

        val result = run.cli.run(Operation.EXECUTION, anyFile())

        assertIs<Failure>(result)
        assertEquals(2, run.program.executed.size, "the third statement should never run")
        assertContains(run.errors.toString(), "Division by zero.")
    }

    @Test
    fun `a failure from the stream stops the run and reaches the renderer`() {
        val run = Run(listOf(Success(statement()), Failure(divisionByZero)))

        val result = run.cli.run(Operation.EXECUTION, anyFile())

        assertIs<Failure>(result)
        assertEquals(1, run.program.executed.size)
        assertEquals("(1:1)-(1:1) Division by zero.", run.errors.toString().trim())
    }

    @Test
    fun `progress is reported once per statement`() {
        val run = Run(succeeding(3))

        run.cli.run(Operation.EXECUTION, anyFile())

        assertEquals(3, run.progress.lines().count { it.isNotBlank() })
    }

    @Test
    fun `validation walks the statements without running any`() {
        val run = Run(succeeding(3))

        val result = run.cli.run(Operation.VALIDATION, anyFile())

        assertIs<Success<Unit>>(result)
        assertTrue(run.program.executed.isEmpty(), "validation must not execute")
        assertEquals(3, run.progress.lines().count { it.isNotBlank() })
    }

    @Test
    fun `validation reports the first failure and stops`() {
        val run = Run(listOf(Success(statement()), Failure(divisionByZero), Success(statement())))

        val result = run.cli.run(Operation.VALIDATION, anyFile())

        assertIs<Failure>(result)
        assertContains(run.errors.toString(), "Division by zero.")
    }

    @Test
    fun `the supported version is accepted`() {
        val run = Run(succeeding(1))

        val result = run.cli.run(Operation.EXECUTION, anyFile(), version = "1.0")

        assertIs<Success<Unit>>(result)
    }

    @Test
    fun `an unsupported version is rejected before the file is read`() {
        val run = Run(succeeding(1))

        val error =
            assertFailsWith<IllegalArgumentException> {
                run.cli.run(Operation.EXECUTION, anyFile(), version = "9.9")
            }

        assertContains(error.message.orEmpty(), "Unsupported version")
        assertTrue(run.program.executed.isEmpty())
    }
}
