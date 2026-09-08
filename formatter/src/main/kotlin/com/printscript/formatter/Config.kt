package com.printscript.formatter

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class ConfigError(
    message: String,
) : IllegalArgumentException(message)

data class Config(
    val spaceBeforeColon: Boolean,
    val spaceAfterColon: Boolean,
    val spaceAroundAssignment: Boolean,
    val noSpaceAroundAssignment: Boolean,
    val singleSpaceSeparation: Boolean,
    val spaceAroundOperators: Boolean,
    val lineBreakAfterStatement: Boolean,
    val blankLinesAfterPrintln: Int?,
) {
    companion object {
        const val SPACE_BEFORE_COLON = "enforce-spacing-before-colon-in-declaration"
        const val SPACE_AFTER_COLON = "enforce-spacing-after-colon-in-declaration"
        const val SPACE_AROUND_ASSIGNMENT = "enforce-spacing-around-equals"
        const val NO_SPACE_AROUND_ASSIGNMENT = "enforce-no-spacing-around-equals"
        const val SINGLE_SPACE_SEPARATION = "mandatory-single-space-separation"
        const val SPACE_AROUND_OPERATORS = "mandatory-space-surrounding-operations"
        const val LINE_BREAK_AFTER_STATEMENT = "mandatory-line-break-after-statement"
        const val BLANK_LINES_AFTER_PRINTLN = "line-breaks-after-println"

        val PRESERVING =
            Config(
                spaceBeforeColon = false,
                spaceAfterColon = false,
                spaceAroundAssignment = false,
                noSpaceAroundAssignment = false,
                singleSpaceSeparation = false,
                spaceAroundOperators = false,
                lineBreakAfterStatement = false,
                blankLinesAfterPrintln = null,
            )

        private val BLANK_LINES_ALLOWED = 0..2

        fun read(stream: InputStream): Config = of(parse(stream.bufferedReader().readText()))

        fun read(path: Path): Config =
            try {
                Files.newInputStream(path).use { read(it) }
            } catch (error: IOException) {
                throw ConfigError(
                    "the configuration file at $path could not be read: ${error.message}",
                )
            }

        private fun parse(text: String): JsonObject {
            val element =
                try {
                    Json.parseToJsonElement(text.ifBlank { "{}" })
                } catch (error: SerializationException) {
                    throw ConfigError("the configuration file is not valid JSON: ${error.message}")
                }

            return element as? JsonObject
                ?: throw ConfigError("the configuration file must hold a set of settings.")
        }

        private fun of(settings: JsonObject): Config =
            Config(
                spaceBeforeColon = flag(settings, SPACE_BEFORE_COLON),
                spaceAfterColon = flag(settings, SPACE_AFTER_COLON),
                spaceAroundAssignment = flag(settings, SPACE_AROUND_ASSIGNMENT),
                noSpaceAroundAssignment = flag(settings, NO_SPACE_AROUND_ASSIGNMENT),
                singleSpaceSeparation = flag(settings, SINGLE_SPACE_SEPARATION),
                spaceAroundOperators = flag(settings, SPACE_AROUND_OPERATORS),
                lineBreakAfterStatement = flag(settings, LINE_BREAK_AFTER_STATEMENT),
                blankLinesAfterPrintln = count(settings, BLANK_LINES_AFTER_PRINTLN),
            )

        private fun flag(
            settings: JsonObject,
            key: String,
        ): Boolean {
            val value = settings[key] ?: return false

            return (value as? JsonPrimitive)?.booleanOrNull
                ?: throw ConfigError("'$key' expects true or false, but the file says '$value'.")
        }

        private fun count(
            settings: JsonObject,
            key: String,
        ): Int? {
            val value = settings[key] ?: return null
            val number =
                (value as? JsonPrimitive)?.intOrNull
                    ?: throw ConfigError(
                        "'$key' expects a whole number, but the file says '$value'.",
                    )

            if (number !in BLANK_LINES_ALLOWED) {
                throw ConfigError(
                    "'$key' expects a number between ${BLANK_LINES_ALLOWED.first} " +
                        "and ${BLANK_LINES_ALLOWED.last}, but the file says $number.",
                )
            }

            return number
        }
    }
}
