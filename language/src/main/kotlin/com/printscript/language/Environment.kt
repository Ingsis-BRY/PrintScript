package com.printscript.language

import com.printscript.ast.Type
import com.printscript.common.Span
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success

sealed interface Slot<out V> {
    val type: Type
    val mutable: Boolean

    data class Declared(
        override val type: Type,
        override val mutable: Boolean,
    ) : Slot<Nothing>

    data class Bound<out V>(
        val value: V,
        override val type: Type,
        override val mutable: Boolean,
    ) : Slot<V>
}

class Environment<V> private constructor(
    private val parent: Environment<V>?,
) {
    constructor() : this(null)

    private val slots: MutableMap<String, Slot<V>> = mutableMapOf()

    fun child(): Environment<V> = Environment(this)

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

    fun initialize(
        name: String,
        value: V,
        type: Type,
        span: Span,
    ): Result<Unit> = bind(name, value, type, span, reassignment = false)

    fun assign(
        name: String,
        value: V,
        type: Type,
        span: Span,
    ): Result<Unit> = bind(name, value, type, span, reassignment = true)

    fun lookup(
        name: String,
        span: Span,
    ): Result<V> = assigned(name, span) { slot -> slot.value }

    fun typeOf(
        name: String,
        span: Span,
    ): Result<Type> = assigned(name, span) { slot -> slot.type }

    fun declaredTypeOf(name: String): Type? = find(name)?.type

    private fun <R> assigned(
        name: String,
        span: Span,
        read: (Slot.Bound<V>) -> R,
    ): Result<R> =
        when (val slot = find(name)) {
            null -> Failure(Diagnostic.VariableNotDeclared(name, span))
            is Slot.Declared -> Failure(Diagnostic.VariableWithoutValue(name, span))
            is Slot.Bound -> Success(read(slot))
        }

    private fun find(name: String): Slot<V>? = slots[name] ?: parent?.find(name)

    private fun ownerOf(name: String): Environment<V>? =
        if (slots.containsKey(name)) this else parent?.ownerOf(name)

    private fun bind(
        name: String,
        value: V,
        type: Type,
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

        if (type != slot.type) {
            return Failure(
                Diagnostic.IncompatibleAssignment(
                    name = name,
                    declared = slot.type,
                    actual = type,
                    span = span,
                ),
            )
        }

        owner.slots[name] = Slot.Bound(value, slot.type, slot.mutable)

        return Success(Unit)
    }
}
