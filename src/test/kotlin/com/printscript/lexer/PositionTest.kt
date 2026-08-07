package com.printscript.lexer

import kotlin.test.Test
import kotlin.test.assertEquals

class PositionTest {

    @Test
    fun `should create position with default values`() {
        val position = Position()

        assertEquals(1, position.line)
        assertEquals(1, position.column)
        assertEquals(0, position.offset)
    }

    @Test
    fun `should create position with specified values`() {
        val position = Position(
            line = 5,
            column = 12,
            offset = 47
        )

        assertEquals(5, position.line)
        assertEquals(12, position.column)
        assertEquals(47, position.offset)
    }
}