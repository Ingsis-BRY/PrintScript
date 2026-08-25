package com.printscript.language

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OperatorRulesTest {

    @Test
    fun `addition accepts number and number`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Addition,
            Type.NumberType,
            Type.NumberType
        )

        assertEquals(Type.NumberType, result)
    }

    @Test
    fun `addition accepts string and string`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Addition,
            Type.StringType,
            Type.StringType
        )

        assertEquals(Type.StringType, result)
    }

    @Test
    fun `addition accepts string and number`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Addition,
            Type.StringType,
            Type.NumberType
        )

        assertEquals(Type.StringType, result)
    }

    @Test
    fun `addition accepts number and string`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Addition,
            Type.NumberType,
            Type.StringType
        )

        assertEquals(Type.StringType, result)
    }

    @Test
    fun `subtraction accepts number and number`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Subtraction,
            Type.NumberType,
            Type.NumberType
        )

        assertEquals(Type.NumberType, result)
    }

    @Test
    fun `subtraction rejects string and string`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Subtraction,
            Type.StringType,
            Type.StringType
        )

        assertNull(result)
    }

    @Test
    fun `subtraction rejects string and number`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Subtraction,
            Type.StringType,
            Type.NumberType
        )

        assertNull(result)
    }

    @Test
    fun `subtraction rejects number and string`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Subtraction,
            Type.NumberType,
            Type.StringType
        )

        assertNull(result)
    }

    @Test
    fun `multiplication accepts number and number`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Multiplication,
            Type.NumberType,
            Type.NumberType
        )

        assertEquals(Type.NumberType, result)
    }

    @Test
    fun `multiplication rejects string and string`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Multiplication,
            Type.StringType,
            Type.StringType
        )

        assertNull(result)
    }

    @Test
    fun `multiplication rejects string and number`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Multiplication,
            Type.StringType,
            Type.NumberType
        )

        assertNull(result)
    }

    @Test
    fun `multiplication rejects number and string`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Multiplication,
            Type.NumberType,
            Type.StringType
        )

        assertNull(result)
    }

    @Test
    fun `division accepts number and number`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Division,
            Type.NumberType,
            Type.NumberType
        )

        assertEquals(Type.NumberType, result)
    }

    @Test
    fun `division rejects string and string`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Division,
            Type.StringType,
            Type.StringType
        )

        assertNull(result)
    }

    @Test
    fun `division rejects string and number`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Division,
            Type.StringType,
            Type.NumberType
        )

        assertNull(result)
    }

    @Test
    fun `division rejects number and string`() {
        val result = OperatorRules.resultType(
            BinaryOperator.Division,
            Type.NumberType,
            Type.StringType
        )

        assertNull(result)
    }
}
