package org.checkerframework.checker.deadlock.tests;

import org.checkerframework.checker.deadlock.qual.*;

public class Test {

    @Acquires({"MyClass.myLock", "MyClass.myLock2"})
    void myMethod1(Object a) {
        synchronized (MyClass.myLock) {
            a.toString();
        }
    }

    @Acquires({"MyClass.myLock2", "MyClass.myLock3"})
    void myMethod2(Object b) {
        synchronized (MyClass.myLock2) {
            synchronized (MyClass.myLock3) {
                myMethod1(b);
            }
        }
    }

    @Acquires({"MyClass.myLock", "MyClass.myLock2", "MyClass.myLock3"})
    void myMethod3(Object b) {
        synchronized (MyClass.myLock2) {
            synchronized (MyClass.myLock3) {
                myMethod1(b);
                b.toString();
                myMethod1(b);
            }
        }
        synchronized (MyClass.myLock2) {
            b.toString();
        }
    }

    @Acquires("this")
    void myMethod3(SynchronizedCounter sc) {
        sc.increment();
    }
}

class MyClass {
    public static @AcquiredAfter({"MyClass.myLock2", "MyClass.myLock3"}) Object myLock =
            new Object();
    public static Object myLock2 = new Object();
    public static @AcquiredAfter("MyClass.myLock2") Object myLock3 = new Object();
}

class SynchronizedCounter {
    private int c = 0;

    @Acquires("this")
    public synchronized void increment() {
        c++;
    }
}

// COMMAND
// javac -AprintErrorStack -processor deadlock
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test.java
// OUTPUT
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test.java:8: error:
// [all.mentioned.locks.not.acquired] (all.mentioned.locks.not.acquired)
//     void myMethod1(Object a) {
//          ^
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test.java:18: error:
// [method.invocation.lock.not.mentioned:MyClass.myLock]
// (method.invocation.lock.not.mentioned:MyClass.myLock)
//                 myMethod1(b);
//                          ^
// 2 errors
