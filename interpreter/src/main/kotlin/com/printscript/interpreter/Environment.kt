package com.printscript.interpreter

import com.printscript.ast.Type
import com.printscript.common.Span
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success

/**
* [Declared] holds the type but no value (`let x: number;`),
* [Bound] holds the value. absent from the map means not declared.
*/
sealed interface Slot {
    data class Declared(
        val type: Type,
    ) : Slot

    data class Bound(
        val value: Value,
    ) : Slot
}

/**
* the interpreter's variable store, the only mutable component, on purpose.
* every operation returns a [Result], failing with the [Diagnostic] case
* that names what went wrong over the [Span] the caller blames.
*/
class Environment {
    private val slots: MutableMap<String, Slot> = mutableMapOf()

    /**
     * declares a name with a type but no value, fails if it already exists
     */
    fun declare(
        name: String,
        type: Type,
        span: Span,
    ): Result<Unit> {
        if (slots.containsKey(name)) {
            return Failure(Diagnostic.VariableAlreadyDeclared(name, span))
        }
        slots[name] = Slot.Declared(type)
        return Success(Unit)
    }

    /**
     * binds the first value to a declared name (`let x: number = 5;`)
     */
    fun initialize(
        name: String,
        value: Value,
        span: Span,
    ): Result<Unit> = bind(name, value, span)

    /**
     * reassigns an already-declared variable (`x = 5;`)
     */
    fun assign(
        name: String,
        value: Value,
        span: Span,
    ): Result<Unit> = bind(name, value, span)

    /**
     * reads a variable's value
     */
    fun lookup(
        name: String,
        span: Span,
    ): Result<Value> =
        when (val slot = slots[name]) {
            null -> Failure(Diagnostic.VariableNotDeclared(name, span))
            is Slot.Declared -> Failure(Diagnostic.VariableWithoutValue(name, span))
            is Slot.Bound -> Success(slot.value)
        }

    private fun bind(
        name: String,
        value: Value,
        span: Span,
    ): Result<Unit> {
        val declaredType =
            when (val slot = slots[name]) {
                null -> return Failure(Diagnostic.VariableNotDeclared(name, span))
                is Slot.Declared -> slot.type
                is Slot.Bound -> slot.value.type
            }
        if (value.type != declaredType) {
            return Failure(
                Diagnostic.IncompatibleAssignment(
                    name = name,
                    declared = declaredType,
                    actual = value.type,
                    span = span,
                ),
            )
        }
        slots[name] = Slot.Bound(value)
        return Success(Unit)
    }
}
