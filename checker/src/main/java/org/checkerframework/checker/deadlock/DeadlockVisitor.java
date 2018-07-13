package org.checkerframework.checker.deadlock;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.deadlock.qual.Acquires;
import org.checkerframework.checker.deadlock.qual.AcquiredAfter;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import java.util.Arrays;
import org.checkerframework.checker.deadlock.DeadlockAnnotatedTypeFactory.*;

/**
 * The DeadlockVisitor enforces the special type-checking rules described in the Deadlock Checker
 * manual chapter.
 *
 * @checker_framework.manual #deadlock-checker Deadlock Checker
 */
public class DeadlockVisitor extends BaseTypeVisitor<DeadlockAnnotatedTypeFactory> {

    private static List<String> methodAcquiresLocks = null;
    private static List<String> methodAcquiredLocks = null;
    private static List<LockGroup> listOfLockGroups = null;
    // private static List<LockExpression> methodAcquiresLocksList = null;

    public DeadlockVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {

        TypeMirror tm = TreeUtils.typeOf(node);
        String lockToBeAdded = node.getName().toString();
        List<? extends AnnotationTree> annotationTreeList = node.getModifiers().getAnnotations();
        if(node.getModifiers().getAnnotations().isEmpty()) {
            listOfLockGroups = atypeFactory.defineAcquisitionOrder(lockToBeAdded);
        }
        for(AnnotationTree annotation: annotationTreeList) {
            if(annotation.getAnnotationType().toString().equals("AcquiredAfter")) {
                for(ExpressionTree argument: annotation.getArguments()) {
                    String arg = argument.toString();
                    if(arg.contains("value")) {
                        String value = arg.substring(arg.indexOf("=")+2, arg.length());
                        ArrayList<String> acquiredAfter = getValueArray(value);
                        listOfLockGroups = atypeFactory.defineAcquisitionOrder(acquiredAfter, lockToBeAdded);
                    }
                }
            }
        }
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        methodAcquiredLocks = null;

        ExecutableElement method = TreeUtils.elementFromDeclaration(node);
        // System.out.println("reached method: " + method);

        List<String> prevLocks = atypeFactory.getHeldLock();
        List<String> locks = prevLocks;
        // System.out.println("Previously held locks " + prevLocks);

        List<String> methodLocks = methodAcquires(method);
        // System.out.println("Method will acquire " + methodLocks);
        methodAcquiresLocks = methodLocks;

        try {
            if (method.getModifiers().contains(Modifier.SYNCHRONIZED)) {
                String appendLock = "";
                if (method.getModifiers().contains(Modifier.STATIC)) {
                    String enclosingClass = method.getEnclosingElement().getSimpleName().toString();
                    appendLock = enclosingClass + ".class";
                } else {
                    appendLock = "this";
                }
                if (!checkIfAcquiresContainsLock(appendLock)) {
                    checker.report(
                            Result.failure("method.acquires.not-mentioned.lock:" + appendLock),
                            node);
                }
                locks = append(locks, appendLock);
                atypeFactory.setHeldLocks(locks);
                // System.out.println("Synchronized method. Currently held locks " + locks);
            }

            return super.visitMethod(node, p);
        } finally {
            atypeFactory.setHeldLocks(prevLocks);

            // check if lock is not within method
            if (!methodAcquiresLocks.isEmpty()) {
                if (methodAcquiredLocks != null) {
                    if (!(methodAcquiredLocks.size() == methodAcquiresLocks.size())) {
                        checker.report(Result.failure("all.mentioned.locks.not.acquired"), node);
                    }
                } else {
                    checker.report(Result.failure("all.mentioned.locks.not.acquired"), node);
                }
            }
        }
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        ExecutableElement method = TreeUtils.elementFromUse(node);
        // System.out.println("METHOD INVOCATION:  " + method);

        List<String> prevLocks = atypeFactory.getHeldLock();
        List<String> locks = prevLocks;
        // System.out.println("Previously held locks " + prevLocks);

        if (method.getModifiers().contains(Modifier.SYNCHRONIZED)) {
            String appendLock = "";
            if (method.getModifiers().contains(Modifier.STATIC)) {
                String enclosingClass = method.getEnclosingElement().getSimpleName().toString();
                appendLock = enclosingClass + ".class";
            } else {
                appendLock = "this";
            }

            if (!checkIfAcquiresContainsLock(appendLock)) {
                checker.report(
                        Result.failure("method.invocation.lock.not.mentioned:" + appendLock), node);
            }
            // System.out.println("Synchronized method. Currently held locks " + locks);
        }

        List<String> methodLocks = methodAcquires(method); //locks that method call will acquire

        // check if any currently-held lock is not in append lock's predecessor
        if(listOfLockGroups!=null && !listOfLockGroups.isEmpty())
        {
            for(LockGroup lockGroup: listOfLockGroups) {
                for (String appendLock: methodLocks) {
                    if (lockGroup.locks.contains(appendLock)) {
                        if(!checkAncestors(prevLocks, lockGroup, appendLock)) {
                            checker.report( Result.failure(
                                    "incomplete.ordering.between.modules:"
                                            + appendLock), node);
                        }
                    }
                }
            }
        }

        if (!methodLocks.isEmpty()) {

            // check if locks that method call acquire are present in @Acquires
            for (String lock : methodLocks) {
                if (!checkIfAcquiresContainsLock(lock)) {
                    checker.report(
                            Result.failure("method.invocation.lock.not.mentioned:" + lock), node);
                }
            }

            // System.out.println("lock release occurs immediately after the method call.");
        }

        return super.visitMethodInvocation(node, p);
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {

        ExpressionTree synchronizedExpression = node.getExpression();
        // System.out.println("LOCK ACQUIRED BY SYNC_STATEMENT: " + synchronizedExpression);

        List<String> prevLocks = atypeFactory.getHeldLock();
        // System.out.println("Previously held locks " + prevLocks);
        try {

            String appendLock = TreeUtils.skipParens(node.getExpression()).toString();
            // check if lock is in @Acquires
            if (!checkIfAcquiresContainsLock(appendLock)) {
                checker.report(
                        Result.failure(
                                "synchronizedExpression in method acquires lock not mentioned: "
                                        + appendLock), node);
            }

            for(LockGroup lockGroup: listOfLockGroups) {
                if (lockGroup.locks.contains(appendLock)) {
                    if(checkAncestors(prevLocks, lockGroup, appendLock)) {
                        List<String> locks = append(prevLocks, appendLock);
                        atypeFactory.setHeldLocks(locks);
                        // System.out.println("Currently held locks " + locks);
                    }
                    else {
                        checker.report(
                            Result.failure(
                                    "incomplete.definition.or.inconsistent.with.defined.order"
                                            + appendLock), node);
                    }
                }
            }

            
            return super.visitSynchronized(node, p);
        } finally {
            atypeFactory.setHeldLocks(prevLocks);
        }
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {

        return super.visitAnnotation(tree, p);
    }

    // returns locks in @Acquires annotation
    protected List<String> methodAcquires(ExecutableElement element) {
        AnnotationMirror acquires = atypeFactory.getDeclAnnotation(element, Acquires.class);

        if (acquires == null) return Collections.emptyList();

        List<String> locks = new ArrayList<String>();

        if (acquires != null) {
            List<String> acquiresValue =
                    AnnotationUtils.getElementValueArray(acquires, "value", String.class, false);
            locks.addAll(acquiresValue);
        }

        return locks;
    }

    private static ArrayList<String> getValueArray(String value) {
        ArrayList<String> acquiredAfter = new ArrayList<String>();
        if (value.contains(",")) {
            value = value.replaceAll("\"", "");
            value = value.substring(1, value.length()-1);
            String[] list = value.split("\\s*,\\s*");
            // System.out.println(list);
            ArrayList<String> aList = new ArrayList<String>();
            for(String val: list) {
                aList.add(val);
            }
            return aList;
        } else {
            acquiredAfter.add(value.substring(1, value.length()-1));
            return acquiredAfter;
        }
    }

    private <T> List<T> append(List<T> lst, T o) {
        if (o == null) return lst;

        List<T> newList = new ArrayList<T>(lst.size() + 1);
        newList.addAll(lst);
        newList.add(o);
        return newList;
    }

    // checks if any currently-held lock is not in lockToBeAdded's predecessors
    public static Boolean checkAncestors(List<String> prevLocks, LockGroup lockGroup, String lockToBeAdded) {
        ArrayList<String> locks = lockGroup.locks;
        int [][] order = lockGroup.order;
        int index = locks.indexOf(lockToBeAdded);
        for (String lock: prevLocks) {
            if(locks.contains(lock)) {
                if(order[locks.indexOf(lock)][index]!=1) {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        return true;
    }    

    // checks if lock is present in @Acquires annotation
    // Adds to methodAcquiredLocks list if present. That list keeps track of acquired locks
    private Boolean checkIfAcquiresContainsLock(String lockExpression) {
        for (String lock : methodAcquiresLocks) {
            if (lock.equals(lockExpression)) {
                if (methodAcquiredLocks == null) {
                    methodAcquiredLocks = new ArrayList<String>();
                    methodAcquiredLocks.add(lock);
                } else {
                    if (!methodAcquiredLocks.contains(lock)) {
                        methodAcquiredLocks.add(lock);
                    }
                }
                return true;
            }
        }
        return false;
    }

}
