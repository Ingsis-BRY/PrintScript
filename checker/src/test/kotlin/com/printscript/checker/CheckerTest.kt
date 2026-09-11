package com.printscript.checker

import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.language.Environment
import com.printscript.lexer.Lexer
import com.printscript.lexer.StringSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.Parser
import com.printscript.parser.expression.PrefixParselets
import com.printscript.parser.syntax.StatementSyntaxes
import com.printscript.pipeline.StatementBoundaries
import com.printscript.pipeline.StatementStream
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CheckerTest {
    private fun checker() =
        Checker(
            globalScope = Environment(),
            signatures = FunctionSignatures.V1_1,
            builtins = setOf("println"),
        )

    private fun statementsOf(source: String): List<Statement> {
        val lexer = Lexer(StringSourceReader(source), TokenRecognizers.V1_1)
        val stream =
            StatementStream(
                source = lexer::tokens,
                parser = Parser(StatementSyntaxes.V1_1, PrefixParselets.V1_1)::parse,
                boundary = StatementBoundaries.V1_1,
            )

        val statements = mutableListOf<Statement>()

        while (stream.hasNext()) {
            statements.add(assertIs<Success<Statement>>(stream.next()).value)
        }

        return statements
    }

    private fun check(source: String): Result<Unit> {
        val checker = checker()

        for (statement in statementsOf(source)) {
            val checked = checker.check(statement)

            if (checked is Failure) {
                return checked
            }
        }

        return Success(Unit)
    }

    private fun accepts(source: String) =
        assertIs<Success<Unit>>(check(source), "should have been accepted: $source")

    private fun rejects(source: String): Diagnostic =
        assertIs<Failure>(check(source), "should have been rejected: $source").error

    @Test
    fun `a program that types checks is accepted`() {
        accepts(
            """
            const yes: boolean = true;
            let total: number = 1;
            if (yes) {
                total = total + 41;
            }
            println("total: " + total);
            """.trimIndent(),
        )
    }

    @Test
    fun `nothing is executed while checking`() {
        accepts("let n: number = 1 / 0;")
    }

    @Test
    fun `an initializer of the wrong type is rejected`() {
        val error =
            assertIs<Diagnostic.IncompatibleAssignment>(rejects("""let x: number = "hola";"""))

        assertEquals("x", error.name)
    }

    @Test
    fun `a declaration with no initializer is accepted`() {
        accepts("let x: number;")
    }

    @Test
    fun `declaring the same name twice is rejected`() {
        assertIs<Diagnostic.VariableAlreadyDeclared>(
            rejects("let x: number = 1;\nlet x: number = 2;"),
        )
    }

    @Test
    fun `assigning to a name that was never declared is rejected`() {
        assertIs<Diagnostic.VariableNotDeclared>(rejects("x = 5;"))
    }

    @Test
    fun `reassigning a constant is rejected`() {
        val error =
            assertIs<Diagnostic.ConstantReassignment>(
                rejects("const b: number = 5;\nb = 2;"),
            )

        assertEquals("b", error.name)
    }

    @Test
    fun `assigning the wrong type is rejected`() {
        assertIs<Diagnostic.IncompatibleAssignment>(
            rejects("""let x: number = 1;${"\n"}x = "hola";"""),
        )
    }

    @Test
    fun `reading a name that has no value yet is rejected`() {
        assertIs<Diagnostic.VariableWithoutValue>(rejects("let x: number;\nprintln(x);"))
    }

    @Test
    fun `an operator applied to types it does not accept is rejected`() {
        val error =
            assertIs<Diagnostic.IncompatibleOperands>(rejects("""let r: string = "s" * 5;"""))

        assertEquals(Type.StringType, error.left)
        assertEquals(Type.NumberType, error.right)
    }

    @Test
    fun `a string and a number concatenate into a string`() {
        accepts("""let r: string = "n: " + 1;""")
    }

    @Test
    fun `a condition that is not a boolean is rejected`() {
        assertIs<Diagnostic.NonBooleanCondition>(
            rejects("let a: number = 1;\nif (a) {\n}"),
        )
    }

    @Test
    fun `a block is checked, and its names do not escape it`() {
        assertIs<Diagnostic.IncompatibleAssignment>(
            rejects(
                """
                const yes: boolean = true;
                if (yes) {
                    let inner: number = "hola";
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `a name declared inside a block is gone after it`() {
        assertIs<Diagnostic.VariableNotDeclared>(
            rejects(
                """
                const yes: boolean = true;
                if (yes) {
                    let inner: number = 1;
                }
                println(inner);
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `the else block is checked too`() {
        assertIs<Diagnostic.VariableNotDeclared>(
            rejects(
                """
                const yes: boolean = true;
                if (yes) {
                } else {
                    missing = 1;
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `a builtin the language does not have is rejected`() {
        val checker = checker()
        val call = statementsOf("println(1);").single()
        val unknown =
            assertIs<Statement.CallStatement>(call).copy(callee = "print")

        assertIs<Diagnostic.UnknownFunction>(
            assertIs<Failure>(checker.check(unknown)).error,
        )
    }

    @Test
    fun `readInput given something that is not a string is rejected`() {
        val error =
            assertIs<Diagnostic.IncompatibleArgument>(
                rejects("let a: string = readInput(5);"),
            )

        assertEquals("readInput", error.name)
    }

    @Test
    fun `readInput fills the type of the variable it is assigned to`() {
        accepts("""let n: number = readInput("How many?");""")
        accepts("""let f: boolean = readEnv("FLAG");""")
    }

    @Test
    fun `readInput inside a println is read as a string`() {
        accepts("""println(readInput("Say:"));""")
    }

    @Test
    fun `1 point 0 has no value functions, so a call to one is unknown`() {
        val checker =
            Checker(
                globalScope = Environment(),
                signatures = FunctionSignatures.NONE,
                builtins = setOf("println"),
            )

        val declaration = statementsOf("""let a: string = readInput("x");""").single()

        assertIs<Diagnostic.UnknownFunction>(
            assertIs<Failure>(checker.check(declaration)).error,
        )
    }

    @Test
    fun `the 1 point 1 catalog names both readers`() {
        assertEquals(setOf("readInput", "readEnv"), FunctionSignatures.V1_1.keys)
    }
}
