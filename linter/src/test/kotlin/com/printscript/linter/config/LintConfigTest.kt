package com.printscript.linter.config

import com.printscript.linter.config.identifier.IdentifierStyle
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LintConfigTest {
    private fun read(settings: String): LintConfig = LintConfig.read(settings.byteInputStream())

    @Test
    fun `naming the style turns the rule on`() {
        val config = read("""{ "identifier_format": "snake case" }""")

        assertTrue(config.identifierFormat.enabled)
        assertEquals(IdentifierStyle.SNAKE_CASE, config.identifierFormat.style)
    }

    @Test
    fun `camel case written as a style is read the same way`() {
        val config = read("""{ "identifier_format": "camel case" }""")

        assertTrue(config.identifierFormat.enabled)
        assertEquals(IdentifierStyle.CAMEL_CASE, config.identifierFormat.style)
    }

    @Test
    fun `the separator between the two words does not change the style`() {
        val spellings = listOf("snake case", "snake_case", "snakeCase", "SNAKE_CASE")

        for (spelling in spellings) {
            assertEquals(
                IdentifierStyle.SNAKE_CASE,
                read("""{ "identifier_format": "$spelling" }""").identifierFormat.style,
                "'$spelling' should name snake case",
            )
        }
    }

    @Test
    fun `the object form still works and its switch is honoured`() {
        val config = read("""{ "identifier_format": { "enabled": true, "style": "camelCase" } }""")

        assertTrue(config.identifierFormat.enabled)
        assertEquals(IdentifierStyle.CAMEL_CASE, config.identifierFormat.style)
    }

    @Test
    fun `the object form can name a style and still be off`() {
        val config =
            read("""{ "identifier_format": { "enabled": false, "style": "snake_case" } }""")

        assertFalse(config.identifierFormat.enabled)
    }

    @Test
    fun `the object form defaults to camel case`() {
        val config = read("""{ "identifier_format": { "enabled": true } }""")

        assertEquals(IdentifierStyle.CAMEL_CASE, config.identifierFormat.style)
    }

    @Test
    fun `an empty configuration turns every rule off`() {
        val config = read("{}")

        assertFalse(config.identifierFormat.enabled)
        assertFalse(config.mandatoryVariableOrLiteralInPrintln)
        assertFalse(config.mandatoryVariableOrLiteralInReadInput)
    }

    @Test
    fun `a style the linter does not have is refused`() {
        val error =
            assertFailsWith<ConfigError> {
                read("""{ "identifier_format": "kebab case" }""")
            }

        assertContains(error.message.orEmpty(), "kebab case")
    }

    @Test
    fun `a style that is neither a name nor a set of settings is refused`() {
        assertFailsWith<ConfigError> {
            read("""{ "identifier_format": ["camel case"] }""")
        }
    }

    @Test
    fun `a switch that is not true or false is refused`() {
        assertFailsWith<ConfigError> {
            read("""{ "mandatory-variable-or-literal-in-println": "yes" }""")
        }
    }

    @Test
    fun `a file that is not JSON is refused`() {
        assertFailsWith<ConfigError> { read("not json at all") }
    }

    @Test
    fun `an empty file reads as an empty configuration`() {
        assertFalse(read("").identifierFormat.enabled)
    }

    @Test
    fun `the println rule is read`() {
        assertTrue(
            read("""{ "mandatory-variable-or-literal-in-println": true }""")
                .mandatoryVariableOrLiteralInPrintln,
        )
    }

    @Test
    fun `the readInput rule is read`() {
        assertTrue(
            read("""{ "mandatory-variable-or-literal-in-readInput": true }""")
                .mandatoryVariableOrLiteralInReadInput,
        )
    }
}
