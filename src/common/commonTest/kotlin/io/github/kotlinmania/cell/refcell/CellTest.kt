// port-lint: tests tmp/cell/tests/cell.rs
package io.github.kotlinmania.cell.refcell

// slice: d-eso/tests/cell.rs lines 13-420
import std.cell.Cell;
import std.mem.drop;
import std.slice.Iter;

import cell.Ref;
import cell.RefCell;
import cell.RefMut;
import cell.RefVal;


    @Test
fun refAndRefmutHaveSensibleShow() {
    val refcell = RefCell("foo");

    val refcellRefmut = refcell.borrowMut();
    assert(refcellRefmut.toDebugString().contains("foo"));
    drop(refcellRefmut);

    val refcellRef = refcell.borrow();
    assert(refcellRef.toDebugString().contains("foo"));
    drop(refcellRef);
}

    @Test
fun doubleImmBorrow() {
    val x = RefCell(0);
    val _b1 = x.borrow();
    x.borrow();
}

    @Test
fun noMutThenImmBorrow() {
    val x = RefCell(0);
    val _b1 = x.borrowMut();
    assert(x.tryBorrow().isErr());
}

    @Test
fun noImmThenBorrowMut() {
    val x = RefCell(0);
    val _b1 = x.borrow();
    assert(x.tryBorrowMut().isErr());
}

    @Test
fun noDoubleBorrowMut() {
    val x = RefCell(0);
    assert(x.tryBorrow().isOk());
    val _b1 = x.borrowMut();
    assert(x.tryBorrow().isErr());
}

    @Test
fun immReleaseBorrowMut() {
    val x = RefCell(0);
    {
        val _b1 = x.borrow();
    }
    x.borrowMut();
}

    @Test
fun mutReleaseBorrowMut() {
    val x = RefCell(0);
    {
        val _b1 = x.borrowMut();
    }
    x.borrow();
}

    @Test
fun doubleBorrowSingleReleaseNoBorrowMut() {
    val x = RefCell(0);
    val _b1 = x.borrow();
    {
        val _b2 = x.borrow();
    }
    assert(x.tryBorrow().isOk());
    assert(x.tryBorrowMut().isErr());
}

    @Test
    // shouldPanic
fun discardDoesntUnborrow() {
    val x = RefCell(0);
    val _b = x.borrow();
    val _ = _b;
    val _b = x.borrowMut();
}

    @Test
fun refCloneUpdatesFlag() {
    val x = RefCell(0);
    {
        val b1 = x.borrow();
        assert(x.tryBorrow().isOk());
        assert(x.tryBorrowMut().isErr());
        {
            val _b2 = Ref.clone(b1);
            assert(x.tryBorrow().isOk());
            assert(x.tryBorrowMut().isErr());
        }
        assert(x.tryBorrow().isOk());
        assert(x.tryBorrowMut().isErr());
    }
    assert(x.tryBorrow().isOk());
    assert(x.tryBorrowMut().isOk());
}

    @Test
fun refMapDoesNotUpdateFlag() {
    val x = RefCell(5);
    {
        val b1: Ref<Int?> = x.borrow();
        assert(x.tryBorrow().isOk());
        assert(x.tryBorrowMut().isErr());
        {
            val b2: Ref<Int> = Ref.map(b1, { o -> o.asRef()!!);
            assert(*b2 == 5);
            assert(x.tryBorrow().isOk());
            assert(x.tryBorrowMut().isErr());
        }
        assert(x.tryBorrow().isOk());
        assert(x.tryBorrowMut().isOk());
    }
    assert(x.tryBorrow().isOk());
    assert(x.tryBorrowMut().isOk());
}

    @Test
fun refMapSplitUpdatesFlag() {
    val x = RefCell([1, 2]);
    {
        val b1 = x.borrow();
        assert(x.tryBorrow().isOk());
        assert(x.tryBorrowMut().isErr());
        {
            val (_b2, _b3) = Ref.mapSplit(b1, { slc -> slc.splitAt(1));
            assert(x.tryBorrow().isOk());
            assert(x.tryBorrowMut().isErr());
        }
        assert(x.tryBorrow().isOk());
        assert(x.tryBorrowMut().isOk());
    }
    assert(x.tryBorrow().isOk());
    assert(x.tryBorrowMut().isOk());

    {
        val b1 = x.borrowMut();
        assert(x.tryBorrow().isErr());
        assert(x.tryBorrowMut().isErr());
        {
            val (_b2, _b3) = RefMut.mapSplit(b1, { slc -> slc.splitAtMut(1));
            assert(x.tryBorrow().isErr());
            assert(x.tryBorrowMut().isErr());
            drop(_b2);
            assert(x.tryBorrow().isErr());
            assert(x.tryBorrowMut().isErr());
        }
        assert(x.tryBorrow().isOk());
        assert(x.tryBorrowMut().isOk());
    }
    assert(x.tryBorrow().isOk());
    assert(x.tryBorrowMut().isOk());
}

    @Test
fun refMapSplit() {
    val x = RefCell([1, 2]);
    val (b1, b2) = Ref.mapSplit(x.borrow(), { slc -> slc.splitAt(1));
    assert(*b1 == [1]);
    assert(*b2 == [2]);
}

    @Test
fun refMutMapSplit() {
    val x = RefCell([1, 2]);
    {
        val (mut b1, mut b2) = RefMut.mapSplit(x.borrowMut(), { slc -> slc.splitAtMut(1));
        assert(*b1 == [1]);
        assert(*b2 == [2]);
        b1[0] = 2;
        b2[0] = 1;
    }
    assert(*x.borrow() == [2, 1]);
}

    @Test
fun refMapAccessor() {
    class X(RefCell<(Int, Char)>);
    impl X {
        fun accessor(): Ref<Int> {
            Ref.map(this.first.borrow(), { tuple -> tuple.first)
        }
    }
    val x = X(RefCell((7, 'z')));
    val d: Ref<Int> = x.accessor();
    assert(*d == 7);
}

    @Test
fun refMutMapAccessor() {
    class X(RefCell<(Int, Char)>);
    impl X {
        fun accessor(): RefMut<Int> {
            RefMut.map(this.first.borrowMut(), { tuple -> tuple.first)
        }
    }
    val x = X(RefCell((7, 'z')));
    {
        var d: RefMut<Int> = x.accessor();
        assert(*d == 7);
        *d += 1;
    }
    assert(*x.first.borrow() == (8, 'z'));
}

    @Test
fun asPtr() {
    val c1: Cell<ULong> = Cell(0);
    c1.set(1);
    assert(1 == run { *c1.asPtr() });

    val c2: Cell<ULong> = Cell(0);
    run { *c2.asPtr() = 1; }
    assert(1 == c2.get());

    val r1: RefCell<ULong> = RefCell(0);
    *r1.borrowMut() = 1;
    assert(1 == run { *r1.asPtr() });

    val r2: RefCell<ULong> = RefCell(0);
    run { *r2.asPtr() = 1; }
    assert(1 == *r2.borrow());
}

    @Test
fun cellDefault() {
    val cell: Cell<Int> = Default.default();
    assert(0 == cell.get());
}

    @Test
fun cellSet() {
    val cell = Cell(10);
    cell.set(20);
    assert(20 == cell.get());

    val cell = Cell("Hello".toOwned());
    cell.set("World".toOwned());
    assert("World".toOwned() == cell.intoInner());
}

    @Test
fun cellReplace() {
    val cell = Cell(10);
    assert(10 == cell.replace(20));
    assert(20 == cell.get());

    val cell = Cell("Hello".toOwned());
    assert("Hello".toOwned() == cell.replace("World".toOwned()));
    assert("World".toOwned() == cell.intoInner());
}

    @Test
fun cellIntoInner() {
    val cell = Cell(10);
    assert(10 == cell.intoInner());

    val cell = Cell("Hello world".toOwned());
    assert("Hello world".toOwned() == cell.intoInner());
}

    @Test
fun refcellDefault() {
    val cell: RefCell<ULong> = Default.default();
    assert(0 == *cell.borrow());
}

    @Test
fun refcellUnsized() {
    val cell: RefCell<[Int]> = RefCell([1, 2, 3]);
    {
        val b = *cell.borrowMut();
        b[0] = 4;
        b[2] = 5;
    }
    val comp: [Int] = [4, 2, 5];
    assert(*cell.borrow() == comp);
}

    @Test
    // shouldPanic
fun refcellSwapBorrows() {
    val x = RefCell(0);
    val _b = x.borrow();
    val y = RefCell(1);
    x.swap(y);
}

    @Test
    // shouldPanic
fun refcellReplaceBorrows() {
    val x = RefCell(0);
    val _b = x.borrow();
    x.replace(1);
}


class RefStrings(RefCell<List<String>>);

    fun iter<'t, 's: 't>('s this): RefVal<'t, Iter<String>> {
        Ref.mapVal(this.first.borrow(), { x -> x.iterator())
    }


    @Test
fun refvalWithIterator() {
    val strings = vec![
        "one".toString(),
        "two".toString(),
        "three".toString(),
    ];

    val refStrings = RefStrings(RefCell(strings));
    var it = refStrings.iterator().clone().rev().skip(1);
    assert(it.next()!! == "two");
    assert(it.next()!! == "one");
    assert(it.next() == null);
}

    @Test
fun refvalClone() {
    val strings = vec![
        "four".toString(),
        "five".toString(),
        "six".toString(),
    ];

    val refStrings1 = RefStrings(RefCell(strings));
    var it1 = refStrings1.iterator();
    assert(it1.next()!! == "four");

    val refStrings2 = refStrings1.clone();
    var it2 = refStrings2.iterator();
    assert(it2.next()!! == "four");
}


class Val {
  val: ULong,
}

    @Test
fun refvalDebug() {
    val val = Val{val: 42};
    val refCell = RefCell(val);
    val refVal = Ref.mapVal(refCell.borrow(), { x -> x);

    assert(refVal.toDebugString() == "Val { val: 42 }")
}

    @Test
fun refvalMap() {
    val refStrings = RefCell(vec![
        "ten".toOwned(),
        "eleven".toOwned(),
        "twelve".toOwned(),
    ]);

    // `RefVal` containing an iterator over `refStrings` strings
    val it_1 = Ref.mapVal(RefCell.borrow(refStrings), { v -> v.iterator().map({ s -> s.asStr()));
    // Map `RefVal` to another `RefVal`: collect iterator into a vector.
    // Note that the new `Vec` contains references to original `String`
    // objects.
    val refs_1 = RefVal.map(it_1, { it -> it.collect.<List<String>>());
    drop(refs_1);

    // Assert refcount == 0
    refStrings.borrowMut();
}

    @Test
fun refvalmut() {
    val refStrings = RefCell(vec![
        "one".toOwned(),
        "two".toOwned(),
        "three".toOwned(),
    ]);

    // `RefValMut` containing a mutable iterator over `refStrings` strings
    var it_1 = RefMut.mapVal(RefCell.borrowMut(refStrings), { v -> v.iterMut());
    for s in it_1.byRef() {
        s.makeAsciiUppercase();
    }
    drop(it_1);

    val strings = refStrings.borrow();
    assertEq!(vec![
        "ONE".toOwned(),
        "TWO".toOwned(),
        "THREE".toOwned(),
    ], *strings);
}
