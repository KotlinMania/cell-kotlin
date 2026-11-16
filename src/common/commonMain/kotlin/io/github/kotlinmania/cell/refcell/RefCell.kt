// port-lint: source tmp/cell/src/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/cell.rs lines 1-30
// Copyright 2012-2014 The Rust Project Developers. See the COPYRIGHT
// file at the top-level directory of this distribution and at
// http://rust-lang.org/COPYRIGHT.
//
// Modified work Copyright 2018-2019 Daniel Mueller (deso@posteo.net).
//
// Licensed under the Apache License, Version 2.first <LICENSE-APACHE or
// http://www.apache.org/licenses/LICENSE-2.first> or the MIT license
// <LICENSE-MIT or http://opensource.org/licenses/MIT>, at your
// option. This file may not be copied, modified, or distributed
// except according to those terms.

import std.cell.Cell;
import std.cell.UnsafeCell;
import std.cmp.Ordering;
import std.fmt;
import std.fmt.Debug;
import std.fmt.Display;
import std.mem;
import std.ops.Deref;
import std.ops.DerefMut;


/**
 * A mutable memory location with dynamically checked borrow rules
 * 
 * See the [module-level documentation](index.html) for more.
 */
class RefCell<T> {
    val borrow: Cell<BorrowFlag>,
    val value: UnsafeCell<T>,
}

// slice: d-eso/cell.rs lines 92-199
/**
 * Creates a new `RefCell` containing `value`.
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * 
 * val c = RefCell(5);
 * ```
 */
    const fun new(value: T): RefCell<T> {
        RefCell {
            value: UnsafeCell(value),
            borrow: Cell(UNUSED),
        }
    }

/**
 * Consumes the `RefCell`, returning the wrapped value.
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * 
 * val c = RefCell(5);
 * 
 * val five = c.intoInner();
 * ```
 */
    fun intoInner(this): T {
        // Since this function takes `this` (the `RefCell`) by value, the
        // compiler statically verifies that it is not currently borrowed.
        // Therefore the following assertion is just a `debugAssert!`.
        assert(this.borrow.get() == UNUSED);
        this.value.intoInner()
    }

/**
 * Replaces the wrapped value with a new one, returning the old value,
 * without deinitializing either one.
 * 
 * This function corresponds to [`std.mem.replace`](../mem/fn.replace.html).
 * 
 * # Panics
 * 
 * Panics if the value is currently borrowed.
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * val cell = RefCell(5);
 * val oldValue = cell.replace(6);
 * assert(oldValue == 5);
 * assert(cell == RefCell(6));
 * ```
 */
    fun replace(t: T): T {
        mem.replace(*this.borrowMut(), t)
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
 * import std.cell.RefCell;
 * val cell = RefCell(5);
 * val oldValue = cell.replaceWith({ old -> old + 1);
 * assert(oldValue == 5);
 * assert(cell == RefCell(6));
 * ```
 */
    fun replaceWith<F: FnOnce(T): T>(f: F) -> T {
        val mutBorrow = *this.borrowMut();
        val replacement = f(mutBorrow);
        mem.replace(mutBorrow, replacement)
    }

/**
 * Swaps the wrapped value of `this` with the wrapped value of `other`,
 * without deinitializing either one.
 * 
 * This function corresponds to [`std.mem.swap`](../mem/fn.swap.html).
 * 
 * # Panics
 * 
 * Panics if the value in either `RefCell` is currently borrowed.
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * val c = RefCell(5);
 * val d = RefCell(6);
 * c.swap(d);
 * assert(c == RefCell(6));
 * assert(d == RefCell(5));
 * ```
 */
    fun swap(other: Self) {
        mem.swap(*this.borrowMut(), *other.borrowMut())
    }

// slice: d-eso/cell.rs lines 201-401
/**
 * Immutably borrows the wrapped value.
 * 
 * The borrow lasts until the returned `Ref` exits scope. Multiple
 * immutable borrows can be taken out at the same time.
 * 
 * # Panics
 * 
 * Panics if the value is currently mutably borrowed. For a non-panicking variant, use
 * [`tryBorrow`](#method.tryBorrow).
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * 
 * val c = RefCell(5);
 * 
 * val borrowedFive = c.borrow();
 * val borrowedFive2 = c.borrow();
 * ```
 * 
 * An example of panic:
 * 
 * ```
 * import std.cell.RefCell;
 * import std.thread;
 * 
 * val result = thread.spawn(move || {
 *    val c = RefCell(5);
 *    val m = c.borrowMut();
 * 
 *    val b = c.borrow(); // this causes a panic
 * }).join();
 * 
 * assert(result.isErr());
 * ```
 */
    fun borrow(): Ref<T> {
        this.tryBorrow().expect("already mutably borrowed")
    }

/**
 * Immutably borrows the wrapped value, returning an error if the value is currently mutably
 * borrowed.
 * 
 * The borrow lasts until the returned `Ref` exits scope. Multiple immutable borrows can be
 * taken out at the same time.
 * 
 * This is the non-panicking variant of [`borrow`](#method.borrow).
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * 
 * val c = RefCell(5);
 * 
 * {
 *     val m = c.borrowMut();
 *     assert(c.tryBorrow().isErr());
 * }
 * 
 * {
 *     val m = c.borrow();
 *     assert(c.tryBorrow().isOk());
 * }
 * ```
 */
    fun tryBorrow(): Result<Ref<T>, BorrowError> {
        when BorrowRef(this.borrow) {
            b -> Ok(Ref {
                value: run { *this.value.get() },
                borrow: b,
            }),
            null -> Err(BorrowError { _private: () }),
        }
    }

/**
 * Mutably borrows the wrapped value.
 * 
 * The borrow lasts until the returned `RefMut` or all `RefMut`s derived
 * from it exit scope. The value cannot be borrowed while this borrow is
 * active.
 * 
 * # Panics
 * 
 * Panics if the value is currently borrowed. For a non-panicking variant, use
 * [`tryBorrowMut`](#method.tryBorrowMut).
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * 
 * val c = RefCell(5);
 * 
 * *c.borrowMut() = 7;
 * 
 * assert(*c.borrow() == 7);
 * ```
 * 
 * An example of panic:
 * 
 * ```
 * import std.cell.RefCell;
 * import std.thread;
 * 
 * val result = thread.spawn(move || {
 *    val c = RefCell(5);
 *    val m = c.borrow();
 * 
 *    val b = c.borrowMut(); // this causes a panic
 * }).join();
 * 
 * assert(result.isErr());
 * ```
 */
    fun borrowMut(): RefMut<T> {
        this.tryBorrowMut().expect("already borrowed")
    }

/**
 * Mutably borrows the wrapped value, returning an error if the value is currently borrowed.
 * 
 * The borrow lasts until the returned `RefMut` or all `RefMut`s derived
 * from it exit scope. The value cannot be borrowed while this borrow is
 * active.
 * 
 * This is the non-panicking variant of [`borrowMut`](#method.borrowMut).
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * 
 * val c = RefCell(5);
 * 
 * {
 *     val m = c.borrow();
 *     assert(c.tryBorrowMut().isErr());
 * }
 * 
 * assert(c.tryBorrowMut().isOk());
 * ```
 */
    fun tryBorrowMut(): Result<RefMut<T>, BorrowMutError> {
        when BorrowRefMut(this.borrow) {
            b -> Ok(RefMut {
                value: run { *this.value.get() },
                borrow: b,
            }),
            null -> Err(BorrowMutError { _private: () }),
        }
    }

/**
 * Returns a raw pointer to the underlying data in this cell.
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * 
 * val c = RefCell(5);
 * 
 * val ptr = c.asPtr();
 * ```
 */
    fun asPtr(): CPointer<T> {
        this.value.get()
    }

/**
 * Returns a mutable reference to the underlying data.
 * 
 * This call borrows `RefCell` mutably (at compile-time) so there is no
 * need for dynamic checks.
 * 
 * However be cautious: this method expects `this` to be mutable, which is
 * generally not the case when using a `RefCell`. Take a look at the
 * [`borrowMut`] method instead if `this` isn't mutable.
 * 
 * Also, please be aware that this method is only for special circumstances and is usually
 * not what you want. In case of doubt, import [`borrowMut`] instead.
 * 
 * [`borrowMut`]: #method.borrowMut
 * 
 * # Examples
 * 
 * ```
 * import std.cell.RefCell;
 * 
 * var c = RefCell(5);
 * *c.getMut() += 1;
 * 
 * assert(c == RefCell(6));
 * ```
 */
    fun getMut(this): T {
        run {
            *this.value.get()
        }
    }

// slice: d-eso/cell.rs lines 403-409

// Note that `RefCell` does not explicitly have a negative trait bound
// for `Sync` as negative trait bounds are only available on nightly but
// we want to be available on stable. However, `RefCell` still won't
// such a negative trait bound for `Sync`.

// slice: d-eso/cell.rs lines 411-497
/**
 * # Panics
 * 
 * Panics if the value is currently mutably borrowed.
 */
    fun clone(): RefCell<T> {
        RefCell(this.borrow().clone())
    }

/**
 * Creates a `RefCell<T>`, with the `Default` value for T.
 */
    fun default(): RefCell<T> {
        RefCell(Default.default())
    }

/**
 * # Panics
 * 
 * Panics if the value in either `RefCell` is currently borrowed.
 */
    fun eq(other: RefCell<T>): Boolean {
        *this.borrow() == *other.borrow()
    }


/**
 * # Panics
 * 
 * Panics if the value in either `RefCell` is currently borrowed.
 */
    fun partialCmp(other: RefCell<T>): Ordering? {
        this.borrow().partialCmp(*other.borrow())
    }

/**
 * # Panics
 * 
 * Panics if the value in either `RefCell` is currently borrowed.
 */
    fun lt(other: RefCell<T>): Boolean {
        *this.borrow() < *other.borrow()
    }

/**
 * # Panics
 * 
 * Panics if the value in either `RefCell` is currently borrowed.
 */
    fun le(other: RefCell<T>): Boolean {
        *this.borrow() <= *other.borrow()
    }

/**
 * # Panics
 * 
 * Panics if the value in either `RefCell` is currently borrowed.
 */
    fun gt(other: RefCell<T>): Boolean {
        *this.borrow() > *other.borrow()
    }

/**
 * # Panics
 * 
 * Panics if the value in either `RefCell` is currently borrowed.
 */
    fun ge(other: RefCell<T>): Boolean {
        *this.borrow() >= *other.borrow()
    }

/**
 * # Panics
 * 
 * Panics if the value in either `RefCell` is currently borrowed.
 */
    fun cmp(other: RefCell<T>): Ordering {
        this.borrow().cmp(*other.borrow())
    }

    fun from(t: T): RefCell<T> {
        RefCell(t)
    }
