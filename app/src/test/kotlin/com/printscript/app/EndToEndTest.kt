package com.printscript.app

import com.printscript.cli.Cli
import com.printscript.cli.Operation
import com.printscript.cli.Program
import com.printscript.cli.ProgressPrinter
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.interpreter.CollectingOutput
import com.printscript.interpreter.Environment
import com.printscript.interpreter.Interpreter
import com.printscript.interpreter.ValueOps
import com.printscript.lexer.Lexer
import com.printscript.lexer.StreamSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.Parser
import com.printscript.pipeline.StatementParser
import com.printscript.pipeline.StatementStream
import com.printscript.pipeline.TokenSource
import com.printscript.report.ErrorRenderer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EndToEndTest {

    private fun sourceFile(source: String): Path {
        val file = Files.createTempFile("printscript", ".ps")
        file.toFile().deleteOnExit()
        Files.writeString(file, source)
        return file
    }

    // the pieces a run writes to, kept together so a test can inspect any of them
    private class Run(
        val output: CollectingOutput = CollectingOutput(),
        val progress: StringBuilder = StringBuilder(),
        val errors: StringBuilder = StringBuilder()
    ) {
        val cli = Cli(
            newStream = { reader ->
                val lexer = Lexer(StreamSourceReader(reader), TokenRecognizers.DEFAULT)

                StatementStream(
                    source = TokenSource(lexer::tokens),
                    parser = StatementParser(Parser::parse)
                )
            },
            newProgram = {
                val interpreter = Interpreter(Environment(), output, ValueOps())

                Program(interpreter::execute)
            },
            renderer = ErrorRenderer(),
            progress = ProgressPrinter(progress),
            errors = errors
        )
    }

    // runs the source and returns the collected program output, failing the test
    // if the run does not succeed
    private fun execute(source: String): List<String> {
        val run = Run()

        assertIs<Success<Unit>>(run.cli.run(Operation.EXECUTION, sourceFile(source)))

        return run.output.lines()
    }

    // The three spec examples, end to end

    @Test
    fun `example 1 concatenates strings from file`() {
        val output = execute(
            """
            let name: string = "Joe";
            let lastName: string = "Doe";
            println(name + " " + lastName);
            """.trimIndent()
        )

        assertEquals(listOf("Joe Doe"), output)
    }

    @Test
    fun `example 2 divides and prints an integer result from file`() {
        val output = execute(
            """
            let a: number = 12;
            let b: number = 4;
            let c: number = a / b;
            println("Result: " + c);
            """.trimIndent()
        )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `example 3 reassigns before printing from file`() {
        val output = execute(
            """
            let a: number = 12;
            let b: number = 4;
            a = a / b;
            println("Result: " + a);
            """.trimIndent()
        )

        assertEquals(listOf("Result: 3"), output)
    }

    // Execution

    @Test
    fun `prints several lines in source order`() {
        val output = execute(
            """
            println(1);
            println(2);
            println(3);
            """.trimIndent()
        )

        assertEquals(listOf("1", "2", "3"), output)
    }

    @Test
    fun `reports progress for every statement it parses`() {
        val run = Run()

        run.cli.run(
            Operation.EXECUTION,
            sourceFile(
                """
                let x: number = 1;
                x = 2;
                println(x);
                """.trimIndent()
            )
        )

        val reported = run.progress.lines().count { it.isNotBlank() }
        assertEquals(3, reported)
    }

    // Validation

    @Test
    fun `validation walks a valid program without executing it`() {
        val run = Run()

        val result = run.cli.run(
            Operation.VALIDATION,
            sourceFile(
                """
                let x: number = 1;
                println(x);
                """.trimIndent()
            )
        )

        assertIs<Success<Unit>>(result)
        assertTrue(run.output.lines().isEmpty())
    }

    @Test
    fun `validation reports a syntax error and stops`() {
        val run = Run()

        val result = run.cli.run(Operation.VALIDATION, sourceFile("let x number = 5;"))

        assertIs<Failure>(result)
        assertContains(run.errors.toString(), "Expected ':'")
    }

    // Errors cut the run and reach the renderer

    @Test
    fun `execution stops at the first error but keeps the output before it`() {
        val run = Run()

        val result = run.cli.run(
            Operation.EXECUTION,
            sourceFile(
                """
                println(1);
                let b: number = 2 / 0;
                println(3);
                """.trimIndent()
            )
        )

        assertIs<Failure>(result)
        assertEquals(listOf("1"), run.output.lines())
        assertContains(run.errors.toString(), "Division by zero.")
    }

    @Test
    fun `a syntax error is reported and stops the run`() {
        val run = Run()

        val result = run.cli.run(Operation.EXECUTION, sourceFile("let x number = 5;"))

        assertIs<Failure>(result)
        assertContains(run.errors.toString(), "Expected ':'")
    }

    @Test
    fun `a lexical error is reported and stops the run`() {
        val run = Run()

        val result = run.cli.run(Operation.EXECUTION, sourceFile("let x = @;"))

        assertIs<Failure>(result)
        assertContains(run.errors.toString(), "Unexpected character '@'.")
    }

    @Test
    fun `an undeclared variable is reported`() {
        val run = Run()

        val result = run.cli.run(Operation.EXECUTION, sourceFile("println(missing);"))

        assertIs<Failure>(result)
        assertContains(run.errors.toString(), "Variable 'missing' is not declared.")
    }

    @Test
    fun `a reported error carries its span from first to last character`() {
        val run = Run()

        run.cli.run(Operation.EXECUTION, sourceFile("println(1 / 0);"))

        assertEquals("(1:9)-(1:13) Division by zero.", run.errors.toString().trim())
    }

    // Version

    @Test
    fun `the supported version is accepted`() {
        val run = Run()

        val result: Result<Unit> =
            run.cli.run(Operation.EXECUTION, sourceFile("println(1);"), version = "1.0")

        assertIs<Success<Unit>>(result)
        assertEquals(listOf("1"), run.output.lines())
    }

    @Test
    fun `an unsupported version is rejected before the file is read`() {
        val run = Run()

        val error = assertFailsWith<IllegalArgumentException> {
            run.cli.run(Operation.EXECUTION, sourceFile("println(1);"), version = "9.9")
        }

        assertContains(error.message.orEmpty(), "Unsupported version")
        assertTrue(run.output.lines().isEmpty())
    }
}
