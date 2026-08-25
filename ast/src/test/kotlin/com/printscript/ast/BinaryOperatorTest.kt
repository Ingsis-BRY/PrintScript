package com.printscript.ast

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BinaryOperatorTest {

    @Test
    fun `addition is a BinaryOperator`() {
        assertEquals(BinaryOperator.Addition, BinaryOperator.Addition)
    }

    @Test
    fun `subtraction is a BinaryOperator`() {
        assertEquals(BinaryOperator.Subtraction, BinaryOperator.Subtraction)
    }

    @Test
    fun `multiplication is a BinaryOperator`() {
        assertEquals(BinaryOperator.Multiplication, BinaryOperator.Multiplication)
    }

    @Test
    fun `division is a BinaryOperator`() {
        assertEquals(BinaryOperator.Division, BinaryOperator.Division)
    }

    @Test
    fun `all operators are valid BinaryOperators`() {
        val operators: List<BinaryOperator> = listOf(
            BinaryOperator.Addition,
            BinaryOperator.Subtraction,
            BinaryOperator.Multiplication,
            BinaryOperator.Division
        )

        assertEquals(4, operators.size)
    }
}