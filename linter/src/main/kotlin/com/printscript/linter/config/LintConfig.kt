package com.printscript.linter.config

import com.printscript.linter.config.identifier.IdentifierFormatConfig
import com.printscript.linter.config.identifier.IdentifierStyle
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * Each rule can be enabled or disabled independently.
 */
data class LintConfig(
    val identifierFormat: IdentifierFormatConfig = IdentifierFormatConfig(),
    val mandatoryVariableOrLiteralInPrintln: Boolean = false,
    val mandatoryVariableOrLiteralInReadInput: Boolean = false,
) {
    companion object {
        private const val IDENTIFIER_FORMAT = "identifier_format"
        private const val PRINTLN = "mandatory-variable-or-literal-in-println"
        private const val READ_INPUT = "mandatory-variable-or-literal-in-readInput"

        private const val ENABLED = "enabled"
        private const val STYLE = "style"

        fun read(stream: InputStream): LintConfig = of(parse(stream.bufferedReader().readText()))

        fun read(path: Path): LintConfig =
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
                    throw ConfigError(
                        "the configuration file is not valid JSON: ${error.message}",
                    )
                }

            return element as? JsonObject
                ?: throw ConfigError(
                    "the configuration file must hold a set of settings.",
                )
        }

        private fun of(settings: JsonObject): LintConfig =
            LintConfig(
                identifierFormat = identifierFormat(settings),
                mandatoryVariableOrLiteralInPrintln = flag(settings, PRINTLN),
                mandatoryVariableOrLiteralInReadInput = flag(settings, READ_INPUT),
            )

        private fun identifierFormat(settings: JsonObject): IdentifierFormatConfig {
            val config =
                settings[IDENTIFIER_FORMAT] as? JsonObject
                    ?: return IdentifierFormatConfig(enabled = false)

            return IdentifierFormatConfig(
                enabled = flag(config, ENABLED),
                style = style(config),
            )
        }

        private fun flag(
            settings: JsonObject,
            key: String,
        ): Boolean {
            val value = settings[key] ?: return false

            return (value as? JsonPrimitive)?.booleanOrNull
                ?: throw ConfigError(
                    "'$key' expects true or false, but the file says '$value'.",
                )
        }

        private fun style(settings: JsonObject): IdentifierStyle {
            val value = settings[STYLE] ?: return IdentifierStyle.CAMEL_CASE

            val style =
                (value as? JsonPrimitive)?.contentOrNull
                    ?: throw ConfigError(
                        "'$STYLE' expects a valid identifier style.",
                    )

            return when (style.lowercase()) {
                "camelcase", "camel_case" -> IdentifierStyle.CAMEL_CASE
                "snakecase", "snake_case" -> IdentifierStyle.SNAKE_CASE
                else ->
                    throw ConfigError(
                        "'$STYLE' expects 'camelCase' or 'snake_case', but the file says '$style'.",
                    )
            }
        }
    }
}
