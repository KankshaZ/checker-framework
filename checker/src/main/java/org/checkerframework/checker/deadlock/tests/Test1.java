package org.checkerframework.checker.deadlock.tests;

import org.checkerframework.checker.deadlock.qual.*;

public class Test1 {
    private final @AcquiredAfter("y") Object x = new Object();
    private final @AcquiredAfter("x") Object y = new Object(); // CYCLIC
}

// COMMAND
// javac -AprintErrorStack -processor deadlock
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test2.java
// OUTPUT
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test2.java:13: error:
// [incomplete.definition.or.inconsistent.with.defined.orderx]
// (incomplete.definition.or.inconsistent.with.defined.orderx)
// 			synchronized (x) {
// 			^
// 1 error
