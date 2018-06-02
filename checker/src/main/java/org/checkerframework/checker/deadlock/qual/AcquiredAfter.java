package org.checkerframework.checker.deadlock.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
/**
 * The run-time value of the integer is unknown.
 *
 * @checker_framework.manual #nonnegative-checker Non-Negative Checker
 */
@Target({ElementType.TYPE_USE})
@SubtypeOf({AcquiredAfterUnknown.class})
public @interface AcquiredAfter {
    /**
     * The set of Java expressions that the lock can be acquired after.
     *
     * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">Syntax of
     *     Java expressions</a>
     */
    String[] value();
}