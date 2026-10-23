package io.github.kotlinmania.cell

// Utilities for formatting and printing strings.
//
// Derived from the upstream formatting module: Debug implementations for
// RefCell, Ref, RefMut, RefVal, and RefValMut.

/**
 * Debug formatting: renders the [RefCell] and its value, or a placeholder
 * when the value is mutably borrowed.
 */
fun <T> RefCell<T>.toDebugString(): String {
    return when (val borrow = this.tryBorrow()) {
        is Ok -> "RefCell { value: ${borrow.value.value.toDebugString()} }"
        is Err -> {
            // The RefCell is mutably borrowed so we can't look at its value
            // here. Show a placeholder instead.
            "RefCell { value: <borrowed> }"
        }
    }
}

/**
 * Debug formatting: renders the underlying value.
 */
fun <T> Ref.toDebugString(): String {
    return this.value.toDebugString()
}

/**
 * Debug formatting: renders the underlying value.
 */
fun <T> RefMut<T>.toDebugString(): String {
    return this.value.toDebugString()
}

/**
 * Debug formatting: renders the underlying value.
 */
fun <T> RefVal<T>.toDebugString(): String {
    return this.value.toDebugString()
}

/**
 * Debug formatting: renders the underlying value.
 */
fun <T> RefValMut<T>.toDebugString(): String {
    return this.value.toDebugString()
}