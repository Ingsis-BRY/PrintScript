package com.printscript.interpreter.executor

import com.printscript.common.Span
import com.printscript.interpreter.Value
import com.printscript.report.Result

fun interface Builtin {
    fun call(
        argument: Value,
        span: Span,
        context: ExecutionContext,
    ): Result<Unit>
}
