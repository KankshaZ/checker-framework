package org.checkerframework.checker.signature;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.InternalForm;
import org.checkerframework.checker.signature.qual.SignatureBottom;
import org.checkerframework.checker.signature.qual.SignatureUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

// TODO: Does not yet handle method signature annotations, such as
// @MethodDescriptor.

/** Accounts for the effects of certain calls to String.replace. */
public class SignatureAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror SIGNATURE_UNKNOWN;
    protected final AnnotationMirror BINARY_NAME;
    protected final AnnotationMirror INTERNAL_FORM;

    /** The {@link String#replace(char, char)} method. */
    private final ExecutableElement replaceCharChar;

    /** The {@link String#replace(CharSequence, CharSequence)} method. */
    private final ExecutableElement replaceCharSequenceCharSequence;

    public SignatureAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        SIGNATURE_UNKNOWN = AnnotationBuilder.fromClass(elements, SignatureUnknown.class);
        BINARY_NAME = AnnotationBuilder.fromClass(elements, BinaryName.class);
        INTERNAL_FORM = AnnotationBuilder.fromClass(elements, InternalForm.class);

        replaceCharChar =
                TreeUtils.getMethod(
                        java.lang.String.class.getName(), "replace", processingEnv, "char", "char");
        replaceCharSequenceCharSequence =
                TreeUtils.getMethod(
                        java.lang.String.class.getName(),
                        "replace",
                        processingEnv,
                        "java.lang.CharSequence",
                        "java.lang.CharSequence");

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithPolyAll(SignatureUnknown.class, SignatureBottom.class);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new SignatureTreeAnnotator(this), super.createTreeAnnotator());
    }

    private class SignatureTreeAnnotator extends TreeAnnotator {

        public SignatureTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringConcatenation(tree)) {
                type.removeAnnotationInHierarchy(SIGNATURE_UNKNOWN);
                // This could be made more precise.
                type.addAnnotation(SignatureUnknown.class);
            }
            return null; // super.visitBinary(tree, type);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringCompoundConcatenation(node)) {
                type.removeAnnotationInHierarchy(SIGNATURE_UNKNOWN);
                // This could be made more precise.
                type.addAnnotation(SignatureUnknown.class);
            }
            return null; // super.visitCompoundAssignment(node, type);
        }

        /**
         * String.replace, when called with specific constant arguments, converts between internal
         * form and binary name.
         *
         * <pre><code>
         * {@literal @}InternalForm String internalForm = binaryName.replace('.', '/');
         * {@literal @}BinaryName String binaryName = internalForm.replace('/', '.');
         * </code></pre>
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isMethodInvocation(tree, replaceCharChar, processingEnv)) {
                ExpressionTree arg0 = tree.getArguments().get(0);
                ExpressionTree arg1 = tree.getArguments().get(1);
                if (arg0.getKind() == Tree.Kind.CHAR_LITERAL
                        && arg1.getKind() == Tree.Kind.CHAR_LITERAL) {
                    char const0 = (char) ((LiteralTree) arg0).getValue();
                    char const1 = (char) ((LiteralTree) arg1).getValue();
                    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
                    final AnnotatedTypeMirror receiverType = getAnnotatedType(receiver);
                    if (const0 == '.'
                            && const1 == '/'
                            && receiverType.getAnnotation(BinaryName.class) != null) {
                        type.replaceAnnotation(INTERNAL_FORM);
                    } else if (const0 == '/'
                            && const1 == '.'
                            && receiverType.getAnnotation(InternalForm.class) != null) {
                        type.replaceAnnotation(BINARY_NAME);
                    }
                }
            } else if (TreeUtils.isMethodInvocation(
                    tree, replaceCharSequenceCharSequence, processingEnv)) {
                ExpressionTree arg0 = tree.getArguments().get(0);
                ExpressionTree arg1 = tree.getArguments().get(1);
                if (arg0.getKind() == Tree.Kind.STRING_LITERAL
                        && arg1.getKind() == Tree.Kind.STRING_LITERAL) {
                    String const0 = (String) ((LiteralTree) arg0).getValue();
                    String const1 = (String) ((LiteralTree) arg1).getValue();
                    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
                    final AnnotatedTypeMirror receiverType = getAnnotatedType(receiver);
                    if (const0.equals(".")
                            && const1.equals("/")
                            && receiverType.getAnnotation(BinaryName.class) != null) {
                        type.replaceAnnotation(INTERNAL_FORM);
                    } else if (const0.equals("/")
                            && const1.equals(".")
                            && receiverType.getAnnotation(InternalForm.class) != null) {
                        type.replaceAnnotation(BINARY_NAME);
                    }
                }
            }
            return super.visitMethodInvocation(tree, type);
        }
    }
}
