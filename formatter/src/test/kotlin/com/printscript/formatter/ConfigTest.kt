package com.printscript.formatter

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigTest {
    private fun configOf(settings: String): Config = Config.read(settings.byteInputStream())

    private fun fileOf(settings: String): Path {
        val file = Files.createTempFile("printscript", ".json")
        file.toFile().deleteOnExit()
        Files.writeString(file, settings)
        return file
    }

    @Test
    fun `reads a flag that is turned on`() {
        val config = configOf("""{ "mandatory-single-space-separation": true }""")

        assertTrue(config.singleSpaceSeparation)
    }

    @Test
    fun `reads a flag that is turned off`() {
        val config = configOf("""{ "mandatory-single-space-separation": false }""")

        assertFalse(config.singleSpaceSeparation)
    }

    @Test
    fun `reads every spacing flag by its own key`() {
        assertTrue(
            configOf(
                """{ "enforce-spacing-before-colon-in-declaration": true }""",
            ).spaceBeforeColon,
        )
        assertTrue(
            configOf("""{ "enforce-spacing-after-colon-in-declaration": true }""").spaceAfterColon,
        )
        assertTrue(configOf("""{ "enforce-spacing-around-equals": true }""").spaceAroundAssignment)
        assertTrue(
            configOf("""{ "enforce-no-spacing-around-equals": true }""").noSpaceAroundAssignment,
        )
        assertTrue(
            configOf("""{ "mandatory-space-surrounding-operations": true }""").spaceAroundOperators,
        )
        assertTrue(
            configOf(
                """{ "mandatory-line-break-after-statement": true }""",
            ).lineBreakAfterStatement,
        )
    }

    @Test
    fun `reads the number of blank lines after println`() {
        assertEquals(2, configOf("""{ "line-breaks-after-println": 2 }""").blankLinesAfterPrintln)
    }

    @Test
    fun `zero blank lines is a value and not an absence`() {
        assertEquals(0, configOf("""{ "line-breaks-after-println": 0 }""").blankLinesAfterPrintln)
    }

    @Test
    fun `a setting the file does not mention stays off`() {
        val config = configOf("""{ "enforce-spacing-around-equals": true }""")

        assertFalse(config.singleSpaceSeparation)
        assertNull(config.blankLinesAfterPrintln)
    }

    @Test
    fun `an empty object turns nothing on`() {
        assertEquals(Config.PRESERVING, configOf("{}"))
    }

    @Test
    fun `an empty file turns nothing on`() {
        assertEquals(Config.PRESERVING, configOf(""))
    }

    @Test
    fun `every rule is off in the preserving config`() {
        assertEquals(Config.PRESERVING, configOf("""{ "unknown-setting": true }"""))
    }

    @Test
    fun `a number outside the range is rejected instead of clamped`() {
        val error =
            assertFailsWith<ConfigError> { configOf("""{ "line-breaks-after-println": 7 }""") }

        assertContains(error.message.orEmpty(), "between 0 and 2")
        assertContains(error.message.orEmpty(), "line-breaks-after-println")
    }

    @Test
    fun `a negative number of blank lines is rejected`() {
        assertFailsWith<ConfigError> { configOf("""{ "line-breaks-after-println": -1 }""") }
    }

    @Test
    fun `a flag that holds text is rejected`() {
        val error =
            assertFailsWith<ConfigError> {
                configOf("""{ "mandatory-single-space-separation": "yes" }""")
            }

        assertContains(error.message.orEmpty(), "true or false")
    }

    @Test
    fun `a count that holds text is rejected`() {
        val error =
            assertFailsWith<ConfigError> { configOf("""{ "line-breaks-after-println": "two" }""") }

        assertContains(error.message.orEmpty(), "whole number")
    }

    @Test
    fun `content that is not a set of settings is rejected`() {
        val error = assertFailsWith<ConfigError> { configOf("[1, 2, 3]") }

        assertContains(error.message.orEmpty(), "set of settings")
    }

    @Test
    fun `malformed json is rejected`() {
        val error = assertFailsWith<ConfigError> { configOf("{ unbalanced: ") }

        assertContains(error.message.orEmpty(), "not valid JSON")
    }

    @Test
    fun `reads the settings from a file on disk`() {
        val file = fileOf("""{ "line-breaks-after-println": 1 }""")

        assertEquals(1, Config.read(file).blankLinesAfterPrintln)
    }

    @Test
    fun `a file that does not exist is rejected`() {
        val error = assertFailsWith<ConfigError> { Config.read(Path.of("no-such-config.json")) }

        assertContains(error.message.orEmpty(), "could not be read")
    }

    @Test
    fun `a config error is a misuse of the tool and not a program failure`() {
        assertFailsWith<IllegalArgumentException> {
            configOf(
                """{ "line-breaks-after-println": 7 }""",
            )
        }
    }
}
