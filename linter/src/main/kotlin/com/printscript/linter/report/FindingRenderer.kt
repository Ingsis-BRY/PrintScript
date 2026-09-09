package com.printscript.linter.report

import com.printscript.common.Span
import com.printscript.linter.config.identifier.IdentifierStyle

class FindingRenderer {
    fun render(finding: LintFinding): String = "${renderSpan(finding.span)} ${describe(finding)}"

    private fun renderSpan(span: Span): String =
        "(${span.start.line}:${span.start.column})-(${span.end.line}:${span.end.column})"

    private fun describe(finding: LintFinding): String =
        when (finding) {
            is LintFinding.InvalidIdentifier ->
                "Invalid identifier '${finding.name}': expected ${describe(finding.expectedStyle)}."

            is LintFinding.InvalidPrintlnArgument ->
                "Invalid println argument: expected a variable or a literal."

            is LintFinding.InvalidReadInputArgument ->
                "Invalid readInput argument: expected a variable or a literal."
        }

    private fun describe(style: IdentifierStyle): String =
        when (style) {
            IdentifierStyle.CAMEL_CASE -> "camel case"
            IdentifierStyle.SNAKE_CASE -> "snake case"
        }
}
