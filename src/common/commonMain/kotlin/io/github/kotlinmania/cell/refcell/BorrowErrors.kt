// port-lint: source tmp/cell/src/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/cell.rs lines 32-64
/**
 * An error returned by [`RefCell.tryBorrow`](struct.RefCell.html#method.tryBorrow).
 */
class BorrowError {
    val _private: (),
}

    fun fmt(f: fmt.Formatter): fmt.Result {
        f.debugStruct("BorrowError").finish()
    }

    fun fmt(f: fmt.Formatter): fmt.Result {
        Display.fmt("already mutably borrowed", f)
    }

/**
 * An error returned by [`RefCell.tryBorrowMut`](struct.RefCell.html#method.tryBorrowMut).
 */
class BorrowMutError {
    val _private: (),
}

    fun fmt(f: fmt.Formatter): fmt.Result {
        f.debugStruct("BorrowMutError").finish()
    }

    fun fmt(f: fmt.Formatter): fmt.Result {
        Display.fmt("already borrowed", f)
    }
