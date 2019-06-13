package com.zte.crm.prm.proc;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.zte.crm.prm.AbstractJavacHelper;
import com.zte.crm.prm.anno.Mixin;
import com.zte.crm.prm.anno.RemoteServiceProvider;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("com.zte.crm.prm.anno.Mixin")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MixinGenerator extends AbstractJavacHelper {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> rootElements = roundEnv.getRootElements();
        Map<String, ? extends List<? extends Element>> rootElementMap = rootElements.stream()
                .collect(Collectors.groupingBy(e -> e.toString(), Collectors.toList()));
        Map<String,Map<String, JCTree.JCExpression>> varDefsMap=new HashMap<>();
        rootElementMap.entrySet().forEach(en->{
            String key = en.getKey();
            en.getValue().forEach(env->{
                varDefsMap.putIfAbsent(key,new HashMap<>());
                Map<String, JCTree.JCExpression> defMap = varDefsMap.get(key);
                JCTree tree = javacTrees.getTree(env);
                if(tree instanceof JCTree.JCClassDecl){
                    JCTree.JCClassDecl classDecl= (JCTree.JCClassDecl) tree;
                    classDecl.defs.forEach(d->{
                        d.accept(new TreeTranslator(){
                            @Override
                            public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                                super.visitVarDef(jcVariableDecl);
                                defMap.put(jcVariableDecl.name.toString(),jcVariableDecl.init);
                            }
                        });
                    });
                }

            });
        });


        Set<? extends Element> mixinSource = roundEnv.getElementsAnnotatedWith(Mixin.class);

        mixinSource.forEach(m->{
            if (m instanceof Symbol.VarSymbol) {
                Symbol.VarSymbol mVar = (Symbol.VarSymbol) m;
                String ownerKey = mVar.owner.toString();
                JCTree.JCClassDecl ownerClassDecl= (JCTree.JCClassDecl) javacTrees
                        .getTree(rootElementMap.get(ownerKey).get(0));
                if (mVar.type instanceof Type.ClassType) {
                    Type.ClassType ctype = (Type.ClassType) mVar.type;
                    System.out.println(ctype);

                    String ctypeName = ctype.toString();
                    List<? extends Element> elements= rootElementMap.get(ctypeName);
                    Element element = elements.get(0);
                    JCTree tree = javacTrees.getTree(element);
                    if(tree instanceof JCTree.JCClassDecl){
                        JCTree.JCClassDecl classDecl= (JCTree.JCClassDecl) tree;
                        classDecl.defs.forEach(d->{
                            d.accept(new TreeTranslator(){
                                @Override
                                public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                                    super.visitVarDef(jcVariableDecl);
                                    Symbol.VarSymbol clone = jcVariableDecl.sym.clone(mVar.owner);
                                    clone.appendAttributes(jcVariableDecl.sym.getDeclarationAttributes());
                                    JCTree.JCExpression initExpr = varDefsMap.get(ctypeName).get(clone.name.toString());
                                    ownerClassDecl.defs=  ownerClassDecl.defs.append(make.VarDef(clone,initExpr));

                                }
                            });
                        });
                        List<JCTree> removeMixin = ownerClassDecl.defs.stream().filter(d -> {
                            if(d instanceof JCTree.JCVariableDecl){
                                JCTree.JCVariableDecl decl= (JCTree.JCVariableDecl) d;
                                Mixin[] mixins = decl.sym.getAnnotationsByType(Mixin.class);
                                if(mixins==null||mixins.length==0){
                                    return true;
                                }else {
                                    return false;
                                }
                            }
                            return true;
                        }).collect(Collectors.toList());
                        ownerClassDecl.defs=com.sun.tools.javac.util.List.from(removeMixin);
                    }
                }
            }
        });
        return true;
    }
}
