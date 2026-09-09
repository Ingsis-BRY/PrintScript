package com.printscript.linter.report

import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.linter.config.identifier.IdentifierStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class FindingRendererTest {
    private val renderer = FindingRenderer()

    private val span = Span(Position(2, 9), Position(2, 14))

    @Test
    fun `a finding carries both ends of its span, like an error does`() {
        val rendered = renderer.render(LintFinding.InvalidPrintlnArgument(span))

        assertEquals(
            "(2:9)-(2:14) Invalid println argument: expected a variable or a literal.",
            rendered,
        )
    }

    @Test
    fun `renders an identifier that does not match camel case`() {
        assertEquals(
            "(2:9)-(2:14) Invalid identifier 'bad_name': expected camel case.",
            renderer.render(
                LintFinding.InvalidIdentifier("bad_name", IdentifierStyle.CAMEL_CASE, span),
            ),
        )
    }

    @Test
    fun `renders an identifier that does not match snake case`() {
        assertEquals(
            "(2:9)-(2:14) Invalid identifier 'badName': expected snake case.",
            renderer.render(
                LintFinding.InvalidIdentifier("badName", IdentifierStyle.SNAKE_CASE, span),
            ),
        )
    }

    @Test
    fun `renders an invalid readInput argument`() {
        assertEquals(
            "(2:9)-(2:14) Invalid readInput argument: expected a variable or a literal.",
            renderer.render(LintFinding.InvalidReadInputArgument(span)),
        )
    }
}
