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
        final String genPkgName = ctype.tsym.name.toString() + ".codegen";

        RemoteServiceContract[] remoteServiceContracts = contractClazz.getAnnotationsByType(RemoteServiceContract.class);
        assert remoteServiceContracts.length == 1;
        String name = remoteServiceContracts[0].name();
        JCTree.JCAssign jcAssign = make.Assign(make.Ident(javacNames.fromString("name")),
                make.Literal(name));

        final long genClassFlag = Flags.INTERFACE;
        JCTree.JCAnnotation annotation =
                make.Annotation(javaTypeExpr(CLASS_FC),
                        List.of(jcAssign));
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
