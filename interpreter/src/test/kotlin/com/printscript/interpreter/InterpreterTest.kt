package com.printscript.interpreter

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.common.Position
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InterpreterTest {
    private val at = Position(1, 1)

    private fun number(value: Double) = Expression.NumberLiteral(value, at, at)

    private fun string(value: String) = Expression.StringLiteral(value, at, at)

    private fun reference(name: String) = Expression.VariableReference(name, at, at)

    private fun binary(
        left: Expression,
        operator: BinaryOperator,
        right: Expression,
    ) = Expression.BinaryExpression(left, operator, right, at, at)

    private fun declare(
        name: String,
        type: Type,
        initializer: Expression?,
    ) = Statement.VariableDeclaration(name, type, initializer, at, at)

    private fun assign(
        name: String,
        value: Expression,
    ) = Statement.Assignment(name, value, at, at)

    private fun println(argument: Expression) = Statement.CallStatement("println", argument, at, at)

    // runs every statement in order and returns the collected output; fails the
    // test if any statement does not succeed
    private fun run(vararg statements: Statement): List<String> {
        val output = CollectingOutput()
        val interpreter = Interpreter(Environment(), output, ValueOps())

        statements.forEach { statement ->
            assertIs<Success<Unit>>(interpreter.execute(statement))
        }

        return output.lines()
    }

    @Test
    fun `example 1 concatenates strings`() {
        val output =
            run(
                declare("name", Type.StringType, string("Joe")),
                declare("lastName", Type.StringType, string("Doe")),
                println(
                    binary(
                        binary(reference("name"), BinaryOperator.Addition, string(" ")),
                        BinaryOperator.Addition,
                        reference("lastName"),
                    ),
                ),
            )

        assertEquals(listOf("Joe Doe"), output)
    }

    @Test
    fun `example 2 divides and prints an integer result`() {
        val output =
            run(
                declare("a", Type.NumberType, number(12.0)),
                declare("b", Type.NumberType, number(4.0)),
                declare(
                    "c",
                    Type.NumberType,
                    binary(reference("a"), BinaryOperator.Division, reference("b")),
                ),
                println(binary(string("Result: "), BinaryOperator.Addition, reference("c"))),
            )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `example 3 reassigns before printing`() {
        val output =
            run(
                declare("a", Type.NumberType, number(12.0)),
                declare("b", Type.NumberType, number(4.0)),
                assign("a", binary(reference("a"), BinaryOperator.Division, reference("b"))),
                println(binary(string("Result: "), BinaryOperator.Addition, reference("a"))),
            )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `a declaration without initializer leaves the variable unassigned`() {
        val output = CollectingOutput()
        val interpreter = Interpreter(Environment(), output, ValueOps())

        assertIs<Success<Unit>>(interpreter.execute(declare("x", Type.NumberType, null)))

        val result = interpreter.execute(println(reference("x")))
        assertIs<Failure>(result)
    }

    @Test
    fun `division by zero is a runtime error`() {
        val interpreter = Interpreter(Environment(), CollectingOutput(), ValueOps())

        val declared = interpreter.execute(declare("a", Type.NumberType, number(1.0)))
        assertIs<Success<Unit>>(declared)

        val result =
            interpreter.execute(
                declare(
                    "b",
                    Type.NumberType,
                    binary(reference("a"), BinaryOperator.Division, number(0.0)),
                ),
            )

        val error = assertIs<Failure>(result).error
        assertIs<Diagnostic.DivisionByZero>(error)
    }

    @Test
    fun `referencing an undeclared variable is an error`() {
        val interpreter = Interpreter(Environment(), CollectingOutput(), ValueOps())

        val result = interpreter.execute(println(reference("missing")))

        val error = assertIs<Failure>(result).error
        assertEquals("missing", assertIs<Diagnostic.VariableNotDeclared>(error).name)
    }

    @Test
    fun `println of a number renders it through NumberCodec`() {
        val output =
            run(
                declare("n", Type.NumberType, number(3.0)),
                println(reference("n")),
            )

        assertEquals(listOf("3"), output)
    }

    @Test
    fun `concatenating a string and a number yields a string`() {
        val output =
            run(
                declare("count", Type.NumberType, number(5.0)),
                println(binary(string("count: "), BinaryOperator.Addition, reference("count"))),
            )

        assertEquals(listOf("count: 5"), output)
    }

    @Test
    fun `calling an unknown function is an error`() {
        val interpreter = Interpreter(Environment(), CollectingOutput(), ValueOps())

        val result: Result<Unit> =
            interpreter.execute(Statement.CallStatement("print", number(1.0), at, at))

        val error = assertIs<Failure>(result).error
        assertEquals("print", assertIs<Diagnostic.UnknownFunction>(error).name)
    }
}
