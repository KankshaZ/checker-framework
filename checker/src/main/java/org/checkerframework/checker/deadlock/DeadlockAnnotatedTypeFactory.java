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
import org.checkerframework.framework.qual.Unqualified;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

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

    // public void setLockGroups(List<LockGroup> lockGroups) {
    //     this.lockGroups = lockGroups;
    // }

    // public List<LockGroup> getLockGroups() {
    //     return lockGroups;
    // }

    public static void addRelationship(
            ArrayList<String> ancestors, String lockToBeAdded, LockGroup lockGroup) {
        ArrayList<String> nameList = lockGroup.locks;
        int[][] order = lockGroup.order;
        // lock already present in lock group
        if (nameList.contains(lockToBeAdded)) {
            int index = nameList.indexOf(lockToBeAdded);
            order[index][index] = 1; // this node just got created
            for (String lock : nameList) {
                if (ancestors.contains(lock)) {
                    order[ancestors.indexOf(lock)][index] =
                            1; // node at index can be reached by ancestor
                    if (order[index][ancestors.indexOf(lock)] == 1) {
                        System.out.println("CYCLE.");
                    } else {
                        order[nameList.indexOf(lock)][index] = 0;
                    }
                }
            }
            lockGroup.order = order;
        }
        // adding specifically only the lock to be added assuming all locks
        // in lockToBeAdded's nameList are present in the lock group
        // must handle case of locks not being present and adding multiple locks ke liye lines
        else {
            int[][] temp = new int[order.length + 1][order.length + 1];

            for (int i = 0; i < nameList.size(); i++) {
                for (int j = 0; j < nameList.size(); j++) {
                    temp[i][j] = order[i][j];
                }
            }
            nameList.add(lockToBeAdded);
            int index = nameList.indexOf(lockToBeAdded);
            for (String lock : nameList) {
                temp[index][nameList.indexOf(lock)] =
                        0; // since I cannot reach previous locks from the lock I am currently
                // adding
                temp[nameList.indexOf(lock)][index] = 0; // initialisation
            }
            temp[index][index] = 1; // this node just got created
            for (String ancestor : ancestors) {
                temp[ancestors.indexOf(ancestor)][index] =
                        1; // node at index can be reached by ancestor
            }
            lockGroup.order = temp;
        }
    }

    public static void addNewAncestors(
            ArrayList<String> ancestors,
            String lockToBeAdded,
            LockGroup lockGroup,
            ArrayList<String> incompleteListOfLocks) {
        ArrayList<String> nameList = lockGroup.locks;
        int[][] order = lockGroup.order;

        // System.out.println("orderInNewAnc  ");
        // for (int k = 0; k < order.length; k++) {
        //     for (int j = 0; j < order.length; j++) {
        //         System.out.print(order[k][j]);
        //     }
        //     System.out.println();
        // }

        ArrayList<String> addedLocks = new ArrayList<String>();
        int count = 0;
        for (String ancestor : ancestors) {
            if (!incompleteListOfLocks.contains(ancestor)) {
                count++;
                nameList.add(ancestor);
                addedLocks.add(ancestor);
            }
        }

        int[][] temp = new int[order.length + count][order.length + count];

        for (int i = 0; i < order.length; i++) {
            for (int j = 0; j < order.length; j++) {
                temp[i][j] = order[i][j];
            }
        }

        for (int i = 0; i < temp.length; i++) {
            for (String lock : addedLocks) {
                temp[i][nameList.indexOf(lock)] =
                        0; // since I cannot reach previous locks from the lock I am currently
                // adding
                temp[nameList.indexOf(lock)][i] = 0; // initialisation
            }
        }
        lockGroup.order = temp;

        // System.out.println("tempInNewAnc  ");
        // for (int k = 0; k < temp.length; k++) {
        //     for (int j = 0; j < temp.length; j++) {
        //         System.out.print(temp[k][j]);
        //     }
        //     System.out.println();
        // }
    }

    // remember that two lock groups will never contain the lockToBeAdded
    // check for cycle is left
    public void merge(
            int indexOfLockGroup,
            LockGroup nextLockGroup,
            String lockToBeAdded,
            ArrayList<String> incompleteListOfLocks,
            ArrayList<String> ancestors) {

        LockGroup lockGroup = lockGroups.get(indexOfLockGroup);
        ArrayList<String> nameList = lockGroup.locks;
        int[][] order = lockGroup.order;
        ArrayList<String> nameList2 = nextLockGroup.locks;
        int[][] order2 = nextLockGroup.order;
        ArrayList<String> newNameList = nameList;
        for (String name : nameList2) {
            newNameList.add(name);
        }

        // System.out.println("order1  ");
        // for (int k = 0; k < order.length; k++) {
        //     for (int j = 0; j < order.length; j++) {
        //         System.out.print(order[k][j]);
        //     }
        //     System.out.println();
        // }

        // System.out.println("order2  ");
        // for (int k = 0; k < order2.length; k++) {
        //     for (int j = 0; j < order2.length; j++) {
        //         System.out.print(order2[k][j]);
        //     }
        //     System.out.println();
        // }

        int index = -1;
        int[][] temp = new int[order.length + order2.length][order.length + order2.length];
        for (int i = 0; i < order.length; i++) {
            for (int j = 0; j < order.length; j++) {
                temp[i][j] = order[i][j];
            }
            for (int j = order.length; j < order.length + order2.length; j++) {
                temp[i][j] = 0;
            }
        }

        for (int i = order.length; i < order.length + order2.length; i++) {
            for (int j = 0; j < order.length; j++) {
                temp[i][j] = 0;
            }
            for (int j = order.length; j < order.length + order2.length; j++) {
                temp[i][j] = order2[i - order.length][j - order.length];
            }
        }
        if (nameList.contains(lockToBeAdded)) {
            index = nameList.indexOf(lockToBeAdded);
        } else if (nameList2.contains(lockToBeAdded)) {
            index = nameList2.indexOf(lockToBeAdded);
        }

        if (index != -1) {
            for (String lock : incompleteListOfLocks) {
                temp[newNameList.indexOf(lock)][index] =
                        1; // node at index can be reached by ancestor
                if (temp[index][newNameList.indexOf(lock)] == 1) {
                    System.out.println("CYCLE.");
                }
            }
            temp[index][index] = 1;
        }

        // System.out.println("temp  ");
        // for (int k = 0; k < temp.length; k++) {
        //     for (int j = 0; j < temp.length; j++) {
        //         System.out.print(temp[k][j]);
        //     }
        //     System.out.println();
        // }

        lockGroups.get(indexOfLockGroup).locks = newNameList;
        lockGroups.get(indexOfLockGroup).order = temp;

        if (incompleteListOfLocks.size() == ancestors.size()) {
            if (index == -1) {
                LockGroup newLockGroup = lockGroups.get(indexOfLockGroup);
                addRelationship(ancestors, lockToBeAdded, newLockGroup);
            }
        }
    }

    // Even if one ancestor is common, it belongs to the same group
    // Currently assumed that each lock can only belong to one lock groups
    public Boolean containsAncestors(ArrayList<String> ancestors, ArrayList<String> nameList) {
        for (String ancestor : ancestors) {
            if (nameList.contains(ancestor)) return true;
        }
        return false;
    }

    public Boolean containsAllAncestors(ArrayList<String> ancestors, ArrayList<String> nameList) {
        for (String ancestor : ancestors) {
            if (!nameList.contains(ancestor)) return false;
        }
        return true;
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
        int indexOfLockGroup = -1;
        ArrayList<String> incompleteListOfLocks = new ArrayList<String>();
        for (LockGroup lockGroup : lockGroups) {
            ArrayList<String> nameList = lockGroup.locks;
            // System.out.println("nameList " + nameList);
            if (containsAncestors(ancestors, nameList)) {
                // contains all ancestors implies belongs to single lock group
                if (containsAllAncestors(ancestors, nameList)) {
                    addRelationship(ancestors, variable, lockGroup);
                    return lockGroups;
                }
                // if I reached here it can mean two things
                // 1. variable has ancestors present in multiple lock groups
                // 2. a few of its ancestors have not been created yet

                // Let's check number 1. first
                indexOfLockGroup = lockGroups.indexOf(lockGroup);
                for (String ancestor : ancestors) {
                    if (nameList.contains(ancestor)) incompleteListOfLocks.add(ancestor);
                }
                List<LockGroup> lockGroupsCopy = new ArrayList<LockGroup>(lockGroups);
                int removed = 0;
                for (int i = indexOfLockGroup + 1; i < lockGroupsCopy.size(); i++) {
                    LockGroup nextLockGroup = lockGroupsCopy.get(i);
                    ArrayList<String> nameListNext = nextLockGroup.locks;
                    // System.out.println("nameListNext " + nameListNext);
                    // System.out.println("incompleteListOfLocks " + incompleteListOfLocks);
                    // System.out.println("ancestors " + ancestors);

                    if (containsAncestors(ancestors, nameListNext)) {
                        for (String ancestor : ancestors) {
                            if (nameListNext.contains(ancestor))
                                incompleteListOfLocks.add(ancestor);
                        }
                        // System.out.println("nameListNext " + nameListNext);
                        // System.out.println("incompleteListOfLocks " + incompleteListOfLocks);
                        // System.out.println("ancestors " + ancestors);
                        merge(
                                indexOfLockGroup,
                                nextLockGroup,
                                variable,
                                incompleteListOfLocks,
                                ancestors);
                        // System.out.println("i (in copy index)" + i);
                        // System.out.println("indexOfLockGroup " + indexOfLockGroup);
                        // System.out.println("i-removed " + (i-removed));
                        lockGroups.remove(i - removed);
                        removed++;
                        if (incompleteListOfLocks.size() == ancestors.size()) {
                            // System.out.println("FInal  " + lockGroup.locks);
                            // System.out.println("order  ");
                            // for (int k = 0; k < lockGroup.order.length; k++) {
                            //     for (int j = 0; j < lockGroup.order.length; j++) {
                            //         System.out.print(lockGroup.order[k][j]);
                            //     }
                            //     System.out.println();
                            // }
                            // System.out.println(lockGroups.size() + " size");
                            return lockGroups;
                        }
                    }
                }

                // if I got out of this loop it means
                // number 2: a few of its ancestors have not been created yet
                // so add the locks and un-created ancestors
                // return lockGroups;
                addNewAncestors(ancestors, variable, lockGroup, incompleteListOfLocks);
                addRelationship(ancestors, variable, lockGroup);
                // System.out.println("FInal2  " + lockGroup.locks);
                // System.out.println("order  ");
                // for (int i = 0; i < lockGroup.order.length; i++) {
                //     for (int j = 0; j < lockGroup.order.length; j++) {
                //         System.out.print(lockGroup.order[i][j]);
                //     }
                //     System.out.println();
                // }
                return lockGroups;
            }
        }
        LockGroup lockGroup = new LockGroup(ancestors, variable);
        lockGroups.add(lockGroup);
        return lockGroups;
    }

    public static class LockGroup {
        ArrayList<String> locks;
        int[][] order;

        LockGroup(String lockToBeAdded) {
            this.locks = new ArrayList<String>();
            locks.add(lockToBeAdded);
            this.order = new int[1][1];
            order[0][0] = 1;
        }

        LockGroup(ArrayList<String> ancestors, String lockToBeAdded) {
            this.locks = new ArrayList<String>();
            locks.addAll(ancestors);
            locks.add(lockToBeAdded);
            int index = locks.indexOf(lockToBeAdded);
            int numberOfAncestors = ancestors.size();
            this.order = new int[numberOfAncestors + 1][numberOfAncestors + 1];
            for (int i = 0; i < numberOfAncestors + 1; i++) {
                for (int j = 0; j < numberOfAncestors + 1; j++) {
                    order[i][j] = 0;
                }
            }
            order[index][index] = 1; // this implies that the node has been created
            for (String ancestor : ancestors)
                order[ancestors.indexOf(ancestor)][index] =
                        1; // node at index can be reached by ancestor
        }
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(
            MultiGraphQualifierHierarchy.MultiGraphFactory ignorefactory) {
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
