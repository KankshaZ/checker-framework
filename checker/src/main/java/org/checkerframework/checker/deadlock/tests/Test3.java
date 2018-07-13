package org.checkerframework.checker.deadlock.tests;

import org.checkerframework.checker.deadlock.qual.*;

public class Test3 {

    private final Object x = new Object();
    private final @AcquiredAfter("x") Object y = new Object();
    private final @AcquiredAfter("x") Object z = new Object();

    @Acquires({"x", "y", "z"})
    void myMethod() {
        synchronized (x) {
            synchronized (y) {
                synchronized (z) {
                }
            }
        }
    }
}

// COMMAND
// javac -AprintErrorStack -processor deadlock
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test3.java
// OUTPUT
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test3.java:15: error:
// [incomplete.definition.or.inconsistent.with.defined.orderz]
// (incomplete.definition.or.inconsistent.with.defined.orderz)
// 				synchronized (z) {
// 				^
// 1 error
