package com.printscript.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DialectTest {
    @Test
    fun `the default version has a dialect`() {
        assertNotNull(Dialect.of(Dialect.DEFAULT_VERSION))
    }

    @Test
    fun `the default version is 1 point 0`() {
        assertEquals("1.0", Dialect.DEFAULT_VERSION)
    }

    @Test
    fun `both language versions have a dialect`() {
        assertEquals(listOf("1.0", "1.1"), Dialect.versions())
    }

    @Test
    fun `a version the language does not have has no dialect`() {
        assertNull(Dialect.of("9.9"))
    }

    @Test
    fun `every catalog of 1 point 1 covers at least what 1 point 0 does`() {
        val older = requireNotNull(Dialect.of("1.0"))
        val newer = requireNotNull(Dialect.of("1.1"))

        assertTrue(newer.recognizers.size > older.recognizers.size)
        assertTrue(newer.syntaxes.size > older.syntaxes.size)
        assertTrue(newer.parselets.size > older.parselets.size)
        assertTrue(newer.executors.size > older.executors.size)
    }
}
