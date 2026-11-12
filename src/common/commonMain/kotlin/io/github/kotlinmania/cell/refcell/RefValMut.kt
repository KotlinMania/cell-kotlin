// port-lint: source tmp/cell/src/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/cell.rs lines 884-930

/**
 * A type containing a value that contains a borrowed mutable reference
 * to a value from a `RefCell<T>`.
 * 
 * See the [module-level documentation](index.html) for more.
 */
class RefValMut<T> {
    val value: T,
    val borrow: BorrowRefMut,
}

/**
 * Make a new `RefValMut` from the another `RefValMut`.
 * 
 * The `RefCell` is already mutably borrowed, so this operation
 * cannot fail.
 */
    fun map<U: Sized, F>(orig: RefValMut<T>, f: F): RefValMut<U>
    // where F: FnOnce(T): U
    {
        RefValMut {
            value: f(orig.value),
            borrow: orig.borrow,
        }
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
