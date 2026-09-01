package com.printscript.ast

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TypeTest {
    @Test
    fun `number type is a Type`() {
        assertEquals(Type.NumberType, Type.NumberType)
    }

    @Test
    fun `string type is a Type`() {
        assertEquals(Type.StringType, Type.StringType)
    }

    @Test
    fun `all types are valid Types`() {
        val types: List<Type> =
            listOf(
                Type.NumberType,
                Type.StringType,
            )

        assertEquals(2, types.size)
    }
}
