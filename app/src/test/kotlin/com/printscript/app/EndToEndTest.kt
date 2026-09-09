package com.printscript.app

import com.printscript.cli.Operation
import com.printscript.formatter.ConfigError
import com.printscript.report.Failure
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EndToEndTest {
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
        val output =
            execute(
                """
                let name: string = "Joe";
                let lastName: string = "Doe";
                println(name + " " + lastName);
                """.trimIndent(),
            )

        assertEquals(listOf("Joe Doe"), output)
    }

    @Test
    fun `example 2 divides and prints an integer result from file`() {
        val output =
            execute(
                """
                let a: number = 12;
                let b: number = 4;
                let c: number = a / b;
                println("Result: " + c);
                """.trimIndent(),
            )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `example 3 reassigns before printing from file`() {
        val output =
            execute(
                """
                let a: number = 12;
                let b: number = 4;
                a = a / b;
                println("Result: " + a);
                """.trimIndent(),
            )

        assertEquals(listOf("Result: 3"), output)
    }

    // Execution

    @Test
    fun `prints several lines in source order`() {
        val output =
            execute(
                """
                println(1);
                println(2);
                println(3);
                """.trimIndent(),
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
                """.trimIndent(),
            ),
        )

        val reported = run.progress.lines().count { it.isNotBlank() }
        assertEquals(3, reported)
    }

    // Validation

    @Test
    fun `validation walks a valid program without executing it`() {
        val run = Run()

        val result =
            run.cli.run(
                Operation.VALIDATION,
                sourceFile(
                    """
                    let x: number = 1;
                    println(x);
                    """.trimIndent(),
                ),
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

        val result =
            run.cli.run(
                Operation.EXECUTION,
                sourceFile(
                    """
                    println(1);
                    let b: number = 2 / 0;
                    println(3);
                    """.trimIndent(),
                ),
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

    private fun format(
        source: String,
        settings: String,
    ): String {
        val run = Run(config = configFile(settings))

        assertIs<Success<Unit>>(run.cli.run(Operation.FORMATTING, sourceFile(source)))

        return run.out.toString()
    }

    // Analyzing

    private val camelCase = """{ "identifier_format": { "enabled": true, "style": "camelCase" } }"""

    private fun analyze(
        source: String,
        settings: String = camelCase,
    ): Run {
        val run = Run(config = configFile(settings))

        run.cli.run(Operation.ANALYZING, sourceFile(source))

        return run
    }

    @Test
    fun `analyzing reports a rule violation with a span, in the same format as errors`() {
        val run = analyze("let BadName: number = 5;")

        assertEquals(
            "(1:1)-(1:24) Invalid identifier 'BadName': expected camel case.",
            run.out.toString().trim(),
        )
    }

    @Test
    fun `analyzing a clean file reports nothing and succeeds`() {
        val run = analyze("let goodName: number = 5;")

        assertEquals("", run.out.toString().trim())
    }

    @Test
    fun `analyzing a file with a syntax error reports it and fails`() {
        val run = Run(config = configFile(camelCase))

        val result = run.cli.run(Operation.ANALYZING, sourceFile("let BadName number = 3;"))

        assertIs<Failure>(result)
        assertContains(run.errors.toString(), "Expected ':'.")
    }

    @Test
    fun `analyzing shows parsing progress`() {
        val run = Run(config = configFile(camelCase))

        run.cli.run(Operation.ANALYZING, sourceFile("let goodName: number = 5;"))

        assertTrue(run.progress.isNotEmpty(), "analyzing must show progress while parsing")
    }

    // Formatting

    @Test
    fun `formatting rewrites a file with the configured rules`() {
        val formatted =
            format(
                "let a:number=1;",
                """{ "mandatory-single-space-separation": true }""",
            )

        assertEquals("let a : number = 1;", formatted)
    }

    @Test
    fun `formatting a file leaves alone what the rules do not name`() {
        val formatted =
            format(
                "let a:number   =   1;",
                """{ "enforce-spacing-after-colon-in-declaration": true }""",
            )

        assertEquals("let a: number   =   1;", formatted)
    }

    @Test
    fun `formatting prints nothing to the program output`() {
        val run = Run(config = configFile("{}"))

        run.cli.run(Operation.FORMATTING, sourceFile("println(1);"))

        assertTrue(run.output.lines().isEmpty(), "formatting must not run the program")
    }

    @Test
    fun `formatting a source with a lexical error reports it and stops`() {
        val run = Run(config = configFile("{}"))

        val result = run.cli.run(Operation.FORMATTING, sourceFile("let a = @;"))

        assertIs<Failure>(result)
        assertContains(run.errors.toString(), "Unexpected character '@'.")
    }

    @Test
    fun `formatting without a configuration file is refused`() {
        val run = Run()

        val error =
            assertFailsWith<ConfigError> {
                run.cli.run(Operation.FORMATTING, sourceFile("println(1);"))
            }

        assertContains(error.message.orEmpty(), "--config")
    }

    @Test
    fun `a setting the formatter cannot use is refused before the source is read`() {
        val run = Run(config = configFile("""{ "line-breaks-after-println": 9 }"""))

        assertFailsWith<ConfigError> {
            run.cli.run(Operation.FORMATTING, sourceFile("println(1);"))
        }

        assertEquals("", run.out.toString())
    }
}
