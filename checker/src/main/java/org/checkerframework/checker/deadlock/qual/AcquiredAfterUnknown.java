package org.checkerframework.checker.deadlock.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
/**
 * It is unknown what locks are allowed to be acquired before the
 * annotated variable.
 *
 *
 * <p><tt>@AcquiredAfterUnknown</tt> is the top of the AcquiredAfter qualifier hierarchy. Any value can be
 * assigned into a variable of type <tt>@AcquiredAfterUnknown</tt>.
 * 
 */
@Target({ElementType.TYPE_USE})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface AcquiredAfterUnknown {}