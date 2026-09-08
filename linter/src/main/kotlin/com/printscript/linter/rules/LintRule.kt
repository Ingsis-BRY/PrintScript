package com.printscript.linter.rules

import com.printscript.linter.report.LintFinding
import com.printscript.linter.rules.LintNode

interface LintRule {
    fun check(node: LintNode): List<LintFinding>
}
