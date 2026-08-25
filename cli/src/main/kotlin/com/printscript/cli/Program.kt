package com.printscript.cli

import com.printscript.ast.Statement
import com.printscript.report.Result

fun interface Program {
    fun execute(statement: Statement): Result<Unit>
}
