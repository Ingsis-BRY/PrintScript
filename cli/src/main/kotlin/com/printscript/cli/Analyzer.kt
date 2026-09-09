package com.printscript.cli

import com.printscript.ast.Statement

fun interface Analyzer {
    fun findings(statement: Statement): List<String>
}
