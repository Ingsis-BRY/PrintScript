package com.printscript.linter.report

/**
 * Collects all findings produced during a linting run instead of stopping
 * after the first finding.
 */
data class LintReport(
    val findings: List<LintFinding>,
) {
    val isClean: Boolean get() = findings.isEmpty()
}
