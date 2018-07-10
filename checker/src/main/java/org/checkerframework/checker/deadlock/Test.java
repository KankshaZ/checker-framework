package org.checkerframework.checker.deadlock;

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
    public static @AcquiredAfterUnknown Object myLock = new Object();
    public static Object myLock2 = new Object();
    public static Object myLock3 = new Object();
}

class SynchronizedCounter {
    private int c = 0;

    @Acquires("this")
    public synchronized void increment() {
        c++;
    }
}

// OUTPUT:
// checker/src/main/java/org/checkerframework/checker/deadlock/Test.java:8: error: [all.mentioned.locks.not.acquired] (all.mentioned.locks.not.acquired)
//     void myMethod1(Object a) {
//          ^
// checker/src/main/java/org/checkerframework/checker/deadlock/Test.java:18: error: [method.invocation.lock.not.mentioned:MyClass.myLock] (method.invocation.lock.not.mentioned:MyClass.myLock)
//                 myMethod1(b);
//                          ^
// 2 errors
// Previously held locks []
// Method will acquire []
// Previously held locks []
// Previously held locks []
// Method will acquire [MyClass.myLock, MyClass.myLock2]
// Previously held locks []
// Currently held locks [MyClass.myLock]
// Previously held locks [MyClass.myLock]
// Previously held locks []
// Method will acquire [MyClass.myLock2, MyClass.myLock3]
// Previously held locks []
// Currently held locks [MyClass.myLock2]
// Previously held locks [MyClass.myLock2]
// Currently held locks [MyClass.myLock2, MyClass.myLock3]
// Previously held locks [MyClass.myLock2, MyClass.myLock3]
// Method call will acquire locks [MyClass.myLock, MyClass.myLock2]
// lock release occurs immediately after the method call.
// Previously held locks []
// Method will acquire [MyClass.myLock, MyClass.myLock2, MyClass.myLock3]
// Previously held locks []
// Currently held locks [MyClass.myLock2]
// Previously held locks [MyClass.myLock2]
// Currently held locks [MyClass.myLock2, MyClass.myLock3]
// Previously held locks [MyClass.myLock2, MyClass.myLock3]
// Method call will acquire locks [MyClass.myLock, MyClass.myLock2]
// lock release occurs immediately after the method call.
// Previously held locks [MyClass.myLock2, MyClass.myLock3]
// Previously held locks [MyClass.myLock2, MyClass.myLock3]
// Method call will acquire locks [MyClass.myLock, MyClass.myLock2]
// lock release occurs immediately after the method call.
// Previously held locks []
// Currently held locks [MyClass.myLock2]
// Previously held locks [MyClass.myLock2]
// Previously held locks []
// Method will acquire [this]
// Previously held locks []
// Synchronized method. Currently held locks []
// Method call will acquire locks [this]
// lock release occurs immediately after the method call.
// Previously held locks []
// Method will acquire []
// Previously held locks []
// Previously held locks []
// Method will acquire [this]
// Synchronized method. Currently held locks [this]
// Previously held locks []
// Method will acquire []
// Previously held locks []