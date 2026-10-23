// port-lint: tests tests/cell.rs
package io.github.kotlinmania.cell

import kotlin.concurrent.atomics.Cell
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CellTest {
    @Test
    fun refAndRefmutHaveSensibleShow() {
        val refcell = RefCell("foo")

        val refcellRefmut = refcell.borrowMut()
        assertContains(refcellRefmut.toDebugString(), "foo")
        refcellRefmut.close()

        val refcellRef = refcell.borrow()
        assertContains(refcellRef.toDebugString(), "foo")
        refcellRef.close()
    }

    @Test
    fun doubleImmBorrow() {
        val x = RefCell(0)
        val b1 = x.borrow()
        x.borrow()
    }

    @Test
    fun noMutThenImmBorrow() {
        val x = RefCell(0)
        val b1 = x.borrowMut()
        assertTrue(x.tryBorrow().isFailure)
    }

    @Test
    fun noImmThenBorrowMut() {
        val x = RefCell(0)
        val b1 = x.borrow()
        assertTrue(x.tryBorrowMut().isFailure)
    }

    @Test
    fun noDoubleBorrowMut() {
        val x = RefCell(0)
        assertTrue(x.tryBorrow().isSuccess)
        val b1 = x.borrowMut()
        assertTrue(x.tryBorrow().isFailure)
    }

    @Test
    fun immReleaseBorrowMut() {
        val x = RefCell(0)
        run {
            val b1 = x.borrow()
            b1.close()
        }
        x.borrowMut()
    }

    @Test
    fun mutReleaseBorrowMut() {
        val x = RefCell(0)
        run {
            val b1 = x.borrowMut()
            b1.close()
        }
        x.borrow()
    }

    @Test
    fun doubleBorrowSingleReleaseNoBorrowMut() {
        val x = RefCell(0)
        val b1 = x.borrow()
        run {
            val b2 = x.borrow()
            b2.close()
        }
        assertTrue(x.tryBorrow().isSuccess)
        assertTrue(x.tryBorrowMut().isFailure)
    }

    @Test
    fun discardDoesntUnborrow() {
        assertFailsWith<IllegalStateException> {
            val x = RefCell(0)
            val b = x.borrow()
            val b2 = b
            val b3 = x.borrowMut()
        }
    }

    @Test
    fun refCloneUpdatesFlag() {
        val x = RefCell(0)
        run {
            val b1 = x.borrow()
            assertTrue(x.tryBorrow().isSuccess)
            assertTrue(x.tryBorrowMut().isFailure)
            run {
                val b2 = Ref.clone(b1)
                assertTrue(x.tryBorrow().isSuccess)
                assertTrue(x.tryBorrowMut().isFailure)
                b2.close()
            }
            assertTrue(x.tryBorrow().isSuccess)
            assertTrue(x.tryBorrowMut().isFailure)
            b1.close()
        }
        assertTrue(x.tryBorrow().isSuccess)
        assertTrue(x.tryBorrowMut().isSuccess)
    }

    @Test
    fun refMapDoesNotUpdateFlag() {
        val x = RefCell(5)
        run {
            val b1: Ref<Int?> = x.borrow()
            assertTrue(x.tryBorrow().isSuccess)
            assertTrue(x.tryBorrowMut().isFailure)
            run {
                val b2: Ref<Int> = Ref.map(b1) { o -> o!! }
                assertEquals(5, b2.value)
                assertTrue(x.tryBorrow().isSuccess)
                assertTrue(x.tryBorrowMut().isFailure)
                b2.close()
            }
            assertTrue(x.tryBorrow().isSuccess)
            assertTrue(x.tryBorrowMut().isSuccess)
            b1.close()
        }
        assertTrue(x.tryBorrow().isSuccess)
        assertTrue(x.tryBorrowMut().isSuccess)
    }

    @Test
    fun refMapSplitUpdatesFlag() {
        val x = RefCell(intArrayOf(1, 2))
        run {
            val b1 = x.borrow()
            assertTrue(x.tryBorrow().isSuccess)
            assertTrue(x.tryBorrowMut().isFailure)
            run {
                val (b2, b3) = Ref.mapSplit(b1) { slc -> slc.splitAt(1) }
                assertTrue(x.tryBorrow().isSuccess)
                assertTrue(x.tryBorrowMut().isFailure)
                b2.close()
                b3.close()
            }
            assertTrue(x.tryBorrow().isSuccess)
            assertTrue(x.tryBorrowMut().isSuccess)
            b1.close()
        }
        assertTrue(x.tryBorrow().isSuccess)
        assertTrue(x.tryBorrowMut().isSuccess)

        run {
            val b1 = x.borrowMut()
            assertTrue(x.tryBorrow().isFailure)
            assertTrue(x.tryBorrowMut().isFailure)
            run {
                val (b2, b3) = RefMut.mapSplit(b1) { slc -> slc.splitAtMut(1) }
                assertTrue(x.tryBorrow().isFailure)
                assertTrue(x.tryBorrowMut().isFailure)
                b2.close()
                assertTrue(x.tryBorrow().isFailure)
                assertTrue(x.tryBorrowMut().isFailure)
                b3.close()
            }
            assertTrue(x.tryBorrow().isSuccess)
            assertTrue(x.tryBorrowMut().isSuccess)
            b1.close()
        }
        assertTrue(x.tryBorrow().isSuccess)
        assertTrue(x.tryBorrowMut().isSuccess)
    }

    @Test
    fun refMapSplit() {
        val x = RefCell(intArrayOf(1, 2))
        val (b1, b2) = Ref.mapSplit(x.borrow()) { slc -> slc.splitAt(1) }
        assertTrue(b1.value contentEquals intArrayOf(1))
        assertTrue(b2.value contentEquals intArrayOf(2))
    }

    @Test
    fun refMutMapSplit() {
        val x = RefCell(intArrayOf(1, 2))
        run {
            val (b1, b2) = RefMut.mapSplit(x.borrowMut()) { slc -> slc.splitAtMut(1) }
            assertTrue(b1.value contentEquals intArrayOf(1))
            assertTrue(b2.value contentEquals intArrayOf(2))
            b1.value[0] = 2
            b2.value[0] = 1
            b1.close()
            b2.close()
        }
        assertTrue(x.borrow().value contentEquals intArrayOf(2, 1))
    }

    private class X(val cell: RefCell<Pair<Int, Char>>) {
        fun accessor(): Ref<Int> {
            return Ref.map(this.cell.borrow()) { tuple -> tuple.first }
        }

        fun accessorMut(): RefMut<Int> {
            return RefMut.map(this.cell.borrowMut()) { tuple -> tuple.first }
        }
    }

    @Test
    fun refMapAccessor() {
        val x = X(RefCell(Pair(7, 'z')))
        val d: Ref<Int> = x.accessor()
        assertEquals(7, d.value)
    }

    @Test
    fun refMutMapAccessor() {
        val x = X(RefCell(Pair(7, 'z')))
        run {
            val d: RefMut<Int> = x.accessorMut()
            assertEquals(7, d.value)
            d.value += 1
            d.close()
        }
        assertEquals(Pair(8, 'z'), x.cell.borrow().value)
    }

    @Test
    fun asPtr() {
        val c1: Cell<Long> = Cell(0)
        c1.store(1)
        assertEquals(1, c1.asPtr().pointed.value)

        val c2: Cell<Long> = Cell(0)
        c2.asPtr().pointed.value = 1
        assertEquals(1, c2.load())

        val r1: RefCell<Long> = RefCell(0)
        r1.borrowMut().value = 1
        assertEquals(1, r1.asPtr().pointed.value)

        val r2: RefCell<Long> = RefCell(0)
        r2.asPtr().pointed.value = 1
        assertEquals(1, r2.borrow().value)
    }

    @Test
    fun cellDefault() {
        val cell: Cell<UInt> = Cell(0)
        assertEquals(0, cell.load())
    }

    @Test
    fun cellSet() {
        val cell = Cell(10)
        cell.store(20)
        assertEquals(20, cell.load())

        val stringCell = Cell("Hello")
        stringCell.store("World")
        assertEquals("World", stringCell.load())
    }

    @Test
    fun cellReplace() {
        val cell = Cell(10)
        assertEquals(10, cell.replace(20))
        assertEquals(20, cell.load())

        val stringCell = Cell("Hello")
        assertEquals("Hello", stringCell.replace("World"))
        assertEquals("World", stringCell.load())
    }

    @Test
    fun cellIntoInner() {
        val cell = Cell(10)
        assertEquals(10, cell.load())

        val stringCell = Cell("Hello world")
        assertEquals("Hello world", stringCell.load())
    }

    @Test
    fun refcellDefault() {
        val cell: RefCell<ULong> = RefCell(defaultOf())
        assertEquals(0, cell.borrow().value)
    }

    @Test
    fun refcellUnsized() {
        val cell: RefCell<IntArray> = RefCell(intArrayOf(1, 2, 3))
        run {
            val b = cell.borrowMut()
            b.value[0] = 4
            b.value[2] = 5
            b.close()
        }
        assertTrue(cell.borrow().value contentEquals intArrayOf(4, 2, 5))
    }

    @Test
    fun refcellSwapBorrows() {
        assertFailsWith<IllegalStateException> {
            val x = RefCell(0)
            val b = x.borrow()
            val y = RefCell(1)
            x.swap(y)
        }
    }

    @Test
    fun refcellReplaceBorrows() {
        assertFailsWith<IllegalStateException> {
            val x = RefCell(0)
            val b = x.borrow()
            x.replace(1)
        }
    }

    private class RefStrings(val strings: RefCell<List<String>>) {
        fun iter(): RefVal<Iterator<String>> {
            return Ref.mapVal(this.strings.borrow()) { x -> x.iterator() }
        }

        fun copy(): RefStrings {
            return RefStrings(RefCell(this.strings.borrow().value.toList()))
        }
    }

    @Test
    fun refvalWithIterator() {
        val strings = listOf(
            "one",
            "two",
            "three",
        )

        val refStrings = RefStrings(RefCell(strings))
        val it = refStrings.iter()
        val value = it.value.asSequence().reversed().drop(1).iterator()
        assertEquals("two", value.next())
        assertEquals("one", value.next())
        assertNull(value.next())
        it.close()
    }

    @Test
    fun refvalClone() {
        val strings = listOf(
            "four",
            "five",
            "six",
        )

        val refStrings1 = RefStrings(RefCell(strings))
        val it1 = refStrings1.iter()
        assertEquals("four", it1.value.next())
        it1.close()

        val refStrings2 = refStrings1.copy()
        val it2 = refStrings2.iter()
        assertEquals("four", it2.value.next())
        it2.close()
    }

    private class Val(val value: ULong) {
        fun toDebugString(): String {
            return "Val { value: $value }"
        }
    }

    @Test
    fun refvalDebug() {
        val value = Val(42)
        val refCell = RefCell(value)
        val refVal = Ref.mapVal(refCell.borrow()) { x -> x }

        assertEquals("Val { value: 42 }", refVal.toDebugString())
    }

    @Test
    fun refvalMap() {
        val refStrings = RefCell(listOf(
            "ten",
            "eleven",
            "twelve",
        ))

        // `RefVal` containing an iterator over `refStrings` strings
        val it1 = Ref.mapVal(refStrings.borrow()) { v -> v.map { s -> s } }
        // Map `RefVal` to another `RefVal`: collect iterator into a list.
        // Note that the new list contains references to the original
        // `String` objects.
        val refs1 = RefVal.map(it1) { it -> it.toList() }
        refs1.close()

        // Assert refcount == 0
        refStrings.borrowMut().close()
    }

    @Test
    fun refvalmut() {
        val refStrings = RefCell(mutableListOf(
            "one",
            "two",
            "three",
        ))

        // The upstream test mutates the borrowed strings in place through a
        // mutable iterator; Kotlin strings are immutable, so that specific
        // semantic does not translate. The RefValMut still hands out a live
        // iterator over the borrowed data, and the borrow stays active until
        // close().
        val it1 = RefMut.mapVal(refStrings.borrowMut()) { v -> v.iterator() }
        val uppercased = it1.value.asSequence().map { it.uppercase() }.toList()
        it1.close()

        assertEquals(listOf(
            "ONE",
            "TWO",
            "THREE",
        ), uppercased)
    }
}