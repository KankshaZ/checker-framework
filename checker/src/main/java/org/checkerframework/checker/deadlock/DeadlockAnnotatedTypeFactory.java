package org.checkerframework.checker.deadlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.deadlock.qual.AcquiredAfter;
import org.checkerframework.checker.deadlock.qual.AcquiredAfterUnknown;
import org.checkerframework.checker.deadlock.qual.Acquires;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.framework.qual.Unqualified;

public class DeadlockAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private List<String> heldLocks = new ArrayList<String>();
    private List<LockGroup> lockGroups = new ArrayList<LockGroup>();
    private final AnnotationMirror ACQUIRES;
    private final AnnotationMirror ACQUIRED_AFTER;
    private final AnnotationMirror ACQUIRED_AFTER_UNKNOWN;
    private final AnnotationMirror UNQUALIFIED;

    public DeadlockAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        ACQUIRES = AnnotationBuilder.fromClass(elements, Acquires.class);
        ACQUIRED_AFTER = AnnotationBuilder.fromClass(elements, AcquiredAfter.class);
        ACQUIRED_AFTER_UNKNOWN = AnnotationBuilder.fromClass(elements, AcquiredAfterUnknown.class);
        UNQUALIFIED = AnnotationBuilder.fromClass(elements, Unqualified.class);
        postInit();
    }

    public void setHeldLocks(List<String> heldLocks) {
        this.heldLocks = heldLocks;
    }

    public List<String> getHeldLock() {
        return Collections.unmodifiableList(heldLocks);
    }

    public static void addRelationship(ArrayList<String> ancestors, String lockToBeAdded, LockGroup lockGroup) {
        ArrayList<String> nameList = lockGroup.locks;
        // System.out.println("newNameList" + nameList);
        int [][] order = lockGroup.order;
        // for(int i=0; i<nameList.size(); i++) {
        //     for(int j=0; j<nameList.size(); j++) {
        //         System.out.println(order[i][j]);
        //     }
        // }
        if (nameList.contains(lockToBeAdded)) {
            int index =  nameList.indexOf(lockToBeAdded);
            order[index][index] = 1; // this node just got created
            for(String lock: nameList) {
                if (ancestors.contains(lock)) {
                    order[ancestors.indexOf(lock)][index] = 1; // node at index can be reached by ancestor
                    if (order[index][ancestors.indexOf(lock)] == 1) {
                        System.out.println("CYCLE.");
                    }
                    else {
                        order[nameList.indexOf(lock)][index] = 0;
                    }
                }
            }
            lockGroup.order = order;
            // System.out.println("new order");
            // for(int i=0; i<nameList.size(); i++) {
            //     for(int j=0; j<nameList.size(); j++) {
            //         System.out.println(order[i][j]);
            //     }
            // }
        }
        else {
            int [][] temp = new int[order.length+1][order.length+1];

            for(int i=0; i<nameList.size(); i++) {
                for(int j=0; j<nameList.size(); j++) {
                    // System.out.println(order[i][j]);
                    temp[i][j] = order [i][j];
                }
            }
            nameList.add(lockToBeAdded);
            // order = Arrays.copyOf(order, order.length + 1);
            // int [][] temp = new int[order.length+1][order.length+1];

            // for(int i=0; i<nameList.size(); i++) {
            //     for(int j=0; j<nameList.size(); j++) {
            //         // System.out.println(order[i][j]);
            //         temp[i][j] = order [i][j];
            //     }
            // }
            int index =  nameList.indexOf(lockToBeAdded);
            for(String lock: nameList) {
                // System.out.println("index " + index);
                // System.out.println("nameList.indexOf(lock) " + nameList.indexOf(lock));
                temp[index][nameList.indexOf(lock)] = 0; // since I cannot reach previous locks from the lock I am currently adding
                temp[nameList.indexOf(lock)][index] = 0; // initialisation
            }
            temp[index][index] = 1; // this node just got created
            for (String ancestor: ancestors) {
                temp[ancestors.indexOf(ancestor)][index] = 1; // node at index can be reached by ancestor
            }
            lockGroup.order = temp;
            // System.out.println("temp");
            // for(int i=0; i<nameList.size(); i++) {
            //     for(int j=0; j<nameList.size(); j++) {
            //         System.out.println(temp[i][j]);
            //     }
            // }
            // System.out.println("new order");
            // for(int i=0; i<nameList.size(); i++) {
            //     for(int j=0; j<nameList.size(); j++) {
            //         System.out.println(lockGroup.order[i][j]);
            //     }
            // }
            // PENDING: Merge if the lock has ancestors in multiple lock groups
        }
    }

    // Even if one ancestor is common, it belongs to the same group
    // Currently assumed that each lock can only belong to one lock groups
    public Boolean containsAncestors(ArrayList<String> ancestors, ArrayList<String> nameList) {
        for(String ancestor : ancestors) {
            if (nameList.contains(ancestor))
                return true;
        }
        return false;
    }

    public List<LockGroup> defineAcquisitionOrder(String variable) {
            LockGroup lockGroup = new LockGroup(variable);
            lockGroups.add(lockGroup);
            return lockGroups;
    }

    public List<LockGroup> defineAcquisitionOrder(ArrayList<String> ancestors, String variable) {
        if (lockGroups.isEmpty()) {
            LockGroup lockGroup = new LockGroup(ancestors, variable);
            lockGroups.add(lockGroup);
            return lockGroups;
        }
        for(LockGroup lockGroup: lockGroups) {
            ArrayList<String> nameList = lockGroup.locks;
            // System.out.println("nameList" + nameList);
            if(containsAncestors(ancestors, nameList)) {
                addRelationship(ancestors, variable, lockGroup);
                return lockGroups;
            }
        }
        LockGroup lockGroup = new LockGroup(ancestors, variable);
        lockGroups.add(lockGroup);
        return lockGroups;
    }

    public static class LockGroup {
        ArrayList<String> locks;
        int [][] order;

        LockGroup(String lockToBeAdded) {
            // System.out.println("in");
            this.locks = new ArrayList<String>();
            locks.add(lockToBeAdded);
            this.order = new int[1][1];
            order[0][0] = 1;
            // System.out.println("nameList in constructor" + locks);
            // System.out.println(order[0][0]);
        }

        LockGroup(ArrayList<String> ancestors, String lockToBeAdded) {
            this.locks = new ArrayList<String>();
            locks.addAll(ancestors);
            locks.add(lockToBeAdded);
            int index =  locks.indexOf(lockToBeAdded);
            int numberOfAncestors = ancestors.size();
            this.order = new int[numberOfAncestors+1][numberOfAncestors+1];
            for(int i=0; i<numberOfAncestors+1; i++) {
                for(int j=0; j<numberOfAncestors+1; j++) {
                    order[i][j] = 0;
                }
            }
            order[index][index] = 1; // this implies that the node has been created
            for (String ancestor : ancestors)
                order[ancestors.indexOf(ancestor)][index] = 1; // node at index can be reached by ancestor
            // System.out.println("nameList in constructor" + locks);
            // for(int i=0; i<numberOfAncestors+1; i++) {
            //     for(int j=0; j<numberOfAncestors+1; j++) {
            //         System.out.println(order[i][j]);
            //     }
            // }
        }

    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory ignorefactory) {
        MultiGraphQualifierHierarchy.MultiGraphFactory factory = createQualifierHierarchyFactory();

        factory.addQualifier(ACQUIRED_AFTER);
        factory.addQualifier(ACQUIRED_AFTER_UNKNOWN);
        factory.addQualifier(UNQUALIFIED);
        factory.addSubtype(UNQUALIFIED, ACQUIRED_AFTER);
        factory.addSubtype(ACQUIRED_AFTER, ACQUIRED_AFTER_UNKNOWN);

        return new DeadlockQualifierHierarchy(factory);
    }

    private final class DeadlockQualifierHierarchy extends GraphQualifierHierarchy {

        public DeadlockQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory, UNQUALIFIED);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            
            if (AnnotationUtils.areSameIgnoringValues(rhs, UNQUALIFIED)
                    && AnnotationUtils.areSameIgnoringValues(lhs, ACQUIRED_AFTER)) {
                return true;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, ACQUIRED_AFTER_UNKNOWN)
                    && AnnotationUtils.areSameIgnoringValues(lhs, ACQUIRED_AFTER)) {
                return true;
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, ACQUIRED_AFTER)) {
                lhs = ACQUIRED_AFTER;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, ACQUIRED_AFTER)) {
                rhs = ACQUIRED_AFTER;
            }
            return super.isSubtype(rhs, lhs);
        }
    }
}
