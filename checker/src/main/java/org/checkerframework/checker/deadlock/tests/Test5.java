package org.checkerframework.checker.deadlock.tests;

import org.checkerframework.checker.deadlock.qual.*;

public class Test5 {

	private final Object x = new Object();
	private final @AcquiredAfter("x") Object y = new Object();

	@Acquires("x")
	public void myMethod() {
		synchronized (x) {
			x.toString();
		}
	}

}

class myOtherClass {
	private final Object y = new Object();

	@Acquires({"x","y"})
	void myMeth() {
		Test5 t = new Test5();
		synchronized(y) {
			t.myMethod();
		}
	}
}

// COMMAND
// javac -AprintErrorStack -processor deadlock checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test5.java
// OUTPUT 
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test5.java:26: error: [incomplete.ordering.between.modules:x] (incomplete.ordering.between.modules:x)
// 			t.myMethod();
// 			          ^
// 1 error