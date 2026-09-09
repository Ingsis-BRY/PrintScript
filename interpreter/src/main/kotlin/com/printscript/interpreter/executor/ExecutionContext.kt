package com.printscript.interpreter.executor

import com.printscript.ast.Expression
import com.printscript.interpreter.Environment
import com.printscript.interpreter.Value
import com.printscript.report.Result

interface ExecutionContext {
    val environment: Environment

    fun evaluate(expression: Expression): Result<Value>

    fun emit(line: String)
}
