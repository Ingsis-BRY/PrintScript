package com.printscript.linter.config.identifier

data class IdentifierFormatConfig(
    val enabled: Boolean = false,
    val style: IdentifierStyle = IdentifierStyle.CAMEL_CASE,
)
