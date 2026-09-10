package com.printscript.linter

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintReport
import com.printscript.linter.rules.LintNode
import com.printscript.linter.rules.LintRule

/**
 * Traverses the AST once and applies all configured [LintRule]s to each node,
 * avoiding a separate traversal for each rule.
 *
 * Traversal is kept separate from rule logic and uses explicit `when`
 * expressions instead of the Visitor pattern.
 */
class LintWalker(
    private val rules: List<LintRule>,
) {
    fun walk(statements: List<Statement>): LintReport =
        LintReport(
            findings = statements.flatMap(::walkStatement),
        )

    private fun walkStatement(statement: Statement): List<LintFinding> {
        val findings =
            rules.flatMap { it.check(LintNode.Statement(statement)) }

        val children =
            when (statement) {
                is Statement.VariableDeclaration ->
                    statement.initializer
                        ?.let(::walkExpression)
                        .orEmpty()

                is Statement.Assignment -> walkExpression(statement.value)

                is Statement.CallStatement -> walkExpression(statement.argument)

                is Statement.IfStatement ->
                    walkExpression(statement.condition) +
                        statement.consequence.flatMap(::walkStatement) +
                        statement.alternative.orEmpty().flatMap(::walkStatement)
            }

        return findings + children
    }

    private fun walkExpression(expression: Expression): List<LintFinding> {
        val findings =
            rules.flatMap { it.check(LintNode.Expression(expression)) }

        val children =
            when (expression) {
                is Expression.BinaryExpression ->
                    walkExpression(expression.left) +
                        walkExpression(expression.right)

                is Expression.FunctionCall ->
                    walkExpression(expression.argument)

                is Expression.NumberLiteral,
                is Expression.StringLiteral,
                is Expression.BooleanLiteral,
                is Expression.VariableReference,
                ->
                    emptyList()
            }

        return findings + children
    }
}
