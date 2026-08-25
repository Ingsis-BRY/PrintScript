package com.printscript.ast

import com.printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatementTest {

    private val start = Position(1, 1)
    private val end = Position(1, 10)

    @Test
    fun `variable declaration should expose its data`() {
        val initializer = Expression.NumberLiteral(
            value = 42.0,
            start = Position(1, 17),
            end = Position(1, 19)
        )

        val statement = Statement.VariableDeclaration(
            name = "x",
            declaredType = Type.NumberType,
            initializer = initializer,
            start = start,
            end = end
        )

        assertEquals("x", statement.name)
        assertEquals(Type.NumberType, statement.declaredType)
        assertEquals(initializer, statement.initializer)
        assertEquals(start, statement.start)
        assertEquals(end, statement.end)
    }

    @Test
    fun `variable declaration should allow missing initializer`() {
        val statement = Statement.VariableDeclaration(
            name = "x",
            declaredType = Type.NumberType,
            initializer = null,
            start = start,
            end = end
        )

        assertEquals("x", statement.name)
        assertEquals(Type.NumberType, statement.declaredType)
        assertNull(statement.initializer)
        assertEquals(start, statement.start)
        assertEquals(end, statement.end)
    }

    @Test
    fun `assignment should expose its data`() {
        val value = Expression.NumberLiteral(
            value = 42.0,
            start = Position(1, 5),
            end = Position(1, 7)
        )

        val statement = Statement.Assignment(
            name = "x",
            value = value,
            start = start,
            end = end
        )

        assertEquals("x", statement.name)
        assertEquals(value, statement.value)
        assertEquals(start, statement.start)
        assertEquals(end, statement.end)
    }

    @Test
    fun `call statement should expose its data`() {
        val argument = Expression.VariableReference(
            name = "x",
            start = Position(1, 9),
            end = Position(1, 10)
        )

        val statement = Statement.CallStatement(
            callee = "println",
            argument = argument,
            start = start,
            end = end
        )

        assertEquals("println", statement.callee)
        assertEquals(argument, statement.argument)
        assertEquals(start, statement.start)
        assertEquals(end, statement.end)
    }

    @Test
    fun `statements should support structural equality`() {
        val first = Statement.VariableDeclaration(
            name = "x",
            declaredType = Type.NumberType,
            initializer = null,
            start = start,
            end = end
        )

        val second = Statement.VariableDeclaration(
            name = "x",
            declaredType = Type.NumberType,
            initializer = null,
            start = start,
            end = end
        )

        assertEquals(first, second)
    }
}
