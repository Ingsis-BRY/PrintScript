package com.printscript.interpreter.executor

import com.printscript.interpreter.Value
import com.printscript.report.Result

fun interface Builtin {
    fun call(
        argument: Value,
        context: ExecutionContext,
    ): Result<Unit>
}
