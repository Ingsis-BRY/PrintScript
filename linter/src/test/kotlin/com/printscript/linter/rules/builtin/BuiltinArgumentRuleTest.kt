package com.printscript.linter.rules.builtin

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuiltinArgumentRuleTest {
    @Test
    fun `accepts variable reference in println`() {
        val span = Span(Position(2, 4), Position(2, 10))
        val rule = printlnRule()

        val node =
            callNode(
                callee = "println",
                argument =
                    Expression.VariableReference(
                        name = "message",
                        start = span.start,
                        end = span.end,
                    ),
                span = span,
            )

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `accepts number literal in readInput`() {
        val span = Span(Position(4, 12), Position(4, 15))
        val rule = readInputRule()

        val node =
            callNode(
                callee = "readInput",
                argument =
                    Expression.NumberLiteral(
                        value = 42.0,
                        start = span.start,
                        end = span.end,
                    ),
                span = span,
            )

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `accepts string literal in println`() {
        val span = Span(Position(7, 8), Position(7, 17))
        val rule = printlnRule()

        val node =
            callNode(
                callee = "println",
                argument =
                    Expression.StringLiteral(
                        value = "hello",
                        start = span.start,
                        end = span.end,
                    ),
                span = span,
            )

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `reports binary expression in println`() {
        val span = Span(Position(10, 9), Position(10, 14))
        val rule = printlnRule()

        val node =
            callNode(
                callee = "println",
                argument = binaryExpression(span),
                span = span,
            )

        assertEquals(
            listOf(
                LintFinding.InvalidPrintlnArgument(span),
            ),
            rule.check(node),
        )
    }

    @Test
    fun `reports binary expression in readInput`() {
        val span = Span(Position(13, 5), Position(13, 11))
        val rule = readInputRule()

        val node =
            callNode(
                callee = "readInput",
                argument = binaryExpression(span),
                span = span,
            )

        assertEquals(
            listOf(
                LintFinding.InvalidReadInputArgument(span),
            ),
            rule.check(node),
        )
    }

    @Test
    fun `ignores calls to another builtin`() {
        val span = Span(Position(16, 3), Position(16, 9))
        val rule = printlnRule()

        val node =
            callNode(
                callee = "readInput",
                argument = binaryExpression(span),
                span = span,
            )

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `does nothing when rule is disabled`() {
        val span = Span(Position(19, 7), Position(19, 13))
        val rule =
            BuiltinArgumentRule(
                builtinName = "readInput",
                enabled = false,
                invalidFinding = LintFinding::InvalidReadInputArgument,
            )

        val node =
            callNode(
                callee = "readInput",
                argument = binaryExpression(span),
                span = span,
            )

        assertTrue(rule.check(node).isEmpty())
    }

    private fun printlnRule() =
        BuiltinArgumentRule(
            builtinName = "println",
            enabled = true,
            invalidFinding = LintFinding::InvalidPrintlnArgument,
        )

    private fun readInputRule() =
        BuiltinArgumentRule(
            builtinName = "readInput",
            enabled = true,
            invalidFinding = LintFinding::InvalidReadInputArgument,
        )

    private fun callNode(
        callee: String,
        argument: Expression,
        span: Span,
    ): LintNode.Statement =
        LintNode.Statement(
            Statement.CallStatement(
                callee = callee,
                argument = argument,
                start = span.start,
                end = span.end,
            ),
        )

    private fun binaryExpression(span: Span): Expression.BinaryExpression =
        Expression.BinaryExpression(
            left =
                Expression.NumberLiteral(
                    value = 10.0,
                    start = span.start,
                    end = span.end,
                ),
            operator = BinaryOperator.Addition,
            right =
                Expression.NumberLiteral(
                    value = 20.0,
                    start = span.start,
                    end = span.end,
                ),
            start = span.start,
            end = span.end,
        )
}
