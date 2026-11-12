// port-lint: source tmp/cell/src/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/cell.rs lines 742-785
class BorrowRefMut {
    val borrow:  Cell<BorrowFlag>,
}

    fun drop(this) {
        val borrow = this.borrow.get();
        assert(isWriting(borrow));
        this.borrow.set(borrow + 1);
    }

    fun new(borrow:  Cell<BorrowFlag>): BorrowRefMut? {
        // NOTE: Unlike BorrowRefMut.clone, new is called to create the initial
        // mutable reference, and so there must currently be no existing
        // references. Thus, while clone increments the mutable refcount, here
        // we explicitly only allow going from UNUSED to UNUSED - 1.
        when borrow.get() {
            UNUSED -> {
                borrow.set(UNUSED - 1);
                BorrowRefMut { borrow }
            },
            _ -> null,
        }
    }

    // Clone a `BorrowRefMut`.
    //
    // This is only valid if each `BorrowRefMut` is used to track a mutable
    // reference to a distinct, nonoverlapping range of the original object.
    // This isn't in a Clone impl so that code doesn't call this implicitly.
    fun clone(): BorrowRefMut {
        val borrow = this.borrow.get();
        assert(isWriting(borrow));
        // Prevent the borrow counter from underflowing.
        assert(borrow != Long.minValue());
        this.borrow.set(borrow - 1);
        BorrowRefMut { borrow: this.borrow }
    }
