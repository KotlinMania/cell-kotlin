// port-lint: source tmp/cell/src/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/cell.rs lines 542-653
/**
 * Wraps a borrowed reference to a value in a `RefCell` box.
 * A wrapper type for an immutably borrowed value from a `RefCell<T>`.
 * 
 * See the [module-level documentation](index.html) for more.
 */
class Ref<T> {
    val value:  T,
    val borrow: BorrowRef,
}

    typealias Target = T;

    fun deref(): T {
        this.value
    }

/**
 * Copies a `Ref`.
 * 
 * The `RefCell` is already immutably borrowed, so this cannot fail.
 * 
 * This is an associated function that needs to be used as
 * `Ref.clone(...)`. A `Clone` implementation or a method would interfere
 * with the widespread import of `r.borrow().clone()` to clone the contents of
 * a `RefCell`.
 */
    fun clone(orig: Ref<T>): Ref<T> {
        Ref {
            value: orig.value,
            borrow: orig.borrow.clone(),
        }
    }

/**
 * Makes a new `Ref` for a component of the borrowed data.
 * 
 * The `RefCell` is already immutably borrowed, so this cannot fail.
 * 
 * This is an associated function that needs to be used as `Ref.map(...)`.
 * A method would interfere with methods of the same name on the contents
 * of a `RefCell` used through `Deref`.
 * 
 * # Examples
 * 
 * ```
 * import RefCell, Ref;
 * 
 * val c = RefCell((5, 'b'));
 * val b1: Ref<(Int, Char)> = c.borrow();
 * val b2: Ref<Int> = Ref.map(b1, { t -> t.first);
 * assert(*b2 == 5)
 * ```
 */
    fun map<U, F>(orig: Ref<T>, f: F): Ref<U>
    // where F: FnOnce(T): U
    {
        Ref {
            value: f(orig.value),
            borrow: orig.borrow,
        }
    }

/**
 * Splits a `Ref` into multiple `Ref`s for different components of the
 * borrowed data.
 * 
 * The `RefCell` is already immutably borrowed, so this cannot fail.
 * 
 * This is an associated function that needs to be used as
 * `Ref.mapSplit(...)`. A method would interfere with methods of the same
 * name on the contents of a `RefCell` used through `Deref`.
 * 
 * # Examples
 * 
 * ```
 * import Ref, RefCell;
 * 
 * val cell = RefCell([1, 2, 3, 4]);
 * val borrow = cell.borrow();
 * val (begin, end) = Ref.mapSplit(borrow, { slice -> slice.splitAt(2));
 * assert(*begin == [1, 2]);
 * assert(*end == [3, 4]);
 * ```
 */
    fun mapSplit<U, V, F>(orig: Ref<T>, f: F): (Ref<U>, Ref<V>)
    // where F: FnOnce(T): (U, V)
    {
        val (a, b) = f(orig.value);
        val borrow = orig.borrow.clone();
        (Ref { value: a, borrow }, Ref { value: b, borrow: orig.borrow })
    }

/**
 * Make a new `RefVal` from the borrowed data.
 * 
 * The `RefCell` is already immutably borrowed, so this operation
 * cannot fail.
 */
    fun mapVal<U: Sized, F>(orig: Ref<T>, f: F): RefVal<U>
    // where F: FnOnce( T): U
    {
        RefVal {
            value: f(orig.value),
            borrow: orig.borrow,
        }
    }

    fun fmt(f: fmt.Formatter): fmt.Result {
        this.value.fmt(f)
    }
