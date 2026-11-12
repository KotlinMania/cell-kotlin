// port-lint: source tmp/cell/src/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/cell.rs lines 499-540
class BorrowRef {
    val borrow:  Cell<BorrowFlag>,
}

    fun new(borrow:  Cell<BorrowFlag>): BorrowRef? {
        val b = borrow.get();
        if isWriting(b) || b == Long.maxValue() {
            // If there's currently a writing borrow, or if incrementing the
            // refcount would overflow into a writing borrow.
            null
        } else {
            borrow.set(b + 1);
            BorrowRef { borrow }
        }
    }

    fun drop(this) {
        val borrow = this.borrow.get();
        assert(isReading(borrow));
        this.borrow.set(borrow - 1);
    }

    fun clone(): Self {
        // Since this Ref exists, we know the borrow flag
        // is a reading borrow.
        val borrow = this.borrow.get();
        assert(isReading(borrow));
        // Prevent the borrow counter from overflowing into
        // a writing borrow.
        assert(borrow != Long.maxValue());
        this.borrow.set(borrow + 1);
        BorrowRef { borrow: this.borrow }
    }
