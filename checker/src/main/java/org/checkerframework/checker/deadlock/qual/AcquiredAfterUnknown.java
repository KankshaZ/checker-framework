package org.checkerframework.checker.deadlock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/**
 * It is unknown what locks must be acquired before the value referred to be the
 * annotated variable.
 *
 *
 * <p><tt>@AcquiredAfterUnknown</tt> is the top of the AcquiredAfter qualifier hierarchy. Any value can be
 * assigned into a variable of type <tt>@AcquiredAfterUnknown</tt>.
 * 
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface AcquiredAfterUnknown {}