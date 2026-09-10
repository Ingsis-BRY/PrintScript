package com.printscript.interpreter

import com.printscript.ast.Type
import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScopeAndConstTest {
    private val span = Span.at(Position(1, 1))

    private fun environmentWith(
        name: String,
        value: Value,
        mutable: Boolean,
    ): Environment {
        val environment = Environment()

        assertIs<Success<Unit>>(environment.declare(name, value.type, mutable, span))
        assertIs<Success<Unit>>(environment.initialize(name, value, span))

        return environment
    }

    @Test
    fun `a constant takes its first value`() {
        val environment = environmentWith("a", Value.NumberValue(5.0), mutable = false)

        assertEquals(Success(Value.NumberValue(5.0)), environment.lookup("a", span))
    }

    @Test
    fun `a constant refuses a second value`() {
        val environment = environmentWith("a", Value.NumberValue(5.0), mutable = false)

        val error = assertIs<Failure>(environment.assign("a", Value.NumberValue(2.0), span)).error

        assertEquals("a", assertIs<Diagnostic.ConstantReassignment>(error).name)
    }

    @Test
    fun `a variable takes a second value`() {
        val environment = environmentWith("a", Value.NumberValue(5.0), mutable = true)

        assertIs<Success<Unit>>(environment.assign("a", Value.NumberValue(2.0), span))
        assertEquals(Success(Value.NumberValue(2.0)), environment.lookup("a", span))
    }

    @Test
    fun `reassignment is refused before the type is even checked`() {
        val environment = environmentWith("a", Value.NumberValue(5.0), mutable = false)

        val error = assertIs<Failure>(environment.assign("a", Value.StringValue("x"), span)).error

        assertIs<Diagnostic.ConstantReassignment>(error)
    }

    @Test
    fun `a constant declared in a block is still a constant there`() {
        val environment = Environment()
        val block = environment.child()

        assertIs<Success<Unit>>(block.declare("a", Type.NumberType, mutable = false, span))
        assertIs<Success<Unit>>(block.initialize("a", Value.NumberValue(1.0), span))

        assertIs<Failure>(block.assign("a", Value.NumberValue(2.0), span))
    }

    @Test
    fun `a block reads a name from the scope around it`() {
        val environment = environmentWith("a", Value.NumberValue(1.0), mutable = true)

        assertEquals(Success(Value.NumberValue(1.0)), environment.child().lookup("a", span))
    }

    @Test
    fun `a block writes to the scope that declared the name`() {
        val environment = environmentWith("a", Value.NumberValue(1.0), mutable = true)

        assertIs<Success<Unit>>(environment.child().assign("a", Value.NumberValue(2.0), span))
        assertEquals(Success(Value.NumberValue(2.0)), environment.lookup("a", span))
    }

    @Test
    fun `a name declared in a block is not visible outside it`() {
        val environment = Environment()
        val block = environment.child()

        assertIs<Success<Unit>>(block.declare("a", Type.NumberType, mutable = true, span))

        val error = assertIs<Failure>(environment.lookup("a", span)).error

        assertIs<Diagnostic.VariableNotDeclared>(error)
    }

    @Test
    fun `a block can shadow a name the scope around it has`() {
        val environment = environmentWith("a", Value.NumberValue(1.0), mutable = true)
        val block = environment.child()

        assertIs<Success<Unit>>(block.declare("a", Type.StringType, mutable = true, span))
        assertIs<Success<Unit>>(block.initialize("a", Value.StringValue("inner"), span))

        assertEquals(Success(Value.StringValue("inner")), block.lookup("a", span))
        assertEquals(Success(Value.NumberValue(1.0)), environment.lookup("a", span))
    }

    @Test
    fun `declaring the same name twice in one scope is still refused`() {
        val environment = Environment()

        assertIs<Success<Unit>>(environment.declare("a", Type.NumberType, mutable = true, span))

        val error =
            assertIs<Failure>(
                environment.declare("a", Type.NumberType, mutable = true, span),
            ).error

        assertIs<Diagnostic.VariableAlreadyDeclared>(error)
    }

    @Test
    fun `scopes nest more than one deep`() {
        val environment = environmentWith("a", Value.NumberValue(1.0), mutable = true)

        assertEquals(
            Success(Value.NumberValue(1.0)),
            environment.child().child().lookup("a", span),
        )
    }

    @Test
    fun `the declared type of a name is readable through the scopes`() {
        val environment = Environment()

        assertIs<Success<Unit>>(environment.declare("a", Type.BooleanType, mutable = true, span))

        assertEquals(Type.BooleanType, environment.child().declaredTypeOf("a"))
    }

    @Test
    fun `a name no scope declares has no declared type`() {
        assertEquals(null, Environment().declaredTypeOf("missing"))
    }

    @Test
    fun `a name keeps its declared type after being bound`() {
        val environment = environmentWith("a", Value.BooleanValue(true), mutable = true)

        assertEquals(Type.BooleanType, environment.declaredTypeOf("a"))
    }
}
