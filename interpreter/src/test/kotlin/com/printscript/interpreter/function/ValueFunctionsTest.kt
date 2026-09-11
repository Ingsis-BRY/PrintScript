package com.printscript.interpreter.function

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.interpreter.EnvironmentSource
import com.printscript.interpreter.InputProvider
import com.printscript.interpreter.NoInput
import com.printscript.interpreter.Value
import com.printscript.interpreter.executor.ExecutionContext
import com.printscript.language.Environment
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ValueFunctionsTest {
    private val span = Span.at(Position(1, 1))

    private class RecordingContext : ExecutionContext {
        val emitted = mutableListOf<String>()

        override val environment = Environment<Value>()

        override fun evaluate(
            expression: Expression,
            expected: Type?,
        ): Result<Value> = Success(Value.NumberValue(0.0))

        override fun executeBlock(statements: List<Statement>): Result<Unit> = Success(Unit)

        override fun emit(line: String) {
            emitted.add(line)
        }
    }

    private fun readInput(vararg answers: String): ValueFunction {
        val remaining = ArrayDeque(answers.toList())

        return functions(InputProvider { remaining.removeFirstOrNull() })["readInput"]!!
    }

    private fun readEnv(vararg variables: Pair<String, String>): ValueFunction {
        val map = variables.toMap()

        return functions(NoInput, EnvironmentSource { map[it] })["readEnv"]!!
    }

    private fun functions(
        input: InputProvider,
        environment: EnvironmentSource = EnvironmentSource { null },
    ): Map<String, ValueFunction> = ValueFunctions.readers(input, environment)

    private fun call(
        function: ValueFunction,
        argument: String,
        expected: Type?,
        context: ExecutionContext = RecordingContext(),
    ): Result<Value> = function.call(Value.StringValue(argument), expected, span, context)

    @Test
    fun `the empty catalog, which is the one 1 point 0 gets, has nothing`() {
        assertTrue(ValueFunctions.NONE.isEmpty())
    }

    @Test
    fun `1 point 1 has readInput and readEnv`() {
        assertEquals(
            setOf("readInput", "readEnv"),
            functions(NoInput).keys,
        )
    }

    @Test
    fun `readInput emits its prompt before asking for the value`() {
        val context = RecordingContext()

        call(readInput("world"), "Name:", Type.StringType, context)

        assertEquals(listOf("Name:"), context.emitted)
    }

    @Test
    fun `readInput yields the line it was given`() {
        assertEquals(
            Success(Value.StringValue("world")),
            call(readInput("world"), "Name:", Type.StringType),
        )
    }

    @Test
    fun `readInput reads a number back as a number`() {
        assertEquals(
            Success(Value.NumberValue(42.0)),
            call(readInput("42"), "How many?", Type.NumberType),
        )
    }

    @Test
    fun `readInput reads a boolean back as a boolean`() {
        assertEquals(
            Success(Value.BooleanValue(true)),
            call(readInput("true"), "Sure?", Type.BooleanType),
        )
    }

    @Test
    fun `with nowhere in particular to go the value stays a string`() {
        assertEquals(
            Success(Value.StringValue("42")),
            call(readInput("42"), "Say:", expected = null),
        )
    }

    @Test
    fun `a value that cannot be read as the expected type is reported`() {
        val error =
            assertIs<Failure>(call(readInput("Hola"), "Sure?", Type.BooleanType)).error

        val uninterpretable = assertIs<Diagnostic.UninterpretableInput>(error)

        assertEquals("Hola", uninterpretable.text)
        assertEquals(Type.BooleanType, uninterpretable.expected)
        assertEquals(span, uninterpretable.span)
    }

    @Test
    fun `a number that is not one is reported`() {
        assertIs<Diagnostic.UninterpretableInput>(
            assertIs<Failure>(call(readInput("many"), "How many?", Type.NumberType)).error,
        )
    }

    @Test
    fun `surrounding blanks do not stop a value from being read`() {
        assertEquals(
            Success(Value.NumberValue(42.0)),
            call(readInput("  42  "), "How many?", Type.NumberType),
        )
    }

    @Test
    fun `a blank answer is a perfectly good string`() {
        assertEquals(
            Success(Value.StringValue("")),
            call(readInput(""), "Name:", Type.StringType),
        )
    }

    @Test
    fun `readInput with nothing left to read is reported`() {
        val error = assertIs<Failure>(call(readInput(), "Name:", Type.StringType)).error

        assertEquals(span, assertIs<Diagnostic.MissingInput>(error).span)
    }

    @Test
    fun `each call takes the next answer`() {
        val function = readInput("first", "second")

        assertEquals(Success(Value.StringValue("first")), call(function, "a", Type.StringType))
        assertEquals(Success(Value.StringValue("second")), call(function, "b", Type.StringType))
    }

    @Test
    fun `readEnv yields the value the environment has`() {
        assertEquals(
            Success(Value.StringValue("San Lorenzo")),
            call(readEnv("CLUB" to "San Lorenzo"), "CLUB", Type.StringType),
        )
    }

    @Test
    fun `readEnv prints nothing`() {
        val context = RecordingContext()

        call(readEnv("CLUB" to "San Lorenzo"), "CLUB", Type.StringType, context)

        assertTrue(context.emitted.isEmpty(), "readEnv has no prompt to print")
    }

    @Test
    fun `readEnv reads its value back as the expected type`() {
        assertEquals(
            Success(Value.NumberValue(8080.0)),
            call(readEnv("PORT" to "8080"), "PORT", Type.NumberType),
        )
    }

    @Test
    fun `a variable the environment does not define is reported`() {
        val error = assertIs<Failure>(call(readEnv(), "NOT_SET", Type.StringType)).error

        assertEquals("NOT_SET", assertIs<Diagnostic.MissingEnvironmentVariable>(error).name)
    }

    @Test
    fun `an environment value that cannot be read as the expected type is reported`() {
        assertIs<Diagnostic.UninterpretableInput>(
            assertIs<Failure>(call(readEnv("CLUB" to "hola"), "CLUB", Type.BooleanType)).error,
        )
    }

    @Test
    fun `the empty provider always has nothing`() {
        assertEquals(null, NoInput.read())
    }
}
