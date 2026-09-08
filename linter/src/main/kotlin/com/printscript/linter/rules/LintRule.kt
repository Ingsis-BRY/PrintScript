package com.printscript.linter.rules

import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintNode

interface LintRule {
    fun check(node: LintNode): List<LintFinding>
}
