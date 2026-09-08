package com.printscript.interpreter.executor

object StatementExecutors {
    val DEFAULT: List<StatementExecutor> =
        listOf(
            DeclarationExecutor,
            AssignmentExecutor,
            CallExecutor(Builtins.DEFAULT),
        )
}
