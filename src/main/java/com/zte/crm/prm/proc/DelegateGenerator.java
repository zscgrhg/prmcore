package com.zte.crm.prm.proc;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.zte.crm.prm.AbstractJavacHelper;
import com.zte.crm.prm.anno.RemoteServiceContract;
import com.zte.crm.prm.anno.RemoteServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SupportedAnnotationTypes("com.zte.crm.prm.anno.RemoteServiceProvider")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DelegateGenerator extends AbstractJavacHelper {
    public static final String CLASS_AUTOWIRED = Autowired.class.getCanonicalName();
    public static final String CLASS_RC = RestController.class.getCanonicalName();
    public static final String CLASS_CFG = Configuration.class.getCanonicalName();
    public static final String CLASS_CSCAN = ComponentScan.class.getCanonicalName();
    public static final String CLASS_CONTRACT = RemoteServiceContract.class.getCanonicalName();
    public static final String CLASS_FEIGN = FeignClient.class.getCanonicalName();
    public static final ConcurrentHashMap<String, Boolean> HISTORY = new ConcurrentHashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println(this+"  round="+roundEnv.hashCode()+" // "+roundEnv);
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(RemoteServiceProvider.class);

        annotated.forEach(element -> {
            if (element instanceof Symbol.ClassSymbol) {
                Symbol.ClassSymbol producerClazz = (Symbol.ClassSymbol) element;
                if (producerClazz.type instanceof Type.ClassType) {
                    Type.ClassType ctype = (Type.ClassType) producerClazz.type;
                    Set<String> contractPkgs = new HashSet<>();
                    process(contractPkgs, producerClazz, ctype, true);
                    attachDelegateScan(contractPkgs, element);
                }
            }
        });
        return true;
    }

    private void process(Set<String> contractPkgs,
                         Symbol.ClassSymbol root,
                         Type.ClassType current,
                         boolean test) {
        if (current.isInterface() && isContract(current)) {
            genDelegateSource(contractPkgs, root, current);
        }
        interfacesFieldOf(current)
                .forEach(t -> process(contractPkgs, root, t, true));

        Type superfield = supertypeFieldOf(current);
        Type tsymType = tsymType(current);
        if (superfield != null && superfield instanceof Type.ClassType) {
            process(contractPkgs, root, (Type.ClassType) superfield, true);
        } else if (test && tsymType != null
                && tsymType instanceof Type.ClassType) {
            process(contractPkgs, root, (Type.ClassType) tsymType, false);
        }

    }

    private boolean isContract(Type.ClassType classType) {
        return hasAnnotation(classType, RemoteServiceContract.class);
    }


    private void attachDelegateScan(Set<String> contractPkgs, Element root) {
        if (!contractPkgs.isEmpty()) {
            JCTree jct = javacTrees.getTree(root);
            jct.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    super.visitClassDef(jcClassDecl);
                    attach(contractPkgs, jcClassDecl);
                }
            });
        }
    }

    private void attach(Set<String> contractPkgs, JCTree.JCClassDecl jcClassDecl) {
        final String simpleName = "DelegateLoader";


        final long genClassFlag = Flags.PUBLIC | Flags.STATIC;
        JCTree.JCAnnotation cfg =
                annotationExpr(CLASS_CFG, List.nil());

        JCTree.JCAnnotation csan =
                annotationExpr(CLASS_CSCAN, contractPkgs);
        List<JCTree.JCAnnotation> annos = List.of(cfg, csan);


        JCTree.JCClassDecl attached = make
                .ClassDef(make.Modifiers(genClassFlag, annos),
                        javacNames.fromString(simpleName),
                        List.nil(),
                        null,
                        List.nil(),
                        List.nil());

        jcClassDecl.defs = jcClassDecl.defs.prepend(attached);
    }

    private void genDelegateSource(Set<String> contractPkgs, Symbol.ClassSymbol jcClassDecl, Type.ClassType contract) {

        Boolean exist = HISTORY.putIfAbsent(contract.tsym.toString(), Boolean.TRUE);
        if (exist != null && exist) {
            return;
        }

        final String simpleName = "DelegateOf" + contract.tsym.name.toString();
        final String genPkgName = contract.tsym.owner.toString() + ".codegen";
        contractPkgs.add(genPkgName);
        final long genClassFlag = Flags.PUBLIC;
        JCTree.JCAnnotation restController =
                make.Annotation(javaTypeExpr(CLASS_RC),
                        List.nil());
        ListBuffer<JCTree.JCAnnotation> annos = new ListBuffer<>();
        annos.append(restController);

        contract
                .asElement()
                .getDeclarationAttributes()
                .forEach(da -> {
                    if (!CLASS_RC.equals(da.type.toString())
                            && !CLASS_CONTRACT.equals(da.type.toString())
                            && !CLASS_FEIGN.equals(da.type.toString())) {
                        annos.append(make.Annotation(da));
                    }

                });

        JCTree.JCClassDecl generatedClass = make
                .ClassDef(make.Modifiers(genClassFlag, annos.toList()),
                        javacNames.fromString(simpleName),
                        List.nil(),
                        null,
                        List.nil(),
                        List.nil());


        JCTree.JCAnnotation autowired = make.Annotation(
                javaTypeExpr(CLASS_AUTOWIRED),
                List.nil());


        JCTree.JCVariableDecl producerVar = varDecl(make.Modifiers(0L, List.of(autowired)),
                "provider", make.Type(jcClassDecl.type), null);


        generatedClass.defs = generatedClass.defs.prepend(producerVar);


        java.util.List<Symbol> enclosedElements = contract.tsym.getEnclosedElements();
        enclosedElements
                .stream()
                .filter(it -> it instanceof Symbol.MethodSymbol)
                .map(it -> (Symbol.MethodSymbol) it)
                .forEach(symbol -> {

                    Symbol.ClassSymbol classSymbol = new Symbol.ClassSymbol(genClassFlag,
                            generatedClass.name, jcClassDecl);
                    Symbol.MethodSymbol stub = new Symbol.MethodSymbol(
                            Flags.PUBLIC,
                            symbol.name,
                            symbol.type,
                            classSymbol
                    );

                    stub.params = symbol.params();
                    stub.savedParameterNames = symbol.savedParameterNames;
                    stub.appendAttributes(symbol.getDeclarationAttributes());
                    ListBuffer<JCTree.JCExpression> argExpr = new ListBuffer<>();
                    stub.params.forEach(arg -> argExpr.append(make.Ident(arg.name)));
                    JCTree.JCMethodInvocation invoke = invocation("this.provider." + stub.name, argExpr.toList());
                    JCTree.JCMethodDecl copyed =
                            make.MethodDef(stub, block(make.Return(invoke)));


                    generatedClass.defs = generatedClass.defs.prepend(copyed);
                });

        addSource(genPkgName, generatedClass);
    }
}
