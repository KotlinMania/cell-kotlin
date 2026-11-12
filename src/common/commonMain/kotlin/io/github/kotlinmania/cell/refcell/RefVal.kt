// port-lint: source tmp/cell/src/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/cell.rs lines 817-882

/**
 * A type containing a value that contains a borrowed reference to a
 * value from a `RefCell<T>`.
 * 
 * See the [module-level documentation](index.html) for more.
 */
class RefVal<T> {
    val value: T,
    val borrow: BorrowRef,
}

/**
 * Copies a `RefVal`.
 * 
 * The `RefCell` is already immutably borrowed, so this cannot fail.
 * 
 * This is an associated function that needs to be used as
 * `Ref.clone(...)`.  A `Clone` implementation or a method would interfere
 * with the widespread import of `r.borrow().clone()` to clone the contents of
 * a `RefCell`.
 */
    fun clone(orig: RefVal<T>): RefVal<T>
    // where T: Clone
    {
        RefVal {
            value: orig.value.clone(),
            borrow: orig.borrow.clone(),
        }
    }

/**
 * Make a new `RefVal` from the another `RefVal`.
 * 
 * The `RefCell` is already immutably borrowed, so this operation
 * cannot fail.
 */
    fun map<U: Sized, F>(orig: RefVal<T>, f: F): RefVal<U>
    // where F: FnOnce(T): U
    {
        RefVal {
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
