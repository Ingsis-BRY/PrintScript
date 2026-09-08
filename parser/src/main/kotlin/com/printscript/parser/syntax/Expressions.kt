package com.printscript.parser.syntax

import com.printscript.ast.Expression
import com.printscript.report.Result

fun interface Expressions {
    fun parse(): Result<Expression>
}
