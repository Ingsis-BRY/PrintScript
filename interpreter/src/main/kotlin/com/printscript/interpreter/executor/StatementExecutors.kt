package com.printscript.interpreter.executor

object StatementExecutors {
    val V1_0: List<StatementExecutor> =
        listOf(
            DeclarationExecutor,
            AssignmentExecutor,
            CallExecutor(Builtins.V1_0),
        )

    val V1_1: List<StatementExecutor> =
        listOf(
            DeclarationExecutor,
            AssignmentExecutor,
            IfExecutor,
            CallExecutor(Builtins.V1_1),
        )
}
