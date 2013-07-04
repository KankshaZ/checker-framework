package checkers.nullness.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.nullness.AbstractNullnessChecker;
import checkers.quals.MonotonicQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that a field (or variable) is lazily initialized to a non-null
 * value.  Once the field becomes non-null, it never becomes null again.
 * There is no guarantee that the field ever becomes non-null, but if it
 * does, it will stay non-null.
 * <p>
 *
 * A monotonically non-null field has these two properties:
 * <ol>
 * <li>The field may be assigned only non-null values.</li>
 * <li>The field may be re-assigned as often as desired.</li>
 * </ol>
 * <p>
 *
 * When the field is first read within a method, the field cannot be
 * assumed to be non-null.  After a check that a {@code MonotonicNonNull} field
 * holds a non-null value, all subsequent accesses <em>within that
 * method</em> can be assumed to be non-null, even after arbitrary external
 * method calls that might access the field.
 * <p>
 * 
 * {@code MonotonicNonNull} gives stronger guarantees than {@link Nullable}.
 * After a check that a {@link Nullable} field holds a non-null value, only
 * accesses until the next non-{@link Pure} method is called can be assumed
 * to be non-null.
 * <p>
 *
 * To indicate that a {@code MonotonicNonNull} or {@code Nullable} field is
 * non-null whenever a particular method is called, use
 * {@link RequiresNonNull}.
 * <p>
 *
 * Final fields are treated as MonotonicNonNull by default.
 * <p>
 *
 * This annotation is associated with the {@link AbstractNullnessChecker}.
 *
 * @see Nullable
 * @see MonotonicQualifier
 * @see AbstractNullnessChecker
 */
@Documented
@TypeQualifier
@SubtypeOf(Nullable.class)
@Target(ElementType.TYPE_USE)
@MonotonicQualifier(NonNull.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonotonicNonNull {
}
