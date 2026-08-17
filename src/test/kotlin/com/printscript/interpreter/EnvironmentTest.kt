package com.printscript.interpreter

import com.printscript.ast.Type
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Result
import com.printscript.common.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class EnvironmentTest {

    private val position = Position(1, 1)

    private fun errorOf(result: Result<*>) = assertIs<Failure>(result).error

    @Test
    fun `declaring a new name succeeds`() {
        val env = Environment()

        val result = env.declare("x", Type.NumberType, position)

        assertIs<Success<Unit>>(result)
    }

    @Test
    fun `declaring the same name twice fails`() {
        val env = Environment()
        env.declare("x", Type.NumberType, position)

        val error = errorOf(env.declare("x", Type.NumberType, position))

        assertEquals("Variable 'x' is already declared.", error.message)
    }

    @Test
    fun `looking up an undeclared variable fails`() {
        val env = Environment()

        val error = errorOf(env.lookup("x", position))

        assertEquals("Variable 'x' is not declared.", error.message)
    }

    @Test
    fun `looking up a declared but unassigned variable fails`() {
        val env = Environment()
        env.declare("x", Type.NumberType, position)

        val error = errorOf(env.lookup("x", position))

        assertEquals("Variable 'x' is used before it has a value.", error.message)
    }

    @Test
    fun `reading undeclared and reading uninitialized are different errors`() {
        val env = Environment()
        env.declare("declared", Type.NumberType, position)

        val undeclared = errorOf(env.lookup("missing", position))
        val uninitialized = errorOf(env.lookup("declared", position))

        assertNotEquals(undeclared.message, uninitialized.message)
    }

    @Test
    fun `initialize binds a value that can then be looked up`() {
        val env = Environment()
        env.declare("x", Type.NumberType, position)

        val initialized = env.initialize("x", Value.NumberValue(5.0), position)
        val looked = env.lookup("x", position)

        assertIs<Success<Unit>>(initialized)
        assertEquals(Value.NumberValue(5.0), assertIs<Success<Value>>(looked).value)
    }

    @Test
    fun `initializing an undeclared variable fails`() {
        val env = Environment()

        val error = errorOf(env.initialize("x", Value.NumberValue(5.0), position))

        assertEquals("Variable 'x' is not declared.", error.message)
    }

    @Test
    fun `assign updates the value of a bound variable`() {
        val env = Environment()
        env.declare("x", Type.NumberType, position)
        env.initialize("x", Value.NumberValue(1.0), position)

        val assigned = env.assign("x", Value.NumberValue(2.0), position)

        assertIs<Success<Unit>>(assigned)
        assertEquals(
            Value.NumberValue(2.0),
            assertIs<Success<Value>>(env.lookup("x", position)).value
        )
    }

    @Test
    fun `assign to a declared but unassigned variable binds it`() {
        val env = Environment()
        env.declare("x", Type.NumberType, position)

        val result = env.assign("x", Value.NumberValue(7.0), position)

        assertIs<Success<Unit>>(result)
        assertEquals(
            Value.NumberValue(7.0),
            assertIs<Success<Value>>(env.lookup("x", position)).value
        )
    }

    @Test
    fun `assigning an undeclared variable fails`() {
        val env = Environment()

        val error = errorOf(env.assign("x", Value.NumberValue(5.0), position))

        assertEquals("Variable 'x' is not declared.", error.message)
    }

    @Test
    fun `assigning a value of a different type fails`() {
        val env = Environment()
        env.declare("x", Type.NumberType, position)

        val error = errorOf(env.assign("x", Value.StringValue("nope"), position))

        assertEquals(
            "Cannot assign a string value to variable 'x' of type number.",
            error.message
        )
    }

    @Test
    fun `initializing with a different type fails`() {
        val env = Environment()
        env.declare("s", Type.StringType, position)

        val error = errorOf(env.initialize("s", Value.NumberValue(3.0), position))

        assertEquals(
            "Cannot assign a number value to variable 's' of type string.",
            error.message
        )
    }

    @Test
    fun `a diagnostic carries the position it was reported at`() {
        val env = Environment()
        val where = Position(7, 12)

        val error = errorOf(env.lookup("x", where))

        assertEquals(where, error.position)
    }
}
