package com.printscript.interpreter.executor

import com.printscript.ast.Statement
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result

internal inline fun <reified T : Statement> Statement.narrow(
    execute: (T) -> Result<Unit>,
): Result<Unit> =
    if (this is T) {
        execute(this)
    } else {
        Failure(Diagnostic.UnsupportedStatement(span))
    }
