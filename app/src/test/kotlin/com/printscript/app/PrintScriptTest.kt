package com.printscript.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrintScriptTest {
    @Test
    fun `the default version is supported`() {
        assertTrue(PrintScript.supports(PrintScript.DEFAULT_VERSION))
    }

    @Test
    fun `the default version is 1 point 0`() {
        assertTrue(PrintScript.supports("1.0"))
    }

    @Test
    fun `a version with no composition is not supported`() {
        assertFalse(PrintScript.supports("9.9"))
    }

    @Test
    fun `the next language version is not supported yet`() {
        assertFalse(PrintScript.supports("1.1"))
    }
}
