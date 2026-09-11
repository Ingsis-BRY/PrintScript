package com.printscript.interpreter.executor

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.interpreter.Value
import com.printscript.language.Environment
import com.printscript.report.Result

interface ExecutionContext {
    val environment: Environment<Value>

    fun evaluate(
        expression: Expression,
        expected: Type?,
    ): Result<Value>

    fun executeBlock(statements: List<Statement>): Result<Unit>

    fun emit(line: String)
}
