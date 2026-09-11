package com.printscript.interpreter.function

import com.printscript.ast.Type
import com.printscript.common.Span
import com.printscript.interpreter.Value
import com.printscript.interpreter.executor.ExecutionContext
import com.printscript.report.Result

fun interface ValueFunction {
    fun call(
        argument: Value,
        expected: Type?,
        span: Span,
        context: ExecutionContext,
    ): Result<Value>
}
