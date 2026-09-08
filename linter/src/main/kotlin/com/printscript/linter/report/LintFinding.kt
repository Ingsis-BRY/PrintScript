package com.printscript.linter.report

import com.printscript.common.Span
import com.printscript.linter.config.identifier.IdentifierStyle

sealed interface LintFinding {
    val span: Span

    data class InvalidIdentifier(
        val name: String,
        val expectedStyle: IdentifierStyle,
        override val span: Span,
    ) : LintFinding

    data class InvalidPrintlnArgument(
        override val span: Span,
    ) : LintFinding

    data class InvalidReadInputArgument(
        override val span: Span,
    ) : LintFinding
}
