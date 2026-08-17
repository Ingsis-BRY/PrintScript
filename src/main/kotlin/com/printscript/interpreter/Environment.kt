package com.printscript.interpreter

import com.printscript.ast.Type
import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Result
import com.printscript.common.Success

/**
* [Declared] holds the type but no value (`let x: number;`),
* [Bound] holds the value. absent from the map means not declared.
*/
sealed interface Slot {
    data class Declared(val type: Type) : Slot
    data class Bound(val value: Value) : Slot
}

/**
* the interpreter's variable store, the only mutable component, on purpose.
* every operation returns a [Result], failing with a [Diagnostic] at [Position]
*/
class Environment {

    private val slots: MutableMap<String, Slot> = mutableMapOf()

    /**
    * declares a name with a type but no value, fails if it already exists
    */
    fun declare(name: String, type: Type, position: Position): Result<Unit> {
        if (slots.containsKey(name)) {
            return failure("Variable '$name' is already declared.", position)
        }
        slots[name] = Slot.Declared(type)
        return Success(Unit)
    }

    /**
    * binds the first value to a declared name (`let x: number = 5;`)
    */
    fun initialize(name: String, value: Value, position: Position): Result<Unit> =
        bind(name, value, position)

    /**
    * reassigns an already-declared variable (`x = 5;`)
    */
    fun assign(name: String, value: Value, position: Position): Result<Unit> =
        bind(name, value, position)

    /**
    * reads a variable's value
    */
    fun lookup(name: String, position: Position): Result<Value> =
        when (val slot = slots[name]) {
            null -> failure("Variable '$name' is not declared.", position)
            is Slot.Declared -> failure("Variable '$name' is used before it has a value.", position)
            is Slot.Bound -> Success(slot.value)
        }

    private fun bind(name: String, value: Value, position: Position): Result<Unit> {
        val declaredType = when (val slot = slots[name]) {
            null -> return failure("Variable '$name' is not declared.", position)
            is Slot.Declared -> slot.type
            is Slot.Bound -> slot.value.type
        }
        if (value.type != declaredType) {
            return failure(
                "Cannot assign a ${typeName(value.type)} value to variable " +
                    "'$name' of type ${typeName(declaredType)}.",
                position
            )
        }
        slots[name] = Slot.Bound(value)
        return Success(Unit)
    }

    private fun failure(message: String, position: Position): Failure =
        Failure(Diagnostic(message, position))

    private fun typeName(type: Type): String = when (type) {
        Type.NumberType -> "number"
        Type.StringType -> "string"
    }
}
