package com.printscript.linter.config

import com.printscript.linter.config.builtin.BuiltinConfig
import com.printscript.linter.config.identifier.IdentifierNamingConfig

/**
 * Each rule can be enabled or disabled independently.
 */
data class LintConfig(
    val identifierNaming: IdentifierNamingConfig = IdentifierNamingConfig(),
    val println: BuiltinConfig = BuiltinConfig(),
    val readInput: BuiltinConfig = BuiltinConfig(),
)
