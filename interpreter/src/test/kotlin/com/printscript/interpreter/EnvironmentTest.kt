package com.printscript.interpreter

import com.printscript.ast.Type
import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.language.Environment
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class EnvironmentTest {
    private val span = Span.at(Position(1, 1))

    private fun errorOf(result: Result<*>) = assertIs<Failure>(result).error

    @Test
    fun `declaring a new name succeeds`() {
        val env = Environment<Value>()

        val result = env.declare("x", Type.NumberType, mutable = true, span)

        assertIs<Success<Unit>>(result)
    }

    @Test
    fun `declaring the same name twice fails`() {
        val env = Environment<Value>()
        env.declare("x", Type.NumberType, mutable = true, span)

        val error = errorOf(env.declare("x", Type.NumberType, mutable = true, span))

        assertEquals(Diagnostic.VariableAlreadyDeclared("x", span), error)
    }

    @Test
    fun `looking up an undeclared variable fails`() {
        val env = Environment<Value>()

        val error = errorOf(env.lookup("x", span))

        assertEquals(Diagnostic.VariableNotDeclared("x", span), error)
    }

    @Test
    fun `looking up a declared but unassigned variable fails`() {
        val env = Environment<Value>()
        env.declare("x", Type.NumberType, mutable = true, span)

        val error = errorOf(env.lookup("x", span))

        assertEquals(Diagnostic.VariableWithoutValue("x", span), error)
    }

    @Test
    fun `reading undeclared and reading uninitialized are different errors`() {
        val env = Environment<Value>()
        env.declare("declared", Type.NumberType, mutable = true, span)

        val undeclared = errorOf(env.lookup("missing", span))
        val uninitialized = errorOf(env.lookup("declared", span))

        assertNotEquals(undeclared, uninitialized)
    }

    @Test
    fun `initialize binds a value that can then be looked up`() {
        val env = Environment<Value>()
        env.declare("x", Type.NumberType, mutable = true, span)

        val initialized =
            env.initialize(
                "x",
                Value.NumberValue(5.0),
                Value.NumberValue(5.0).type,
                span,
            )
        val looked = env.lookup("x", span)

        assertIs<Success<Unit>>(initialized)
        assertEquals(Value.NumberValue(5.0), assertIs<Success<Value>>(looked).value)
    }

    @Test
    fun `initializing an undeclared variable fails`() {
        val env = Environment<Value>()

        val error =
            errorOf(env.initialize("x", Value.NumberValue(5.0), Value.NumberValue(5.0).type, span))

        assertEquals(Diagnostic.VariableNotDeclared("x", span), error)
    }

    @Test
    fun `assign updates the value of a bound variable`() {
        val env = Environment<Value>()
        env.declare("x", Type.NumberType, mutable = true, span)
        env.initialize("x", Value.NumberValue(1.0), Value.NumberValue(1.0).type, span)

        val assigned = env.assign("x", Value.NumberValue(2.0), Value.NumberValue(2.0).type, span)

        assertIs<Success<Unit>>(assigned)
        assertEquals(
            Value.NumberValue(2.0),
            assertIs<Success<Value>>(env.lookup("x", span)).value,
        )
    }

    @Test
    fun `assign to a declared but unassigned variable binds it`() {
        val env = Environment<Value>()
        env.declare("x", Type.NumberType, mutable = true, span)

        val result = env.assign("x", Value.NumberValue(7.0), Value.NumberValue(7.0).type, span)

        assertIs<Success<Unit>>(result)
        assertEquals(
            Value.NumberValue(7.0),
            assertIs<Success<Value>>(env.lookup("x", span)).value,
        )
    }

    @Test
    fun `assigning an undeclared variable fails`() {
        val env = Environment<Value>()

        val error =
            errorOf(env.assign("x", Value.NumberValue(5.0), Value.NumberValue(5.0).type, span))

        assertEquals(Diagnostic.VariableNotDeclared("x", span), error)
    }

    @Test
    fun `assigning a value of a different type fails`() {
        val env = Environment<Value>()
        env.declare("x", Type.NumberType, mutable = true, span)

        val error =
            errorOf(
                env.assign("x", Value.StringValue("nope"), Value.StringValue("nope").type, span),
            )

        assertEquals(
            Diagnostic.IncompatibleAssignment(
                name = "x",
                declared = Type.NumberType,
                actual = Type.StringType,
                span = span,
            ),
            error,
        )
    }

    @Test
    fun `initializing with a different type fails`() {
        val env = Environment<Value>()
        env.declare("s", Type.StringType, mutable = true, span)

        val error =
            errorOf(env.initialize("s", Value.NumberValue(3.0), Value.NumberValue(3.0).type, span))

        assertEquals(
            Diagnostic.IncompatibleAssignment(
                name = "s",
                declared = Type.StringType,
                actual = Type.NumberType,
                span = span,
            ),
            error,
        )
    }

    @Test
    fun `an error carries the span it was reported over`() {
        val env = Environment<Value>()
        val where = Span(Position(7, 12), Position(7, 13))

        val error = errorOf(env.lookup("x", where))

        assertEquals(where, error.span)
    }
}
