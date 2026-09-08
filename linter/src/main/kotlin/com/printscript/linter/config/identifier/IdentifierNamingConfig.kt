package com.printscript.linter.config.identifier

data class IdentifierNamingConfig(
    val enabled: Boolean = true,
    val style: IdentifierStyle = IdentifierStyle.CAMEL_CASE,
)
