package com.printscript.language

import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NumberCodecTest {

    private val position = Position(1, 1)

    @Test
    fun `parse integer`() {
        val result = NumberCodec.parse(
            text = "12",
            start = position,
            end = position
        )

        assertIs<Success<Double>>(result)
        assertEquals(12.0, result.value)
    }

    @Test
    fun `parse decimal`() {
        val result = NumberCodec.parse(
            text = "12.5",
            start = position,
            end = position
        )

        assertIs<Success<Double>>(result)
        assertEquals(12.5, result.value)
    }

    @Test
    fun `parse malformed number`() {
        val start = Position(1, 1)
        val end = Position(1, 5)

        val result = NumberCodec.parse(
            text = "1.2.3",
            start = start,
            end = end
        )

        assertIs<Failure>(result)
        assertEquals(start, result.error.position)
    }

    @Test
    fun `render 3 point 0 as 3`() {
        assertEquals("3", NumberCodec.render(3.0))
    }

    @Test
    fun `render decimal`() {
        assertEquals("3.14", NumberCodec.render(3.14))
    }

    @Test
    fun `render negative integer without decimal`() {
        assertEquals("-3", NumberCodec.render(-3.0))
    }
}