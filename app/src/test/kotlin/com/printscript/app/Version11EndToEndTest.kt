package com.printscript.app

import com.printscript.cli.Operation
import com.printscript.interpreter.EnvironmentSource
import com.printscript.interpreter.InputProvider
import com.printscript.report.Failure
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Version11EndToEndTest {
    private fun run11(
        input: List<String> = emptyList(),
        environment: Map<String, String> = emptyMap(),
    ) = Run(
        version = "1.1",
        input = QueuedInput(input),
        environment = FixedEnvironment(environment),
    )

    private fun execute11(
        source: String,
        input: List<String> = emptyList(),
        environment: Map<String, String> = emptyMap(),
    ): List<String> {
        val run = run11(input, environment)

        assertIs<Success<Unit>>(run.cli.run(Operation.EXECUTION, sourceFile(source)))

        return run.output.lines()
    }

    private fun failure11(
        source: String,
        input: List<String> = emptyList(),
    ): String {
        val run = run11(input)

        assertIs<Failure>(run.cli.run(Operation.EXECUTION, sourceFile(source)))

        return run.errors.toString().trim()
    }

    private fun failure10(source: String): String {
        val run = Run(version = "1.0")

        assertIs<Failure>(run.cli.run(Operation.EXECUTION, sourceFile(source)))

        return run.errors.toString().trim()
    }

    @Test
    fun `a const holds its value like a let`() {
        val output =
            execute11(
                """
                const greeting: string = "hola";
                println(greeting);
                """.trimIndent(),
            )

        assertEquals(listOf("hola"), output)
    }

    @Test
    fun `a const cannot be reassigned`() {
        val error =
            failure11(
                """
                const b: number = 5;
                b = 2;
                """.trimIndent(),
            )

        assertContains(error, "Constant 'b' cannot be reassigned.")
    }

    @Test
    fun `a let declared next to a const still can be`() {
        val output =
            execute11(
                """
                const a: number = 1;
                let b: number = 2;
                b = 3;
                println(a + b);
                """.trimIndent(),
            )

        assertEquals(listOf("4"), output)
    }

    @Test
    fun `a boolean prints as its keyword`() {
        val output =
            execute11(
                """
                let flag: boolean = true;
                println(flag);
                """.trimIndent(),
            )

        assertEquals(listOf("true"), output)
    }

    @Test
    fun `a boolean cannot be given a number`() {
        val error = failure11("let flag: boolean = 1;")

        assertContains(error, "Cannot assign a number value to variable 'flag' of type boolean.")
    }

    @Test
    fun `a true condition runs the block and execution goes on after it`() {
        val output =
            execute11(
                """
                const booleanValue: boolean = true;
                if(booleanValue) {
                    println("if statement working correctly");
                }
                println("outside of conditional");
                """.trimIndent(),
            )

        assertEquals(
            listOf("if statement working correctly", "outside of conditional"),
            output,
        )
    }

    @Test
    fun `a false condition skips the block`() {
        val output =
            execute11(
                """
                const booleanValue: boolean = false;
                if(booleanValue) {
                    println("if statement is not working correctly");
                }
                println("outside of conditional");
                """.trimIndent(),
            )

        assertEquals(listOf("outside of conditional"), output)
    }

    @Test
    fun `a false condition runs the else block`() {
        val output =
            execute11(
                """
                const booleanResult: boolean = false;
                if(booleanResult) {
                    println("else statement not working correctly");
                } else {
                    println("else statement working correctly");
                }
                println("outside of conditional");
                """.trimIndent(),
            )

        assertEquals(
            listOf("else statement working correctly", "outside of conditional"),
            output,
        )
    }

    @Test
    fun `a true condition skips the else block`() {
        val output =
            execute11(
                """
                const booleanResult: boolean = true;
                if(booleanResult) {
                    println("else statement working correctly");
                } else {
                    println("else statement not working correctly");
                }
                """.trimIndent(),
            )

        assertEquals(listOf("else statement working correctly"), output)
    }

    @Test
    fun `blocks nest`() {
        val output =
            execute11(
                """
                const yes: boolean = true;
                if(yes) {
                    if(yes) {
                        println("two deep");
                    }
                }
                """.trimIndent(),
            )

        assertEquals(listOf("two deep"), output)
    }

    @Test
    fun `a condition that is not a boolean is refused`() {
        val error =
            failure11(
                """
                let a: number = 21;
                if(a) {
                    println("this should fail, invalid argument in if statement");
                }
                """.trimIndent(),
            )

        assertContains(error, "An 'if' condition must be a boolean, but this one is a number.")
    }

    @Test
    fun `there is no else if`() {
        val error =
            failure11(
                """
                const yes: boolean = true;
                if(yes) {
                    println(1);
                } else if(yes) {
                    println(2);
                }
                """.trimIndent(),
            )

        assertContains(error, "Expected '{'.")
    }

    @Test
    fun `a name declared inside a block is gone after it`() {
        val error =
            failure11(
                """
                const yes: boolean = true;
                if(yes) {
                    let inner: number = 1;
                }
                println(inner);
                """.trimIndent(),
            )

        assertContains(error, "Variable 'inner' is not declared.")
    }

    @Test
    fun `a block reads and writes the names around it`() {
        val output =
            execute11(
                """
                const yes: boolean = true;
                let total: number = 1;
                if(yes) {
                    total = total + 41;
                }
                println(total);
                """.trimIndent(),
            )

        assertEquals(listOf("42"), output)
    }

    @Test
    fun `readInput prints its prompt through the program output and reads a line`() {
        val output =
            execute11(
                """
                const name: string = readInput("Name:");
                println("Hello " + name + "!");
                """.trimIndent(),
                input = listOf("world"),
            )

        assertEquals(listOf("Name:", "Hello world!"), output)
    }

    @Test
    fun `readInput reads its value back as the type of the variable it fills`() {
        val output =
            execute11(
                """
                let amount: number = readInput("How many?");
                println(amount + 1);
                """.trimIndent(),
                input = listOf("41"),
            )

        assertEquals(listOf("How many?", "42"), output)
    }

    @Test
    fun `readInput reads a boolean back as a boolean`() {
        val output =
            execute11(
                """
                let flag: boolean = readInput("Sure?");
                if(flag) {
                    println("yes");
                }
                """.trimIndent(),
                input = listOf("true"),
            )

        assertEquals(listOf("Sure?", "yes"), output)
    }

    @Test
    fun `a value that cannot be read as the declared type fails the run`() {
        val error =
            failure11(
                "let flag: boolean = readInput(\"Sure?\");",
                input = listOf("Hola"),
            )

        assertContains(error, "Cannot read 'Hola' as a boolean.")
    }

    @Test
    fun `readInput inside a println reads its value as a string`() {
        val output =
            execute11(
                """println(readInput("Say:"));""",
                input = listOf("42"),
            )

        assertEquals(listOf("Say:", "42"), output)
    }

    @Test
    fun `readInput with nothing left to read fails the run`() {
        val error = failure11("""let a: string = readInput("Name:");""")

        assertContains(error, "No input available.")
    }

    @Test
    fun `readEnv reads a variable from the environment without prompting`() {
        val output =
            execute11(
                """
                const name: string = readEnv("BEST_FOOTBALL_CLUB");
                println("What is the best football club?");
                println(name);
                """.trimIndent(),
                environment = mapOf("BEST_FOOTBALL_CLUB" to "San Lorenzo"),
            )

        assertEquals(listOf("What is the best football club?", "San Lorenzo"), output)
    }

    @Test
    fun `readEnv on a variable the environment does not define fails the run`() {
        val error = failure11("""const a: string = readEnv("NOT_SET");""")

        assertContains(error, "Environment variable 'NOT_SET' is not defined.")
    }

    @Test
    fun `const does not exist in 1 point 0`() {
        assertTrue(failure10("const a: string = \"nope\";").isNotEmpty())
    }

    @Test
    fun `if does not exist in 1 point 0`() {
        val error =
            failure10(
                """
                let a: number = 21;
                if(a) {
                    println("if should not be supported in version 1.0");
                }
                """.trimIndent(),
            )

        assertTrue(error.isNotEmpty(), "1.0 must refuse an if statement")
    }

    @Test
    fun `boolean is not a type in 1 point 0`() {
        assertContains(failure10("let flag: boolean = true;"), "Expected a type.")
    }

    @Test
    fun `readInput does not exist in 1 point 0`() {
        assertTrue(failure10("""let a: string = readInput("Name:");""").isNotEmpty())
    }

    @Test
    fun `everything 1 point 0 runs, 1 point 1 runs too`() {
        val output =
            execute11(
                """
                let a: number = 12;
                let b: number = 4;
                a = a / b;
                println("Result: " + a);
                """.trimIndent(),
            )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `an input provider and an environment source are only asked for when used`() {
        val refusing =
            InputProvider { error("nothing should have asked for input") }
        val absent =
            EnvironmentSource { error("nothing should have asked the environment") }

        val run =
            Run(version = "1.1", input = refusing, environment = absent)

        assertIs<Success<Unit>>(
            run.cli.run(Operation.EXECUTION, sourceFile("println(1);")),
        )
    }
}
