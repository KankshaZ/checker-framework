package org.checkerframework.checker.deadlock;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.deadlock.qual.Acquires;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The DeadlockVisitor enforces the special type-checking rules described in the Deadlock Checker
 * manual chapter.
 *
 * @checker_framework.manual #deadlock-checker Deadlock Checker
 */
public class DeadlockVisitor extends BaseTypeVisitor<DeadlockAnnotatedTypeFactory> {

    public DeadlockVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {

        TypeMirror tm = TreeUtils.typeOf(node);
        System.out.println("VARIABLE DECLARATION: " + tm);
        AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
        System.out.println("annotated type: " + atm);

        return super.visitVariable(node, p);
    }
    /*
    @Override
    public DeadlockAnnotatedTypeFactory createTypeFactory() {
        return new DeadlockAnnotatedTypeFactory(checker);
    }*/

    @Override
    public Void visitMethod(MethodTree node, Void p) {

        ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);
        System.out.println("reached method: " + methodElement);

        return super.visitMethod(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        ExecutableElement methodElement = TreeUtils.elementFromUse(node);
        System.out.println("METHOD INVOCATION:  " + methodElement);
        AnnotationMirror AcquiresAnno =
                atypeFactory.getDeclAnnotation(methodElement, Acquires.class);
        System.out.println("annotation on method: " + AcquiresAnno);
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {

        ExpressionTree synchronizedExpression = node.getExpression();
        System.out.println("LOCK ACQUIRED BY SYNC_STATEMENT: " + synchronizedExpression);

        return super.visitSynchronized(node, p);
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {

        ArrayList<AnnotationTree> annotationTreeList = new ArrayList<>(1);
        annotationTreeList.add(tree);
        List<AnnotationMirror> amList =
                TreeUtils.annotationsFromTypeAnnotationTrees(annotationTreeList);

        System.out.println("ANNOTATION FOUND: " + amList);

        return super.visitAnnotation(tree, p);
    }
}
