// port-lint: source cell.rs
package io.github.kotlinmania.cell

import kotlin.concurrent.atomics.Cell

// Original work Copyright 2012-2014 The Rust Project Developers.
// Modified work Copyright 2018-2019 Daniel Mueller.
//
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0> or the MIT license
// http://opensource.org/licenses/MIT>, at your option.

// The upstream slice opens by importing Cell from the standard library,
// which maps to kotlin.concurrent.atomics.Cell; Ordering from std maps to
// the comparison result type below; Debug, Display, Formatter, and Result
// map to Kotlin's toString/toDebugString conventions; mem maps to plain
// value swapping; and Deref/DerefMut map to the public `value` property of
// each wrapper. UnsafeCell has no Kotlin counterpart and remains an
// unresolved seam.

/**
 * A mutable memory location with dynamically checked borrow rules.
 *
 * See the module-level documentation for more.
 */
class RefCell<T>(value: T) {
    internal val borrow: Cell<BorrowFlag> = Cell(UNUSED)
    internal val value: UnsafeCell<T> = UnsafeCell(value)

    /**
     * Creates a new [RefCell] containing `value`.
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     * ```
     */

    /**
     * Consumes the [RefCell], returning the wrapped value.
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     *
     * val five = c.intoInner()
     * ```
     */
    fun intoInner(): T {
        // Since this function takes the `RefCell` by value, the
        // caller statically guarantees that it is not currently borrowed.
        // Therefore the following check is just a debug check.
        check(this.borrow.load() == UNUSED)
        return this.value.intoInner()
    }

    /**
     * Replaces the wrapped value with a new one, returning the old value,
     * without deinitializing either one.
     *
     * This function corresponds to `mem.replace`.
     *
     * # Panics
     *
     * Panics if the value is currently borrowed.
     *
     * # Examples
     *
     * ```
     * val cell = RefCell(5)
     * val oldValue = cell.replace(6)
     * assert(oldValue == 5)
     * assert(cell == RefCell(6))
     * ```
     */
    fun replace(t: T): T {
        val mutBorrow = this.borrowMut()
        val old = mutBorrow.value
        mutBorrow.value = t
        mutBorrow.close()
        return old
    }

    /**
     * Replaces the wrapped value with a new one computed from `f`, returning
     * the old value, without deinitializing either one.
     *
     * # Panics
     *
     * Panics if the value is currently borrowed.
     *
     * # Examples
     *
     * ```
     * val cell = RefCell(5)
     * val oldValue = cell.replaceWith { old -> old + 1 }
     * assert(oldValue == 5)
     * assert(cell == RefCell(6))
     * ```
     */
    fun replaceWith(f: (T) -> T): T {
        val mutBorrow = this.borrowMut()
        val replacement = f(mutBorrow.value)
        val old = mutBorrow.value
        mutBorrow.value = replacement
        mutBorrow.close()
        return old
    }

    /**
     * Swaps the wrapped value of this cell with the wrapped value of `other`,
     * without deinitializing either one.
     *
     * This function corresponds to `mem.swap`.
     *
     * # Panics
     *
     * Panics if the value in either [RefCell] is currently borrowed.
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     * val d = RefCell(6)
     * c.swap(d)
     * assert(c == RefCell(6))
     * assert(d == RefCell(5))
     * ```
     */
    fun swap(other: RefCell<T>) {
        val mutA = this.borrowMut()
        val mutB = other.borrowMut()
        val tmp = mutA.value
        mutA.value = mutB.value
        mutB.value = tmp
        mutA.close()
        mutB.close()
    }

    /**
     * Immutably borrows the wrapped value.
     *
     * The borrow lasts until the returned [Ref] is released. Multiple
     * immutable borrows can be taken out at the same time.
     *
     * # Panics
     *
     * Panics if the value is currently mutably borrowed. For a non-panicking variant, use
     * [tryBorrow].
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     *
     * val borrowedFive = c.borrow()
     * val borrowedFive2 = c.borrow()
     * ```
     *
     * An example of panic:
     *
     * ```
     * val result = runCatching {
     *     val c = RefCell(5)
     *     val m = c.borrowMut()
     *
     *     val b = c.borrow() // this causes a panic
     * }
     *
     * assert(result.isFailure())
     * ```
     */
    fun borrow(): Ref<T> {
        return tryBorrow().getOrElse { throw IllegalStateException("already mutably borrowed") }
    }

    /**
     * Immutably borrows the wrapped value, returning an error if the value is currently mutably
     * borrowed.
     *
     * The borrow lasts until the returned [Ref] is released. Multiple immutable borrows can be
     * taken out at the same time.
     *
     * This is the non-panicking variant of [borrow].
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     *
     * run {
     *     val m = c.borrowMut()
     *     assert(c.tryBorrow().isFailure)
     *     m.close()
     * }
     *
     * run {
     *     val m = c.borrow()
     *     assert(c.tryBorrow().isSuccess)
     * }
     * ```
     */
    fun tryBorrow(): Result<Ref<T>, BorrowError> {
        return when (val b = BorrowRef.new(this.borrow)) {
            null -> Err(BorrowError())
            else -> {
                val result = Ok(Ref(this.value.get(), b))
                result
            }
        }
    }

    /**
     * Mutably borrows the wrapped value.
     *
     * The borrow lasts until the returned [RefMut] or all [RefMut]s derived
     * from it are released. The value cannot be borrowed while this borrow is
     * active.
     *
     * # Panics
     *
     * Panics if the value is currently borrowed. For a non-panicking variant, use
     * [tryBorrowMut].
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     *
     * c.borrowMut().value = 7
     * c.borrowMut().close()
     *
     * assert(c.borrow().value == 7)
     * ```
     *
     * An example of panic:
     *
     * ```
     * val result = runCatching {
     *     val c = RefCell(5)
     *     val m = c.borrow()
     *
     *     val b = c.borrowMut() // this causes a panic
     * }
     *
     * assert(result.isFailure())
     * ```
     */
    fun borrowMut(): RefMut<T> {
        return tryBorrowMut().getOrElse { throw IllegalStateException("already borrowed") }
    }

    /**
     * Mutably borrows the wrapped value, returning an error if the value is currently borrowed.
     *
     * The borrow lasts until the returned [RefMut] or all [RefMut]s derived
     * from it are released. The value cannot be borrowed while this borrow is
     * active.
     *
     * This is the non-panicking variant of [borrowMut].
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     *
     * run {
     *     val m = c.borrow()
     *     assert(c.tryBorrowMut().isFailure)
     * }
     *
     * assert(c.tryBorrowMut().isSuccess)
     * ```
     */
    fun tryBorrowMut(): Result<RefMut<T>, BorrowMutError> {
        return when (val b = BorrowRefMut.new(this.borrow)) {
            null -> Err(BorrowMutError())
            else -> Ok(RefMut(this.value.get(), b))
        }
    }

    /**
     * Returns a raw pointer to the underlying data in this cell.
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     *
     * val ptr = c.asPtr()
     * ```
     */
    fun asPtr(): CPointer<T> {
        return this.value.get()
    }

    /**
     * Returns a mutable reference to the underlying data.
     *
     * This call borrows the [RefCell] mutably (at compile-time) so there is no
     * need for dynamic checks.
     *
     * However be cautious: this method expects the cell to be mutable, which is
     * generally not the case when using a [RefCell]. Take a look at the
     * [borrowMut] method instead if the cell isn't mutable.
     *
     * Also, please be aware that this method is only for special circumstances and is usually
     * not what you want. In case of doubt, use [borrowMut] instead.
     *
     * # Examples
     *
     * ```
     * val c = RefCell(5)
     * c.getMut().value += 1
     *
     * assert(c == RefCell(6))
     * ```
     */
    fun getMut(): T {
        return this.value.get()
    }

    /**
     * # Panics
     *
     * Panics if the value is currently mutably borrowed.
     */
    fun copy(): RefCell<T> where T : Cloneable {
        return RefCell(this.borrow().value.clone())
    }

    /**
     * Creates a [RefCell], with the default value for T.
     */
    fun default(): RefCell<T> where T : Default {
        return RefCell(defaultOf())
    }

    /**
     * # Panics
     *
     * Panics if the value in either [RefCell] is currently borrowed.
     */
    override fun equals(other: Any?): Boolean {
        val o = other as RefCell<T>
        return this.borrow().value == o.borrow().value
    }

    /**
     * # Panics
     *
     * Panics if the value in either [RefCell] is currently borrowed.
     */
    fun partialCmp(other: RefCell<T>): Int? where T : PartialOrd {
        return this.borrow().value.partialCmp(o = other.borrow().value)
    }

    /**
     * # Panics
     *
     * Panics if the value in either [RefCell] is currently borrowed.
     */
    operator fun compareTo(other: RefCell<T>): Int where T : Comparable<T> {
        return this.borrow().value.compareTo(other.borrow().value)
    }

    /**
     * # Panics
     *
     * Panics if the value in either [RefCell] is currently borrowed.
     */
    fun lt(other: RefCell<T>): Boolean where T : PartialOrd {
        return this.borrow().value < other.borrow().value
    }

    /**
     * # Panics
     *
     * Panics if the value in either [RefCell] is currently borrowed.
     */
    fun le(other: RefCell<T>): Boolean where T : PartialOrd {
        return this.borrow().value <= other.borrow().value
    }

    /**
     * # Panics
     *
     * Panics if the value in either [RefCell] is currently borrowed.
     */
    fun gt(other: RefCell<T>): Boolean where T : PartialOrd {
        return this.borrow().value > other.borrow().value
    }

    /**
     * # Panics
     *
     * Panics if the value in either [RefCell] is currently borrowed.
     */
    fun ge(other: RefCell<T>): Boolean where T : PartialOrd {
        return this.borrow().value >= other.borrow().value
    }

    companion object {
        /**
         * Converts a `T` into a [RefCell].
         */
        fun <T> from(t: T): RefCell<T> {
            return RefCell(t)
        }
    }
}

// RefCell is safe to send across threads when T is.
// RefCell deliberately does not implement Sync: it contains an UnsafeCell,
// which is not safe to share between threads.

/**
 * An error returned by [RefCell.tryBorrow].
 */
class BorrowError private constructor() {
    /**
     * Debug formatting: renders the type name.
     */
    fun toDebugString(): String {
        return "BorrowError"
    }

    /**
     * Renders "already mutably borrowed".
     */
    override fun toString(): String {
        return "already mutably borrowed"
    }
}

/**
 * An error returned by [RefCell.tryBorrowMut].
 */
class BorrowMutError private constructor() {
    /**
     * Debug formatting: renders the type name.
     */
    fun toDebugString(): String {
        return "BorrowMutError"
    }

    /**
     * Renders "already borrowed".
     */
    override fun toString(): String {
        return "already borrowed"
    }
}

// Positive values represent the number of `Ref` active. Negative values
// represent the number of `RefMut` active. Multiple `RefMut`s can only be
// active at a time if they refer to distinct, nonoverlapping components of a
// `RefCell` (e.g., different ranges of a slice).
//
// `Ref` and `RefMut` are both two words in size, and so there will likely never
// be enough `Ref`s or `RefMut`s in existence to overflow half of the `Long`
// range. Thus, a `BorrowFlag` will probably never overflow or underflow.
// However, this is not a guarantee, as a pathological program could repeatedly
// create and then leak `Ref`s or `RefMut`s. Thus, all code must
// explicitly check for overflow and underflow in order to avoid unsafety, or at
// least behave correctly in the event that overflow or underflow happens (e.g.,
// see BorrowRef.new).
internal typealias BorrowFlag = Long

internal const val UNUSED: BorrowFlag = 0

internal fun isWriting(x: BorrowFlag): Boolean {
    return x < UNUSED
}

internal fun isReading(x: BorrowFlag): Boolean {
    return x > UNUSED
}

internal class BorrowRef(
    val borrow: Cell<BorrowFlag>,
) {
    companion object {
        fun new(borrow: Cell<BorrowFlag>): BorrowRef? {
            val b = borrow.load()
            if (isWriting(b) || b == Long.MAX_VALUE) {
                // If there's currently a writing borrow, or if incrementing the
                // refcount would overflow into a writing borrow.
                return null
            } else {
                borrow.store(b + 1)
                return BorrowRef(borrow)
            }
        }
    }

    // The upstream Drop for BorrowRef has no Kotlin counterpart: Kotlin has
    // no destructors, so the decrement performed there happens in close()
    // instead, called by the owning Ref when it is released.
    fun close() {
        val borrow = this.borrow.load()
        check(isReading(borrow))
        this.borrow.store(borrow - 1)
    }

    fun clone(): BorrowRef {
        // Since this Ref exists, we know the borrow flag
        // is a reading borrow.
        val borrow = this.borrow.load()
        check(isReading(borrow))
        // Prevent the borrow counter from overflowing into
        // a writing borrow.
        check(borrow != Long.MAX_VALUE)
        this.borrow.store(borrow + 1)
        return BorrowRef(this.borrow)
    }
}

/**
 * Wraps a borrowed reference to a value in a `RefCell` box.
 * A wrapper type for an immutably borrowed value from a [RefCell].
 *
 * See the module-level documentation for more.
 */
class Ref<T>(
    val value: T,
    internal val borrow: BorrowRef,
) {
    companion object {
        /**
         * Copies a [Ref].
         *
         * The [RefCell] is already immutably borrowed, so this cannot fail.
         *
         * This is an associated function that needs to be used as
         * `Ref.clone(...)`. A `Clone` implementation or a method would interfere
         * with the widespread use of `r.borrow().clone()` to clone the contents of
         * a [RefCell].
         */
        fun <T> clone(orig: Ref<T>): Ref<T> {
            return Ref(orig.value, orig.borrow.clone())
        }

        /**
         * Makes a new [Ref] for a component of the borrowed data.
         *
         * The [RefCell] is already immutably borrowed, so this cannot fail.
         *
         * This is an associated function that needs to be used as `Ref.map(...)`.
         * A method would interfere with methods of the same name on the contents
         * of a [RefCell] used through the public `value` property.
         *
         * # Examples
         *
         * ```
         * val c = RefCell(Pair(5, 'b'))
         * val b1: Ref<Pair<Int, Char>> = c.borrow()
         * val b2: Ref<Int> = Ref.map(b1) { t -> t.first }
         * assert(b2.value == 5)
         * ```
         */
        fun <T, U> map(orig: Ref<T>, f: (T) -> U): Ref<U> {
            return Ref(f(orig.value), orig.borrow)
        }

        /**
         * Splits a [Ref] into multiple [Ref]s for different components of the
         * borrowed data.
         *
         * The [RefCell] is already immutably borrowed, so this cannot fail.
         *
         * This is an associated function that needs to be used as
         * `Ref.mapSplit(...)`. A method would interfere with methods of the same
         * name on the contents of a [RefCell] used through the public `value`
         * property.
         *
         * # Examples
         *
         * ```
         * val cell = RefCell(intArrayOf(1, 2, 3, 4))
         * val borrow = cell.borrow()
         * val (begin, end) = Ref.mapSplit(borrow) { slice -> slice.splitAt(2) }
         * assert(begin.value contentEquals intArrayOf(1, 2))
         * assert(end.value contentEquals intArrayOf(3, 4))
         * ```
         */
        fun <T, U, V> mapSplit(orig: Ref<T>, f: (T) -> Pair<U, V>): Pair<Ref<U>, Ref<V>> {
            val (a, b) = f(orig.value)
            val borrow = orig.borrow.clone()
            return Pair(Ref(a, borrow), Ref(b, orig.borrow))
        }

        /**
         * Make a new [RefVal] from the borrowed data.
         *
         * The [RefCell] is already immutably borrowed, so this operation
         * cannot fail.
         */
        fun <T, U> mapVal(orig: Ref<T>, f: (T) -> U): RefVal<U> {
            return RefVal(f(orig.value), orig.borrow)
        }
    }

    /**
     * Releases the borrow. Kotlin has no destructors, so the decrement the
     * upstream Drop performs on scope exit happens here instead.
     */
    fun close() {
        this.borrow.close()
    }

    /**
     * Renders the underlying value.
     */
    override fun toString(): String {
        return this.value.toString()
    }
}

internal class BorrowRefMut(
    val borrow: Cell<BorrowFlag>,
) {
    companion object {
        // NOTE: Unlike BorrowRefMut.clone, new is called to create the initial
        // mutable reference, and so there must currently be no existing
        // references. Thus, while clone increments the mutable refcount, here
        // we explicitly only allow going from UNUSED to UNUSED - 1.
        fun new(borrow: Cell<BorrowFlag>): BorrowRefMut? {
            return when (borrow.load()) {
                UNUSED -> {
                    borrow.store(UNUSED - 1)
                    BorrowRefMut(borrow)
                }
                else -> null
            }
        }
    }

    // Clone a `BorrowRefMut`.
    //
    // This is only valid if each `BorrowRefMut` is used to track a mutable
    // reference to a distinct, nonoverlapping range of the original object.
    // This isn't in a Clone impl so that code doesn't call this implicitly.
    fun clone(): BorrowRefMut {
        val borrow = this.borrow.load()
        check(isWriting(borrow))
        // Prevent the borrow counter from underflowing.
        check(borrow != Long.MIN_VALUE)
        this.borrow.store(borrow - 1)
        return BorrowRefMut(this.borrow)
    }

    // The upstream Drop for BorrowRefMut has no Kotlin counterpart: Kotlin
    // has no destructors, so the increment performed there happens in
    // close() instead, called by the owning RefMut when it is released.
    fun close() {
        val borrow = this.borrow.load()
        check(isWriting(borrow))
        this.borrow.store(borrow + 1)
    }
}

/**
 * A wrapper type for a mutably borrowed value from a [RefCell].
 *
 * See the module-level documentation for more.
 */
class RefMut<T>(
    var value: T,
    internal val borrow: BorrowRefMut,
) {
    companion object {
        /**
         * Makes a new [RefMut] for a component of the borrowed data, e.g., an enum
         * variant.
         *
         * The [RefCell] is already mutably borrowed, so this cannot fail.
         *
         * This is an associated function that needs to be used as
         * `RefMut.map(...)`. A method would interfere with methods of the same
         * name on the contents of a [RefCell] used through the public `value`
         * property.
         *
         * # Examples
         *
         * ```
         * val c = RefCell(Pair(5, 'b'))
         * run {
         *     val b1: RefMut<Pair<Int, Char>> = c.borrowMut()
         *     val b2: RefMut<Int> = RefMut.map(b1) { t -> t.first }
         *     assert(b2.value == 5)
         *     b2.value = 42
         * }
         * assert(c.borrow().value == Pair(42, 'b'))
         * ```
         */
        fun <T, U> map(orig: RefMut<T>, f: (T) -> U): RefMut<U> {
            return RefMut(f(orig.value), orig.borrow)
        }

        /**
         * Splits a [RefMut] into multiple [RefMut]s for different components of the
         * borrowed data.
         *
         * The underlying [RefCell] will remain mutably borrowed until both
         * returned [RefMut]s are released.
         *
         * The [RefCell] is already mutably borrowed, so this cannot fail.
         *
         * This is an associated function that needs to be used as
         * `RefMut.mapSplit(...)`. A method would interfere with methods of the
         * same name on the contents of a [RefCell] used through the public
         * `value` property.
         *
         * # Examples
         *
         * ```
         * val cell = RefCell(intArrayOf(1, 2, 3, 4))
         * val borrow = cell.borrowMut()
         * val (begin, end) = RefMut.mapSplit(borrow) { slice -> slice.splitAtMut(2) }
         * assert(begin.value contentEquals intArrayOf(1, 2))
         * assert(end.value contentEquals intArrayOf(3, 4))
         * begin.value = intArrayOf(4, 3)
         * end.value = intArrayOf(2, 1)
         * ```
         */
        fun <T, U, V> mapSplit(orig: RefMut<T>, f: (T) -> Pair<U, V>): Pair<RefMut<U>, RefMut<V>> {
            val (a, b) = f(orig.value)
            val borrow = orig.borrow.clone()
            return Pair(RefMut(a, borrow), RefMut(b, orig.borrow))
        }

        /**
         * Make a new [RefValMut] from the borrowed data.
         *
         * The [RefCell] is already immutably borrowed, so this operation
         * cannot fail.
         */
        fun <T, U> mapVal(orig: RefMut<T>, f: (T) -> U): RefValMut<U> {
            return RefValMut(f(orig.value), orig.borrow)
        }
    }

    /**
     * Releases the borrow. Kotlin has no destructors, so the increment the
     * upstream Drop performs on scope exit happens here instead.
     */
    fun close() {
        this.borrow.close()
    }

    /**
     * Renders the underlying value.
     */
    override fun toString(): String {
        return this.value.toString()
    }
}

/**
 * A type containing a value that contains a borrowed reference to a
 * value from a [RefCell].
 *
 * See the module-level documentation for more.
 */
class RefVal<T>(
    val value: T,
    internal val borrow: BorrowRef,
) {
    companion object {
        /**
         * Copies a [RefVal].
         *
         * The [RefCell] is already immutably borrowed, so this cannot fail.
         *
         * This is an associated function that needs to be used as
         * `Ref.clone(...)`. A `Clone` implementation or a method would interfere
         * with the widespread use of `r.borrow().clone()` to clone the contents of
         * a [RefCell].
         */
        fun <T> clone(orig: RefVal<T>): RefVal<T> where T : Cloneable {
            return RefVal(orig.value.clone(), orig.borrow.clone())
        }

        /**
         * Make a new [RefVal] from another [RefVal].
         *
         * The [RefCell] is already immutably borrowed, so this operation
         * cannot fail.
         */
        fun <T, U> map(orig: RefVal<T>, f: (T) -> U): RefVal<U> {
            return RefVal(f(orig.value), orig.borrow)
        }
    }

    /**
     * Releases the borrow. Kotlin has no destructors, so the decrement the
     * upstream Drop performs on scope exit happens here instead.
     */
    fun close() {
        this.borrow.close()
    }

    /**
     * Renders the underlying value.
     */
    override fun toString(): String {
        return this.value.toString()
    }
}

/**
 * A type containing a value that contains a borrowed mutable reference
 * to a value from a [RefCell].
 *
 * See the module-level documentation for more.
 */
class RefValMut<T>(
    var value: T,
    internal val borrow: BorrowRefMut,
) {
    companion object {
        /**
         * Make a new [RefValMut] from another [RefValMut].
         *
         * The [RefCell] is already mutably borrowed, so this operation
         * cannot fail.
         */
        fun <T, U> map(orig: RefValMut<T>, f: (T) -> U): RefValMut<U> {
            return RefValMut(f(orig.value), orig.borrow)
        }
    }

    /**
     * Releases the borrow. Kotlin has no destructors, so the increment the
     * upstream Drop performs on scope exit happens here instead.
     */
    fun close() {
        this.borrow.close()
    }

    /**
     * Renders the underlying value.
     */
    override fun toString(): String {
        return this.value.toString()
    }
}