package com.zte.crm.prm.proc;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.zte.crm.prm.AbstractJavacHelper;
import com.zte.crm.prm.anno.RemoteServiceContract;
import org.springframework.cloud.openfeign.FeignClient;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("com.zte.crm.prm.anno.RemoteServiceContract")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ClientGenerator extends AbstractJavacHelper {

    public static final String CLASS_FC = FeignClient.class.getCanonicalName();


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(RemoteServiceContract.class);

        annotated.forEach(element -> {
            if (element instanceof Symbol.ClassSymbol) {
                Symbol.ClassSymbol contractClazz = (Symbol.ClassSymbol) element;


                if (contractClazz.type instanceof Type.ClassType) {
                    Type.ClassType ctype = (Type.ClassType) contractClazz.type;
                    genClient(contractClazz, ctype);

                }
            }
        });
        return true;
    }


    private void genClient(Symbol.ClassSymbol contractClazz, Type.ClassType ctype) {
        final String simpleName = "ClientOf" + ctype.tsym.name.toString();
        final String genPkgName = ctype.tsym.owner.toString() + ".codegen";

        RemoteServiceContract[] remoteServiceContracts = contractClazz.getAnnotationsByType(RemoteServiceContract.class);
        assert remoteServiceContracts.length == 1;
        RemoteServiceContract rsc = remoteServiceContracts[0];

        JCTree.JCAssign nameAssign = make.Assign(make.Ident(javacNames.fromString("name")),
                make.Literal(rsc.name()));

        JCTree.JCAssign urlAssign = make.Assign(make.Ident(javacNames.fromString("url")),
                make.Literal(rsc.url()));

        JCTree.JCAssign primaryAssign = make.Assign(make.Ident(javacNames.fromString("primary")),
                make.Literal(false));
        JCTree.JCExpression qualifier=make.Literal(rsc.qualifier());
        if(RemoteServiceContract.CLIENT_STUB.equals(rsc.qualifier())){
            qualifier=make.Ident(javacNames.fromString(RemoteServiceContract.class.getCanonicalName()
                    +".CLIENT_STUB"));
        }
        JCTree.JCAssign qualifierAssign = make.Assign(make.Ident(javacNames.fromString("qualifier")),
                qualifier);
        final long genClassFlag = Flags.INTERFACE|Flags.PUBLIC;
        JCTree.JCAnnotation annotation =
                make.Annotation(javaTypeExpr(CLASS_FC),
                        List.of(nameAssign, primaryAssign,qualifierAssign,urlAssign));
        ListBuffer<JCTree.JCAnnotation> annos = new ListBuffer<>();
        annos.append(annotation);
        JCTree.JCClassDecl generatedClass = make
                .ClassDef(make.Modifiers(genClassFlag, annos.toList()),
                        javacNames.fromString(simpleName),
                        List.nil(),
                        null,
                        List.of(javaTypeExpr(toDottedId(ctype))),
                        List.nil());
        addSource(genPkgName, generatedClass);
    }
}
