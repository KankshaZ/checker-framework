package org.checkerframework.checker.deadlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.deadlock.qual.AcquiredAfter;
import org.checkerframework.checker.deadlock.qual.AcquiredAfterUnknown;
import org.checkerframework.checker.deadlock.qual.Acquires;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;

public class DeadlockAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private List<String> heldLocks = new ArrayList<String>();
    private final AnnotationMirror ACQUIRES;
    private final AnnotationMirror ACQUIRED_AFTER;
    private final AnnotationMirror ACQUIRED_AFTER_UNKNOWN;

    public DeadlockAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        ACQUIRES = AnnotationBuilder.fromClass(elements, Acquires.class);
        ACQUIRED_AFTER = AnnotationBuilder.fromClass(elements, AcquiredAfter.class);
        ACQUIRED_AFTER_UNKNOWN = AnnotationBuilder.fromClass(elements, AcquiredAfterUnknown.class);

        postInit();
    }

    public void setHeldLocks(List<String> heldLocks) {
        this.heldLocks = heldLocks;
    }

    public List<String> getHeldLock() {
        return Collections.unmodifiableList(heldLocks);
    }
}
