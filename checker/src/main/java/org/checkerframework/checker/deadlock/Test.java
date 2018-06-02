package org.checkerframework.checker.deadlock;

import org.checkerframework.checker.deadlock.qual.*;

public class Test {

	@Acquires("myLock")
	void helper1(Object a) {
        synchronized(MyClass.myLock) {
            a.toString();  // OK: the lock is held
        }
    }

    @Acquires({"myLock", "myLock2"})
    void myMethod2(Object e) {
    	synchronized(MyClass.myLock2){
        	helper1(e);
    	}
    }
}

class MyClass {
    public static @AcquiredAfterUnknown Object myLock = new Object();
    public static Object myLock2 = new Object();
}