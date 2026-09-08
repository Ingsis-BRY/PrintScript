package com.printscript.linter.config.builtin

/**
 * Config for arguments validation of PrintScript built-in operations
 * such as println and readInput.
 */
data class BuiltinConfig(
    val enabled: Boolean = true,
)
