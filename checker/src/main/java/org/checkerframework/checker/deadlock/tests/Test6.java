package org.checkerframework.checker.deadlock.tests;

import org.checkerframework.checker.deadlock.qual.*;

public class Test6 {

    // to check if lock group merge is happening properly
    private final Object a = new Object();
    private final @AcquiredAfter("a") Object b = new Object();
    private final @AcquiredAfter({"a", "b", "c"}) Object d = new Object();
    private final Object x = new Object();
    private final @AcquiredAfter("x") Object y = new Object();
    private final Object r = new Object();
    private final @AcquiredAfter("r") Object s = new Object();
    private final Object p = new Object();
    private final @AcquiredAfter("p") Object q = new Object();
    private final @AcquiredAfter({"a", "b", "x", "y", "p", "q"}) Object c = new Object();

}