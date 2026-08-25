package com.printscript.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PositionTest {
    @Test
    fun `should create position with line and column`() {
        val position = Position(3, 7)

        assertEquals(3, position.line)
        assertEquals(7, position.column)
    }

    @Test
    fun `should expose line and column properties`() {
        val position = Position(line = 10, column = 20)

        assertEquals(10, position.line)
        assertEquals(20, position.column)
    }

    @Test
    fun `positions with same line and column should be equal`() {
        val first = Position(3, 7)
        val second = Position(3, 7)

        assertEquals(first, second)
    }

    @Test
    fun `positions with different line or column should not be equal`() {
        val position = Position(3, 7)

        assertNotEquals(position, Position(4, 7))
        assertNotEquals(position, Position(3, 8))
    }
}
