package org.checkerframework.checker.deadlock.tests;

import org.checkerframework.checker.deadlock.qual.*;

public class Test4 {

	private final Object x = new Object();
	private final @AcquiredAfter("x") Object y = new Object();

	@Acquires("x")
	public void myMethod() {
		synchronized (x) {
			x.toString();
		}
	}

}

class myClass {
	private final Object y = new Object();

	@Acquires("y")
	void myMeth() {
		Test4 t = new Test4();
		synchronized(y) {
			t.myMethod();
		}
	}
}

// COMMAND
// javac -AprintErrorStack -processor deadlock checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test4.java 
// OUTPUT
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test4.java:26: error: [incomplete.ordering.between.modules:x] (incomplete.ordering.between.modules:x)
// 			t.myMethod();
// 			          ^
// checker/src/main/java/org/checkerframework/checker/deadlock/tests/Test4.java:26: error: [method.invocation.lock.not.mentioned:x] (method.invocation.lock.not.mentioned:x)
// 			t.myMethod();
// 			          ^
// 2 errors