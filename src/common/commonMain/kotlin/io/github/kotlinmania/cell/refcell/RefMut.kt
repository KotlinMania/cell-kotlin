// port-lint: source tmp/cell/src/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/cell.rs lines 655-740
/**
 * Makes a new `RefMut` for a component of the borrowed data, e.g., an enum
 * variant.
 * 
 * The `RefCell` is already mutably borrowed, so this cannot fail.
 * 
 * This is an associated function that needs to be used as
 * `RefMut.map(...)`. A method would interfere with methods of the same
 * name on the contents of a `RefCell` used through `Deref`.
 * 
 * # Examples
 * 
 * ```
 * import RefCell, RefMut;
 * 
 * val c = RefCell((5, 'b'));
 * {
 *     val b1: RefMut<(Int, Char)> = c.borrowMut();
 *     var b2: RefMut<Int> = RefMut.map(b1, { t -> t.first);
 *     assert(*b2 == 5);
 *     *b2 = 42;
 * }
 * assert(*c.borrow() == (42, 'b'));
 * ```
 */
    fun map<U, F>(orig: RefMut<T>, f: F): RefMut<U>
    // where F: FnOnce(T): U
    {
        // FIXME(nll-rfc#40): fix borrow-check
        val RefMut { value, borrow } = orig;
        RefMut {
            value: f(value),
            borrow,
        }
    }

/**
 * Splits a `RefMut` into multiple `RefMut`s for different components of the
 * borrowed data.
 * 
 * The underlying `RefCell` will remain mutably borrowed until both
 * returned `RefMut`s go out of scope.
 * 
 * The `RefCell` is already mutably borrowed, so this cannot fail.
 * 
 * This is an associated function that needs to be used as
 * `RefMut.mapSplit(...)`. A method would interfere with methods of the
 * same name on the contents of a `RefCell` used through `Deref`.
 * 
 * # Examples
 * 
 * ```
 * import RefCell, RefMut;
 * 
 * val cell = RefCell([1, 2, 3, 4]);
 * val borrow = cell.borrowMut();
 * val (mut begin, mut end) = RefMut.mapSplit(borrow, { slice -> slice.splitAtMut(2));
 * assert(*begin == [1, 2]);
 * assert(*end == [3, 4]);
 * begin.copyFromSlice([4, 3]);
 * end.copyFromSlice([2, 1]);
 * ```
 */
    fun mapSplit<U, V, F>(
        orig: RefMut<T>, f: F
    ): (RefMut<U>, RefMut<V>)
    // where F: FnOnce(T): (U, V)
    {
        val (a, b) = f(orig.value);
        val borrow = orig.borrow.clone();
        (RefMut { value: a, borrow }, RefMut { value: b, borrow: orig.borrow })
    }

/**
 * Make a new `RefValMut` from the borrowed data.
 * 
 * The `RefCell` is already immutably borrowed, so this operation
 * cannot fail.
 */
    fun mapVal<U: Sized, F>(orig: RefMut<T>, f: F): RefValMut<U>
    // where F: FnOnce( mut T): U
    {
        RefValMut {
            value: f(orig.value),
            borrow: orig.borrow,
        }
    }

// slice: d-eso/cell.rs lines 787-815
/**
 * A wrapper type for a mutably borrowed value from a `RefCell<T>`.
 * 
 * See the [module-level documentation](index.html) for more.
 */
class RefMut<T> {
    val value:  mut T,
    val borrow: BorrowRefMut,
}

    typealias Target = T;

    fun deref(): T {
        this.value
    }

    fun derefMut(this): T {
        this.value
    }

    fun fmt(f: fmt.Formatter): fmt.Result {
        this.value.fmt(f)
    }
