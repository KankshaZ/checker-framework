package org.checkerframework.checker.deadlock;

import org.checkerframework.checker.deadlock.qual.Acquires;
import org.checkerframework.checker.deadlock.qual.AcquiredAfter;
import org.checkerframework.checker.deadlock.qual.AcquiredAfterUnknown;

import static org.checkerframework.javacutil.TreeUtils.getReceiverTree;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.deadlock.DeadlockAnnotatedTypeFactory;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The DeadlockVisitor enforces the special type-checking rules described in the Deadlock Checker manual
 * chapter.
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