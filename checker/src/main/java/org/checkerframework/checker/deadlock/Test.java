package org.checkerframework.checker.deadlock;

import org.checkerframework.checker.deadlock.qual.*;

public class Test {

    @Acquires("myLock")
    void myMethod1(Object a) {
        synchronized (MyClass.myLock) {
            a.toString();
        }
    }

    @Acquires({"myLock", "myLock2"})
    void myMethod2(Object b) {
        synchronized (MyClass.myLock2) {
            myMethod1(b);
        }
    }
}

class MyClass {
    public static @AcquiredAfterUnknown Object myLock = new Object();
    public static Object myLock2 = new Object();
}

/*COMMAND: javacheck -processor deadlock Test.java
OUTPUT:
reached method: Test()
METHOD INVOCATION:  Object()
annotation on method: null
reached method: myMethod1(java.lang.Object)
ANNOTATION FOUND: [@org.checkerframework.checker.deadlock.qual.Acquires({"myLock"})]
VARIABLE DECLARATION: java.lang.Object
annotated type: @AcquiredAfterUnknown Object
LOCK ACQUIRED BY SYNC_STATEMENT: (MyClass.myLock)
METHOD INVOCATION:  toString()
annotation on method: null
reached method: myMethod2(java.lang.Object)
ANNOTATION FOUND: [@org.checkerframework.checker.deadlock.qual.Acquires({"myLock", "myLock2"})]
VARIABLE DECLARATION: java.lang.Object
annotated type: @AcquiredAfterUnknown Object
LOCK ACQUIRED BY SYNC_STATEMENT: (MyClass.myLock2)
METHOD INVOCATION:  myMethod1(java.lang.Object)
annotation on method: @org.checkerframework.checker.deadlock.qual.Acquires({"myLock"})
reached method: MyClass()
METHOD INVOCATION:  Object()
annotation on method: null
VARIABLE DECLARATION: (@org.checkerframework.checker.deadlock.qual.AcquiredAfterUnknown :: java.lang.Object)
annotated type: @AcquiredAfterUnknown Object
ANNOTATION FOUND: [@org.checkerframework.checker.deadlock.qual.AcquiredAfterUnknown]
VARIABLE DECLARATION: java.lang.Object
annotated type: @AcquiredAfterUnknown Object
*/
