package org.checkerframework.checker.deadlock.tests;

import org.checkerframework.checker.deadlock.qual.*;

public class Test7 {

    public String myMethod() {
        Object c = new Object();
        return "hi";
    }
}

class myAClass {
    private final Object y = new Object();
    private final Object a = new Object();
    private final @AcquiredAfter("a") Object b = new Object();

    @Acquires({"y", "c", "d"})
    void myMeth() {
        Test7 t = new Test7();
        @AcquiredAfter("y")
        Object c = new Object();
        synchronized (y) {
            synchronized (c) {
                @AcquiredAfter({"y", "a", "b", "c"})
                String d = t.myMethod();
                synchronized (d) {
                }
            }
        }
    }
}
