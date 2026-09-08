package com.printscript.linter.rules

sealed interface LintNode {
    data class Statement(
        val value: com.printscript.ast.Statement,
    ) : LintNode

    data class Expression(
        val value: com.printscript.ast.Expression,
    ) : LintNode
}
