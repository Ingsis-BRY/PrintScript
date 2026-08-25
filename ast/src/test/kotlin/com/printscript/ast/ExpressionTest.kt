package com.printscript.ast

import com.printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpressionTest {

    @Test
    fun `number literal should expose value and position`() {
        val start = Position(1, 1)
        val end = Position(1, 3)

        val expression = Expression.NumberLiteral(
            value = 42.0,
            start = start,
            end = end
        )

        assertEquals(42.0, expression.value)
        assertEquals(start, expression.start)
        assertEquals(end, expression.end)
    }

    @Test
    fun `string literal should expose value and position`() {
        val start = Position(1, 1)
        val end = Position(1, 8)

        val expression = Expression.StringLiteral(
            value = "hello",
            start = start,
            end = end
        )

        assertEquals("hello", expression.value)
        assertEquals(start, expression.start)
        assertEquals(end, expression.end)
    }

    @Test
    fun `variable reference should expose name and position`() {
        val start = Position(2, 5)
        val end = Position(2, 9)

        val expression = Expression.VariableReference(
            name = "value",
            start = start,
            end = end
        )

        assertEquals("value", expression.name)
        assertEquals(start, expression.start)
        assertEquals(end, expression.end)
    }

    @Test
    fun `binary expression should expose its children operator and position`() {
        val left = Expression.NumberLiteral(
            value = 10.0,
            start = Position(1, 1),
            end = Position(1, 2)
        )

        val right = Expression.NumberLiteral(
            value = 20.0,
            start = Position(1, 5),
            end = Position(1, 6)
        )

        val expression = Expression.BinaryExpression(
            left = left,
            operator = BinaryOperator.Addition,
            right = right,
            start = Position(1, 1),
            end = Position(1, 6)
        )

        assertEquals(left, expression.left)
        assertEquals(BinaryOperator.Addition, expression.operator)
        assertEquals(right, expression.right)
        assertEquals(Position(1, 1), expression.start)
        assertEquals(Position(1, 6), expression.end)
    }

    @Test
    fun `nested binary expression should have position covering its children`() {
        val left = number(10.0, 1, 2)

        val right = Expression.BinaryExpression(
            left = number(20.0, 5, 6),
            operator = BinaryOperator.Multiplication,
            right = number(30.0, 9, 10),
            start = Position(1, 5),
            end = Position(1, 10)
        )

        val expression = Expression.BinaryExpression(
            left = left,
            operator = BinaryOperator.Addition,
            right = right,
            start = Position(1, 1),
            end = Position(1, 10)
        )

        assertEquals(Position(1, 1), expression.start)
        assertEquals(Position(1, 10), expression.end)
        assertEquals(Position(1, 1), expression.left.start)
        assertEquals(Position(1, 2), expression.left.end)
        assertEquals(Position(1, 5), expression.right.start)
        assertEquals(Position(1, 10), expression.right.end)
    }

    private fun number(value: Double, start: Int, end: Int) =
        Expression.NumberLiteral(
            value = value,
            start = Position(1, start),
            end = Position(1, end)
        )
}
