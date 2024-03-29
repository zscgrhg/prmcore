package com.zte.crm.prm;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractJavacHelper extends AbstractProcessor {
    protected Messager javacMessager;
    protected JavacTrees javacTrees;
    protected TreeMaker make;
    protected Names javacNames;
    protected ProcessingEnvironment processingEnv;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.javacMessager = processingEnv.getMessager();
        this.javacTrees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.make = TreeMaker.instance(context);
        this.javacNames = Names.instance(context);
        this.processingEnv=processingEnv;

    }

    protected JCTree.JCExpression dottedId(String dotted, int pos) {
        if (pos >= 0) {
            make.at(pos);
        }
        String[] idents = dotted.split("\\.");
        JCTree.JCExpression ret = make.Ident(javacNames.fromString(idents[0]));

        for (int i = 1; i < idents.length; i++) {
            ret = make.Select(ret, javacNames.fromString(idents[i]));
        }
        return ret;
    }

    protected String toDottedId(Type classType) {
        Symbol.TypeSymbol tsym = classType.tsym;
        if (tsym.owner != null
                && tsym.owner.name != null
                && !tsym.owner.name.toString().isEmpty()) {
            return toDottedId(tsym.owner.type) + "." + tsym.name.toString();
        } else {
            return tsym.name.toString();
        }
    }

    protected JCTree.JCExpression javaTypeExpr(String javaTypeName) {
        return dottedId(javaTypeName, 0);
    }

    protected JCTree.JCTypeCast typeCastExpr(String id, String javaType) {
        return typeCastExpr(id, javaTypeExpr(javaType));
    }

    protected JCTree.JCTypeCast typeCastExpr(String id, JCTree.JCExpression javaType) {
        return make
                .TypeCast(javaType,
                        make.Ident(javacNames.fromString(id)));
    }

    protected JCTree.JCVariableDecl varDecl(JCTree.JCModifiers mods,
                                            Name name,
                                            JCTree.JCExpression javaType,
                                            JCTree.JCExpression init) {
        return make.VarDef(mods,
                name,
                javaType,
                init);
    }

    protected JCTree.JCVariableDecl varDecl(JCTree.JCModifiers mods,
                                            String name,
                                            JCTree.JCExpression javaType,
                                            JCTree.JCExpression init) {
        return varDecl(mods, javacNames.fromString(name), javaType, init);
    }

    protected JCTree.JCVariableDecl varDecl(long modifires,
                                            String name,
                                            JCTree.JCExpression javaType,
                                            JCTree.JCExpression init) {
        return varDecl(make.Modifiers(modifires),
                javacNames.fromString(name),
                javaType,
                init);
    }

    protected JCTree.JCVariableDecl varDecl(long modifires,
                                            String name,
                                            String javaType,
                                            JCTree.JCExpression init) {
        return varDecl(make.Modifiers(modifires),
                javacNames.fromString(name),
                javaTypeExpr(javaType),
                init);
    }


    protected JCTree.JCMethodInvocation invocation(JCTree.JCExpression methodExpr, List<JCTree.JCExpression> args) {
        return make.Apply(List.nil(), methodExpr, args);
    }

    protected JCTree.JCMethodInvocation invocation(String methodRef, List<JCTree.JCExpression> args) {
        return invocation(dottedId(methodRef, -1), args);
    }


    protected JCTree.JCThrow throwByName(String name, String msg) {
        JCTree.JCExpression exception = make.NewClass(null, List.<JCTree.JCExpression>nil(),
                javaTypeExpr(name),
                List.<JCTree.JCExpression>of(make.Literal(TypeTag.CLASS, msg)), null);
        return make.Throw(exception);
    }

    protected JCTree.JCThrow throwByName(String name) {
        JCTree.JCExpression exception = make.NewClass(null, List.<JCTree.JCExpression>nil(),
                javaTypeExpr(name),
                List.<JCTree.JCExpression>nil(), null);
        return make.Throw(exception);
    }

    protected JCTree.JCMethodInvocation invocation(String methodRef) {
        return invocation(methodRef, List.nil());
    }

    protected JCTree.JCMethodInvocation invocation(String varRef, Name name, List<JCTree.JCExpression> args) {

        return invocation(make.Select(dottedId(varRef, -1),
                name), args);
    }

    protected JCTree.JCReturn returnRef(String name) {
        return make.Return(make.Ident(javacNames.fromString(name)));
    }

    protected JCTree.JCReturn returnDottedRef(String name) {
        return make.Return(dottedId(name, -1));
    }

    protected JCTree.JCReturn returnInvocation(JCTree.JCMethodInvocation invocation) {
        return make.Return(invocation);
    }

    protected JCTree.JCBlock block(JCTree.JCStatement... statements) {
        ListBuffer<JCTree.JCStatement> buffer = new ListBuffer<>();
        buffer.appendArray(statements);
        return make.Block(0, buffer.toList());
    }


    protected boolean hasAnnotation(Type.ClassType classType, Class<? extends Annotation> anno) {
        return (classType
                .asElement()
                .getDeclarationAttributes()
                .stream()
                .filter(t -> anno
                        .getCanonicalName()
                        .equals(t.type.tsym.toString()))
                .findAny()
                .isPresent()

                || classType.getAnnotation(anno) != null);
    }

    protected Stream<Type.ClassType> interfacesFieldOf(Type.ClassType classType) {
        return Stream
                .of(classType.all_interfaces_field, classType.interfaces_field)
                .filter(Objects::nonNull)
                .flatMap(t -> t.stream())
                .filter(t -> t instanceof Type.ClassType)
                .map(t -> (Type.ClassType) t);
    }

    protected Type supertypeFieldOf(Type.ClassType classType) {


        return classType.supertype_field;
    }

    protected Type tsymType(Type.ClassType classType) {


        return Optional
                .ofNullable(classType)
                .map(t -> t.tsym)
                .map(t -> t.type)
                .orElse(null);
    }


    protected Stream<Type.ClassType> superTypeAndInterfaceOf(Type.ClassType classType) {
        return Stream
                .of(interfacesFieldOf(classType), Stream.of(supertypeFieldOf(classType)))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .filter(t -> t instanceof Type.ClassType)
                .map(t -> (Type.ClassType) t);
    }

    protected JCTree.JCAnnotation annotationExpr(String anno, Collection<?> values) {
        List<JCTree.JCExpression> args = Optional
                .ofNullable(values)
                .filter(t -> !t.isEmpty())
                .map(tt -> List.of((JCTree.JCExpression) make.NewArray(null, List.nil(), List.from(tt
                        .stream()
                        .map(t -> make.Literal(t))
                        .collect(Collectors.toList())))))
                .orElse(List.nil());

        return
                make.Annotation(javaTypeExpr(anno),
                        args);
    }


    protected java.util.List<Type.ClassType> getAnnotationValues(Element element, Class<? extends Annotation> annotation){
        java.util.List<? extends AnnotationMirror> mixList = element
                .getAnnotationMirrors()
                .stream()
                .filter(t -> annotation
                        .getCanonicalName()
                        .equals(t.getAnnotationType().toString()))
                .collect(Collectors.toList());
        if(mixList.size()==0){
            return Collections.emptyList();
        }
        java.util.List<Type.ClassType> ret=mixList.stream()
                .flatMap(annotationMirror->{
                    Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();

                    Attribute.Array value = (Attribute.Array)(elementValues
                            .values()
                            .stream()
                            .findFirst()
                            .get());
                    return value
                            .getValue()
                            .stream()
                            .map(t -> {
                                Type.ClassType ctype=
                                        (Type.ClassType)t
                                        .getValue();
                                return ctype;
                            });

                }).collect(Collectors.toList());
        return ret;
    }

    protected void addSource(String pkg, JCTree.JCClassDecl classDecl) {
        try (Writer writer = processingEnv
                .getFiler()
                .createSourceFile(pkg + "." + classDecl.name)
                .openWriter();
             PrintWriter printWriter = new PrintWriter(writer)) {
            printWriter.print("package " + pkg + ";\n");
            printWriter.print(classDecl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
