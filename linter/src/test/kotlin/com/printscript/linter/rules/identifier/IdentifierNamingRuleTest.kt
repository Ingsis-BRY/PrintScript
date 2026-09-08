package com.printscript.linter.rules.identifier

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.linter.config.identifier.IdentifierNamingConfig
import com.printscript.linter.config.identifier.IdentifierStyle
import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdentifierNamingRuleTest {
    @Test
    fun `accepts valid camel case variable declaration`() {
        val span = Span(Position(2, 4), Position(2, 14))
        val rule = camelCaseRule()

        val node =
            statementNode(
                VariableDeclaration(
                    name = "myVariable",
                    span = span,
                ),
            )

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `reports invalid camel case variable declaration`() {
        val span = Span(Position(5, 1), Position(5, 14))
        val rule = camelCaseRule()

        val node =
            statementNode(
                VariableDeclaration(
                    name = "my_variable",
                    span = span,
                ),
            )

        assertEquals(
            listOf(
                LintFinding.InvalidIdentifier(
                    name = "my_variable",
                    expectedStyle = IdentifierStyle.CAMEL_CASE,
                    span = span,
                ),
            ),
            rule.check(node),
        )
    }

    @Test
    fun `accepts valid snake case assignment`() {
        val span = Span(Position(8, 3), Position(8, 15))
        val rule = snakeCaseRule()

        val node =
            statementNode(
                Assignment(
                    name = "user_name",
                    span = span,
                ),
            )

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `reports invalid snake case assignment`() {
        val span = Span(Position(11, 5), Position(11, 17))
        val rule = snakeCaseRule()

        val node =
            statementNode(
                Assignment(
                    name = "userName",
                    span = span,
                ),
            )

        assertEquals(
            listOf(
                LintFinding.InvalidIdentifier(
                    name = "userName",
                    expectedStyle = IdentifierStyle.SNAKE_CASE,
                    span = span,
                ),
            ),
            rule.check(node),
        )
    }

    @Test
    fun `reports invalid variable reference`() {
        val span = Span(Position(14, 10), Position(14, 19))
        val rule = camelCaseRule()

        val node =
            LintNode.Expression(
                Expression.VariableReference(
                    name = "user_name",
                    start = span.start,
                    end = span.end,
                ),
            )

        assertEquals(
            listOf(
                LintFinding.InvalidIdentifier(
                    name = "user_name",
                    expectedStyle = IdentifierStyle.CAMEL_CASE,
                    span = span,
                ),
            ),
            rule.check(node),
        )
    }

    @Test
    fun `ignores call statements`() {
        val span = Span(Position(17, 1), Position(17, 16))
        val rule = camelCaseRule()

        val node =
            statementNode(
                Statement.CallStatement(
                    callee = "println",
                    argument =
                        Expression.StringLiteral(
                            value = "hello",
                            start = span.start,
                            end = span.end,
                        ),
                    start = span.start,
                    end = span.end,
                ),
            )

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `ignores number literals`() {
        val span = Span(Position(20, 6), Position(20, 9))
        val rule = camelCaseRule()

        val node =
            LintNode.Expression(
                Expression.NumberLiteral(
                    value = 42.0,
                    start = span.start,
                    end = span.end,
                ),
            )

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `does nothing when rule is disabled`() {
        val span = Span(Position(23, 2), Position(23, 14))
        val rule =
            IdentifierNamingRule(
                IdentifierNamingConfig(
                    enabled = false,
                    style = IdentifierStyle.CAMEL_CASE,
                ),
            )

        val node =
            statementNode(
                VariableDeclaration(
                    name = "invalid_name",
                    span = span,
                ),
            )

        assertTrue(rule.check(node).isEmpty())
    }

    private fun camelCaseRule() =
        IdentifierNamingRule(
            IdentifierNamingConfig(
                style = IdentifierStyle.CAMEL_CASE,
            ),
        )

    private fun snakeCaseRule() =
        IdentifierNamingRule(
            IdentifierNamingConfig(
                style = IdentifierStyle.SNAKE_CASE,
            ),
        )

    private fun statementNode(statement: Statement): LintNode.Statement =
        LintNode.Statement(statement)

    private fun VariableDeclaration(
        name: String,
        span: Span,
    ): Statement.VariableDeclaration =
        Statement.VariableDeclaration(
            name = name,
            declaredType = Type.NumberType,
            initializer = null,
            start = span.start,
            end = span.end,
        )

    private fun Assignment(
        name: String,
        span: Span,
    ): Statement.Assignment =
        Statement.Assignment(
            name = name,
            value =
                Expression.NumberLiteral(
                    value = 1.0,
                    start = span.start,
                    end = span.end,
                ),
            start = span.start,
            end = span.end,
        )
}
