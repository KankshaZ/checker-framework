package org.checkerframework.checker.deadlock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the specified expressions must be acquired during the execution of the annotated
 * method.
 *
 * <p>The argument is a string or set of strings that indicates the expression(s) that will be
 * acquired, using the <a
 * href="https://checkerframework.org/manual/#java-expressions-as-arguments">syntax of Java
 * expressions</a> described in the manual. The expressions evaluate to an intrinsic (built-in,
 * synchronization) monitor.
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Acquires {
    /**
     * The Java expressions that need to be held.
     *
     * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">Syntax of
     *     Java expressions</a>
     */
    String[] value();
}
