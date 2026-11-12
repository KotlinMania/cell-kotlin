package io.github.kotlinmania.cell.refcell

// Derived from the formatting module root (fmt/mod.rs); no port-lint header
// because module-root headers trip stub detection.

// slice: fmt/mod.rs lines 13-79
//  Utilities for formatting and printing strings.

import std.fmt.Debug;
import std.fmt.Formatter;
import std.fmt.Result;
import std.ops.Deref;

import crate.cell.Ref;
import crate.cell.RefCell;
import crate.cell.RefMut;
import crate.cell.RefVal;
import crate.RefValMut;


    fun fmt(f: Formatter): Result {
        when this.tryBorrow() {
            Ok(borrow) -> {
                f.debugStruct("RefCell")
                    .field("value", borrow)
                    .finish()
            }
            Err(_) -> {
                // The RefCell is mutably borrowed so we can't look at its value
                // here. Show a placeholder instead.
                class BorrowedPlaceholder;

                impl Debug for BorrowedPlaceholder {
                    fun fmt(f: Formatter): Result {
                        f.writeStr("<borrowed>")
                    }
                }

                f.debugStruct("RefCell")
                    .field("value", BorrowedPlaceholder)
                    .finish()
            }
        }
    }

    fun fmt(f: Formatter): Result {
        Debug.fmt(**this, f)
    }

    fun fmt(f: Formatter): Result {
        Debug.fmt(*(this.deref()), f)
    }

    fun fmt(f: Formatter): Result {
        Debug.fmt(*(this.deref()), f)
    }

    fun fmt(f: Formatter): Result {
        Debug.fmt(*(this.deref()), f)
    }

// If you expected tests to be here, look instead at the run-pass/ifmt.rs test,
// it's a lot easier than creating all of the rt.Piece structures here.
