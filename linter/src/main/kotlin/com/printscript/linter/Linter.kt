package com.printscript.linter

import com.printscript.ast.Statement
import com.printscript.linter.config.LintConfig
import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintReport
import com.printscript.linter.rules.LintRule
import com.printscript.linter.rules.builtin.BuiltinArgumentRule
import com.printscript.linter.rules.identifier.IdentifierFormatingRule

/**
 * Facade for the linting process.
 *
 * Coordinates the configured lint rules and delegates AST traversal to [LintWalker],
 * keeping rule composition and traversal details separate from the public linting API.
 */
class Linter(
    private val config: LintConfig,
) {
    private val rules: List<LintRule> =
        listOf(
            IdentifierFormatingRule(config.identifierFormat),
            BuiltinArgumentRule(
                builtinName = "println",
                enabled = config.mandatoryVariableOrLiteralInPrintln,
                invalidFinding = LintFinding::InvalidPrintlnArgument,
            ),
            BuiltinArgumentRule(
                builtinName = "readInput",
                enabled = config.mandatoryVariableOrLiteralInReadInput,
                invalidFinding = LintFinding::InvalidReadInputArgument,
            ),
        )

    private val walker = LintWalker(rules)

    fun lint(statements: List<Statement>): LintReport = walker.walk(statements)
}
