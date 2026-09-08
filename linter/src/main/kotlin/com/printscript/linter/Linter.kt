package com.printscript.linter

import com.printscript.ast.Statement
import com.printscript.linter.config.LintConfig
import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintReport
import com.printscript.linter.rules.LintRule
import com.printscript.linter.rules.builtin.BuiltinArgumentRule
import com.printscript.linter.rules.identifier.IdentifierNamingRule

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
            IdentifierNamingRule(config.identifierNaming),
            BuiltinArgumentRule(
                builtinName = "println",
                config = config.println,
                invalidFinding = LintFinding::InvalidPrintlnArgument,
            ),
            BuiltinArgumentRule(
                builtinName = "readInput",
                config = config.readInput,
                invalidFinding = LintFinding::InvalidReadInputArgument,
            ),
        )

    private val walker = LintWalker(rules)

    fun lint(statements: List<Statement>): LintReport = walker.walk(statements)
}
