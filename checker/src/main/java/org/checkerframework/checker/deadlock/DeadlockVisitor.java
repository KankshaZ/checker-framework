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
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.deadlock.qual.Acquires;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The DeadlockVisitor enforces the special type-checking rules described in the Deadlock Checker
 * manual chapter.
 *
 * @checker_framework.manual #deadlock-checker Deadlock Checker
 */
public class DeadlockVisitor extends BaseTypeVisitor<DeadlockAnnotatedTypeFactory> {

    private static List<String> methodAcquiresLocks = null;
    private static List<String> methodAcquiredLocks = null;
    // private static List<LockExpression> methodAcquiresLocksList = null;

    public DeadlockVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {

        TypeMirror tm = TreeUtils.typeOf(node);
        // System.out.println("VARIABLE DECLARATION: " + tm);
        AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
        // System.out.println("annotated type: " + atm);

        return super.visitVariable(node, p);
    }

    private <T> List<T> append(List<T> lst, T o) {
        if (o == null) return lst;

        List<T> newList = new ArrayList<T>(lst.size() + 1);
        newList.addAll(lst);
        newList.add(o);
        return newList;
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        methodAcquiredLocks = null;

        ExecutableElement method = TreeUtils.elementFromDeclaration(node);
        // System.out.println("reached method: " + method);

        List<String> prevLocks = atypeFactory.getHeldLock();
        List<String> locks = prevLocks;
        System.out.println("Previously held locks " + prevLocks);

        List<String> methodLocks = methodAcquires(method);
        System.out.println("Method will acquire " + methodLocks);
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
                System.out.println("Synchronized method. Currently held locks " + locks);
            }

            // AnnotationMirror acquires = atypeFactory.getDeclAnnotation(method, Acquires.class);
            // if(acquires!=null) {
            //     List<LockExpression> expressions = getLockExpressions(false, acquires, node);
            //     methodAcquiresLocksList = expressions;
            //     System.out.println("Method will acquire " + expressions);
            // }

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
        System.out.println("Previously held locks " + prevLocks);

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
            System.out.println("Synchronized method. Currently held locks " + locks);
        }

        List<String> methodLocks = methodAcquires(method);
        if (!methodLocks.isEmpty()) {
            System.out.println("Method call will acquire locks " + methodLocks);

            // check if lock is in @Acquires
            for (String lock : methodLocks) {
                if (!checkIfAcquiresContainsLock(lock)) {
                    checker.report(
                            Result.failure("method.invocation.lock.not.mentioned:" + lock), node);
                }
            }

            System.out.println("lock release occurs immediately after the method call.");
        }

        return super.visitMethodInvocation(node, p);
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {

        ExpressionTree synchronizedExpression = node.getExpression();
        // System.out.println("LOCK ACQUIRED BY SYNC_STATEMENT: " + synchronizedExpression);

        List<String> prevLocks = atypeFactory.getHeldLock();
        System.out.println("Previously held locks " + prevLocks);
        try {

            // String lockInHere = TreeUtils.skipParens(node.getExpression()).toString();
            // TreePath currentPath = getCurrentPath();
            // List<Receiver> params =
            //         FlowExpressions.getParametersOfEnclosingMethod(atypeFactory, currentPath);
            // TypeMirror enclosingType = TreeUtils.typeOf(TreeUtils.enclosingClass(currentPath));
            // Receiver pseudoReceiver =
            //         FlowExpressions.internalReprOfPseudoReceiver(currentPath, enclosingType);
            // FlowExpressionContext exprContext =
            //         new FlowExpressionContext(pseudoReceiver, params, atypeFactory.getContext());
            // LockExpression lockExpression = parseExpressionString(lockInHere, exprContext,
            // currentPath);
            // if(!checkIfListContainsLock(lockExpression)) {
            //     ErrorReporter.errorAbort("Method is acquiring unmentioned lock: " +
            // lockExpression);
            // }

            // check if lock is in @Acquires
            if (!checkIfAcquiresContainsLock(
                    TreeUtils.skipParens(node.getExpression()).toString())) {
                checker.report(
                        Result.failure(
                                "synchronizedExpression in method acquires lock not mentioned: "
                                        + TreeUtils.skipParens(node.getExpression()).toString()),
                        node);
            }
            List<String> locks =
                    append(prevLocks, TreeUtils.skipParens(node.getExpression()).toString());

            atypeFactory.setHeldLocks(locks);
            System.out.println("Currently held locks " + locks);
            return super.visitSynchronized(node, p);
        } finally {
            atypeFactory.setHeldLocks(prevLocks);
        }
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {

        // ArrayList<AnnotationTree> annotationTreeList = new ArrayList<>(1);
        // annotationTreeList.add(tree);
        //  List<AnnotationMirror> amList =
        // TreeUtils.annotationsFromTypeAnnotationTrees(annotationTreeList);
        // System.out.println("ANNOTATION FOUND: " + amList);

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

    // private Boolean checkIfListContainsLock(LockExpression lockExpression) {
    //     for(LockExpression lock : methodAcquiresLocksList) {
    //         if (lock.expressionString.equals(lockExpression.expressionString)) {
    //             return true;
    //         }
    //     }
    //     return false;
    // }

    // private void checkLock(Tree tree, AnnotationMirror acquiresAnno) {
    //     if (acquiresAnno == null) {
    //         ErrorReporter.errorAbort("DeadlockLockVisitor.checkLock: Anno cannot be null");
    //     }

    //     List<LockExpression> expressions = getLockExpressions(acquiresAnno, tree);
    //     if (expressions.isEmpty()) {
    //         return;
    //     }

    //     for (LockExpression expression : expressions) {
    //         if (expression.error != null) {
    //             checker.report(
    //                     Result.failure(
    //                             "expression.unparsable.type.invalid",
    // expression.error.toString()),
    //                     tree);
    //         } else if (expression.lockExpression == null) {
    //             checker.report(
    //                     Result.failure(
    //                             "expression.unparsable.type.invalid",
    // expression.expressionString),
    //                     tree);
    //         }
    //     }
    // }

    // private List<LockExpression> getLockExpressions(AnnotationMirror acquiresAnno, Tree tree) {

    //     List<String> expressions =
    //             AnnotationUtils.getElementValueArray(acquiresAnno, "value", String.class, true);

    //     if (expressions.isEmpty()) {
    //         return Collections.emptyList();
    //     }

    //     TreePath currentPath = getCurrentPath();
    //     List<Receiver> params =
    //             FlowExpressions.getParametersOfEnclosingMethod(atypeFactory, currentPath);

    //     TypeMirror enclosingType = TreeUtils.typeOf(TreeUtils.enclosingClass(currentPath));
    //     Receiver pseudoReceiver =
    //             FlowExpressions.internalReprOfPseudoReceiver(currentPath, enclosingType);
    //     FlowExpressionContext exprContext =
    //             new FlowExpressionContext(pseudoReceiver, params, atypeFactory.getContext());

    //     List<LockExpression> lockExpressions = new ArrayList<>();
    //     for (String expression : expressions) {
    //         lockExpressions.add(parseExpressionString(expression, exprContext, currentPath));
    //     }
    //     return lockExpressions;
    // }

    // private LockExpression parseExpressionString(
    //         String expression,
    //         FlowExpressionContext flowExprContext,
    //         TreePath path) {

    //     LockExpression lockExpression = new LockExpression(expression);
    //     if (DependentTypesError.isExpressionError(expression)) {
    //         lockExpression.error = new DependentTypesError(expression);
    //         return lockExpression;
    //     }

    //     try {
    //             lockExpression.lockExpression =
    //                     FlowExpressionParseUtil.parse(expression, flowExprContext, path, true);
    //             return lockExpression;
    //     } catch (FlowExpressionParseException ex) {
    //         lockExpression.error = new DependentTypesError(expression, ex);
    //         return lockExpression;
    //     }
    // }

    // private static class LockExpression {
    //     final String expressionString;
    //     Receiver lockExpression = null;
    //     DependentTypesError error = null;

    //     LockExpression(String expression) {
    //         this.expressionString = expression;
    //     }
    // }

}
