package com.printscript.linter

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.common.Position
import com.printscript.linter.config.LintConfig
import com.printscript.linter.config.identifier.IdentifierFormatConfig
import com.printscript.linter.config.identifier.IdentifierStyle
import com.printscript.linter.report.LintFinding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BlockAndCallLintingTest {
    private val at = Position(1, 1)

    private fun name(value: String) = Expression.VariableReference(value, at, at)

    private fun concat(
        left: Expression,
        right: Expression,
    ) = Expression.BinaryExpression(left, BinaryOperator.Addition, right, at, at)

    private fun declare(
        name: String,
        initializer: Expression? = null,
    ) = Statement.VariableDeclaration(
        name = name,
        declaredType = Type.StringType,
        initializer = initializer,
        mutable = true,
        start = at,
        end = at,
    )

    private fun conditional(
        consequence: List<Statement>,
        alternative: List<Statement>? = null,
    ) = Statement.IfStatement(
        condition = name("flag"),
        consequence = consequence,
        alternative = alternative,
        start = at,
        end = at,
    )

    private val camelCase =
        LintConfig(
            identifierFormat =
                IdentifierFormatConfig(enabled = true, style = IdentifierStyle.CAMEL_CASE),
        )

    private val readInputRule = LintConfig(mandatoryVariableOrLiteralInReadInput = true)

    private val printlnRule = LintConfig(mandatoryVariableOrLiteralInPrintln = true)

    @Test
    fun `a rule reaches a statement inside a block`() {
        val report = Linter(camelCase).lint(listOf(conditional(listOf(declare("bad_name")))))

        assertEquals(1, report.findings.size)
        assertIs<LintFinding.InvalidIdentifier>(report.findings.first())
    }

    @Test
    fun `a rule reaches a statement inside an else block`() {
        val report =
            Linter(camelCase).lint(
                listOf(conditional(emptyList(), alternative = listOf(declare("bad_name")))),
            )

        assertEquals(1, report.findings.size)
    }

    @Test
    fun `a rule reaches a statement two blocks deep`() {
        val report =
            Linter(camelCase).lint(
                listOf(conditional(listOf(conditional(listOf(declare("bad_name")))))),
            )

        assertEquals(1, report.findings.size)
    }

    @Test
    fun `a rule reaches the condition of an if`() {
        val report =
            Linter(camelCase).lint(
                listOf(
                    Statement.IfStatement(
                        condition = name("bad_name"),
                        consequence = emptyList(),
                        alternative = null,
                        start = at,
                        end = at,
                    ),
                ),
            )

        assertEquals(1, report.findings.size)
    }

    @Test
    fun `an if whose parts are all clean reports nothing`() {
        val report =
            Linter(camelCase).lint(listOf(conditional(listOf(declare("goodName")))))

        assertTrue(report.isClean)
    }

    @Test
    fun `readInput called with an expression is reported`() {
        val call =
            Expression.FunctionCall(
                callee = "readInput",
                argument = concat(name("a"), name("b")),
                start = at,
                end = at,
            )

        val report = Linter(readInputRule).lint(listOf(declare("input", call)))

        assertEquals(1, report.findings.size)
        assertIs<LintFinding.InvalidReadInputArgument>(report.findings.first())
    }

    @Test
    fun `readInput called with a literal is not reported`() {
        val call =
            Expression.FunctionCall(
                callee = "readInput",
                argument = Expression.StringLiteral("Name:", at, at),
                start = at,
                end = at,
            )

        assertTrue(Linter(readInputRule).lint(listOf(declare("input", call))).isClean)
    }

    @Test
    fun `the readInput rule leaves println alone and the println rule leaves readInput alone`() {
        val readInputCall =
            Expression.FunctionCall("readInput", concat(name("a"), name("b")), at, at)
        val printlnCall =
            Statement.CallStatement("println", concat(name("a"), name("b")), at, at)

        assertTrue(Linter(printlnRule).lint(listOf(declare("x", readInputCall))).isClean)
        assertTrue(Linter(readInputRule).lint(listOf(printlnCall)).isClean)
    }

    @Test
    fun `a rule that is off reports nothing`() {
        val call = Expression.FunctionCall("readInput", concat(name("a"), name("b")), at, at)

        assertTrue(Linter(LintConfig()).lint(listOf(declare("input", call))).isClean)
    }

    @Test
    fun `a rule reaches inside the argument of a call`() {
        val call =
            Expression.FunctionCall("readInput", name("bad_name"), at, at)

        val report = Linter(camelCase).lint(listOf(declare("goodName", call)))

        assertEquals(1, report.findings.size)
    }
}
