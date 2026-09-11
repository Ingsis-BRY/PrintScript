package com.printscript.checker

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.language.Environment
import com.printscript.language.OperatorRules
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap
import com.printscript.report.map

class Checker(
    private val globalScope: Environment<Unit>,
    private val signatures: Map<String, Signature>,
    private val builtins: Set<String>,
) {
    fun check(statement: Statement): Result<Unit> = check(statement, globalScope)

    private fun check(
        statement: Statement,
        scope: Environment<Unit>,
    ): Result<Unit> =
        when (statement) {
            is Statement.VariableDeclaration -> checkDeclaration(statement, scope)
            is Statement.Assignment -> checkAssignment(statement, scope)
            is Statement.CallStatement -> checkCall(statement, scope)
            is Statement.IfStatement -> checkIf(statement, scope)
        }

    private fun checkDeclaration(
        declaration: Statement.VariableDeclaration,
        scope: Environment<Unit>,
    ): Result<Unit> =
        scope
            .declare(
                name = declaration.name,
                type = declaration.declaredType,
                mutable = declaration.mutable,
                span = declaration.span,
            ).flatMap {
                val initializer =
                    declaration.initializer
                        ?: return@flatMap Success(Unit)

                typeOf(initializer, declaration.declaredType, scope).flatMap { actual ->
                    scope.initialize(declaration.name, Unit, actual, declaration.span)
                }
            }

    private fun checkAssignment(
        assignment: Statement.Assignment,
        scope: Environment<Unit>,
    ): Result<Unit> {
        val expected = scope.declaredTypeOf(assignment.name)

        return typeOf(assignment.value, expected, scope).flatMap { actual ->
            scope.assign(assignment.name, Unit, actual, assignment.span)
        }
    }

    private fun checkCall(
        call: Statement.CallStatement,
        scope: Environment<Unit>,
    ): Result<Unit> {
        if (call.callee !in builtins) {
            return Failure(Diagnostic.UnknownFunction(call.callee, call.span))
        }

        return typeOf(call.argument, Type.StringType, scope).map { }
    }

    private fun checkIf(
        conditional: Statement.IfStatement,
        scope: Environment<Unit>,
    ): Result<Unit> =
        typeOf(conditional.condition, Type.BooleanType, scope).flatMap { condition ->
            if (condition != Type.BooleanType) {
                return@flatMap Failure(
                    Diagnostic.NonBooleanCondition(condition, conditional.condition.span),
                )
            }

            checkBlock(conditional.consequence, scope).flatMap {
                checkBlock(conditional.alternative.orEmpty(), scope)
            }
        }

    private fun checkBlock(
        statements: List<Statement>,
        scope: Environment<Unit>,
    ): Result<Unit> {
        val block = scope.child()

        for (statement in statements) {
            val checked = check(statement, block)

            if (checked is Failure) {
                return checked
            }
        }

        return Success(Unit)
    }

    private fun typeOf(
        expression: Expression,
        expected: Type?,
        scope: Environment<Unit>,
    ): Result<Type> =
        when (expression) {
            is Expression.NumberLiteral -> Success(Type.NumberType)
            is Expression.StringLiteral -> Success(Type.StringType)
            is Expression.BooleanLiteral -> Success(Type.BooleanType)
            is Expression.VariableReference -> scope.typeOf(expression.name, expression.span)
            is Expression.BinaryExpression -> typeOfBinary(expression, scope)
            is Expression.FunctionCall -> typeOfCall(expression, expected, scope)
        }

    private fun typeOfBinary(
        expression: Expression.BinaryExpression,
        scope: Environment<Unit>,
    ): Result<Type> =
        typeOf(expression.left, null, scope).flatMap { left ->
            typeOf(expression.right, null, scope).flatMap { right ->
                OperatorRules
                    .resultType(expression.operator, left, right)
                    ?.let { Success(it) }
                    ?: Failure(
                        Diagnostic.IncompatibleOperands(
                            operator = expression.operator,
                            left = left,
                            right = right,
                            span = expression.span,
                        ),
                    )
            }
        }

    private fun typeOfCall(
        expression: Expression.FunctionCall,
        expected: Type?,
        scope: Environment<Unit>,
    ): Result<Type> {
        val signature =
            signatures[expression.callee]
                ?: return Failure(
                    Diagnostic.UnknownFunction(expression.callee, expression.span),
                )

        return typeOf(expression.argument, signature.argument, scope).flatMap { argument ->
            if (argument != signature.argument) {
                Failure(
                    Diagnostic.IncompatibleArgument(
                        name = expression.callee,
                        expected = signature.argument,
                        actual = argument,
                        span = expression.argument.span,
                    ),
                )
            } else {
                Success(expected ?: Type.StringType)
            }
        }
    }
}
