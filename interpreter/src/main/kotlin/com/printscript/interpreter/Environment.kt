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
    val type: Type
    val mutable: Boolean

    data class Declared(
        override val type: Type,
        override val mutable: Boolean,
    ) : Slot

    data class Bound(
        val value: Value,
        override val type: Type,
        override val mutable: Boolean,
    ) : Slot
}

/**
* the interpreter's variable store, the only mutable component, on purpose.
* every operation returns a [Result], failing with the [Diagnostic] case
* that names what went wrong over the [Span] the caller blames.
*/
class Environment private constructor(
    private val parent: Environment?,
) {
    constructor() : this(null)

    private val slots: MutableMap<String, Slot> = mutableMapOf()

    fun child(): Environment = Environment(this)

    /**
     * declares a name with a type but no value, fails if it already exists
     */
    fun declare(
        name: String,
        type: Type,
        mutable: Boolean,
        span: Span,
    ): Result<Unit> {
        if (slots.containsKey(name)) {
            return Failure(Diagnostic.VariableAlreadyDeclared(name, span))
        }

        slots[name] = Slot.Declared(type, mutable)

        return Success(Unit)
    }

    /**
     * binds the first value to a declared name (`let x: number = 5;`)
     */
    fun initialize(
        name: String,
        value: Value,
        span: Span,
    ): Result<Unit> = bind(name, value, span, reassignment = false)

    /**
     * reassigns an already-declared variable (`x = 5;`)
     */
    fun assign(
        name: String,
        value: Value,
        span: Span,
    ): Result<Unit> = bind(name, value, span, reassignment = true)

    /**
     * reads a variable's value
     */
    fun lookup(
        name: String,
        span: Span,
    ): Result<Value> =
        when (val slot = find(name)) {
            null -> Failure(Diagnostic.VariableNotDeclared(name, span))
            is Slot.Declared -> Failure(Diagnostic.VariableWithoutValue(name, span))
            is Slot.Bound -> Success(slot.value)
        }

    fun declaredTypeOf(name: String): Type? = find(name)?.type

    private fun find(name: String): Slot? = slots[name] ?: parent?.find(name)

    private fun ownerOf(name: String): Environment? =
        if (slots.containsKey(name)) this else parent?.ownerOf(name)

    private fun bind(
        name: String,
        value: Value,
        span: Span,
        reassignment: Boolean,
    ): Result<Unit> {
        val owner =
            ownerOf(name)
                ?: return Failure(Diagnostic.VariableNotDeclared(name, span))

        val slot = requireNotNull(owner.slots[name])

        if (reassignment && !slot.mutable) {
            return Failure(Diagnostic.ConstantReassignment(name, span))
        }

        if (value.type != slot.type) {
            return Failure(
                Diagnostic.IncompatibleAssignment(
                    name = name,
                    declared = slot.type,
                    actual = value.type,
                    span = span,
                ),
            )
        }

        owner.slots[name] = Slot.Bound(value, slot.type, slot.mutable)

        return Success(Unit)
    }
}
