package com.zte.crm.prm.proc;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.zte.crm.prm.AbstractJavacHelper;
import com.zte.crm.prm.anno.Mixin;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SupportedAnnotationTypes("com.zte.crm.prm.anno.Mixin")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MixinGenerator extends AbstractJavacHelper {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println(this + "  round=" + roundEnv.hashCode() + " // " + roundEnv);
        Set<? extends Element> rootElements = roundEnv.getRootElements();

        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(Mixin.class);
        Map<String, Element> eleMap = new HashMap<>();
        for (Element element : rootElements) {
            eleMap.put(element.toString(), element);
        }
        Map<String, List<String>> mixinMap = new HashMap<>();
        for (Element element : annotated) {
           List<String> av=getAnnotationValues(element,Mixin.class);
           mixinMap.put(element.toString(),av);
        }

        Map<String, ? extends List<? extends Element>> rootElementMap = rootElements
                .stream()
                .collect(Collectors.groupingBy(e -> e.toString(), Collectors.toList()));
        Map<String, Map<String, JCTree.JCExpression>> varDefsMap = new HashMap<>();
        rootElementMap
                .entrySet()
                .forEach(en -> {
                    String key = en.getKey();
                    en
                            .getValue()
                            .forEach(env -> {
                                varDefsMap.putIfAbsent(key, new HashMap<>());
                                Map<String, JCTree.JCExpression> defMap = varDefsMap.get(key);
                                JCTree tree = javacTrees.getTree(env);
                                if (tree instanceof JCTree.JCClassDecl) {
                                    JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
                                    classDecl.defs.forEach(d -> {
                                        d.accept(new TreeTranslator() {
                                            @Override
                                            public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                                                super.visitVarDef(jcVariableDecl);
                                                defMap.put(jcVariableDecl.name.toString(), jcVariableDecl.init);
                                            }
                                        });
                                    });
                                }

                            });
                });



        while (!mixinMap.isEmpty()){
            int c=0;
            Set<String> kes = mixinMap.keySet();
            List<String> mixable = kes
                    .stream()
                    .filter(s -> {
                        List<String> vs = mixinMap.get(s);
                        return vs
                                .stream()
                                .allMatch(v -> !kes.contains(v));
                    })
                    .collect(Collectors.toList());
            for (String m : mixable) {
                List<String> v = mixinMap.get(m);
                for (String s : v) {
                    mix(eleMap.get(m),eleMap.get(s),varDefsMap);
                }
                mixinMap.remove(m);
                c++;
            }
            if(c==0){
                break;
            }
        }


        if(true){
            return true;
        }
        Set<? extends Element> mixinSource = roundEnv.getElementsAnnotatedWith(Mixin.class);

        mixinSource.forEach(m -> {
            if (m instanceof Symbol.VarSymbol) {
                Symbol.VarSymbol mVar = (Symbol.VarSymbol) m;
                String ownerKey = mVar.owner.toString();
                JCTree.JCClassDecl ownerClassDecl = (JCTree.JCClassDecl) javacTrees
                        .getTree(rootElementMap
                                .get(ownerKey)
                                .get(0));
                if (mVar.type instanceof Type.ClassType) {
                    Type.ClassType ctype = (Type.ClassType) mVar.type;
                    System.out.println(ctype);

                    String ctypeName = ctype.toString();
                    List<? extends Element> elements = rootElementMap.get(ctypeName);
                    Element element = elements.get(0);
                    JCTree tree = javacTrees.getTree(element);
                    if (tree instanceof JCTree.JCClassDecl) {
                        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
                        classDecl.defs.forEach(d -> {
                            d.accept(new TreeTranslator() {
                                @Override
                                public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                                    super.visitVarDef(jcVariableDecl);
                                    //Symbol.VarSymbol clone = jcVariableDecl.sym.clone(mVar.owner);
                                    Symbol.VarSymbol tmpl = jcVariableDecl.sym;
                                    Symbol.VarSymbol clone = new Symbol.VarSymbol(tmpl.flags_field, javacNames.fromString(tmpl.name.toString() + "_mixed"),
                                            tmpl.type, mVar.owner);
                                    clone.appendAttributes(jcVariableDecl.sym.getDeclarationAttributes());
                                    JCTree.JCExpression initExpr = varDefsMap
                                            .get(ctypeName)
                                            .get(clone.name.toString());
                                    ownerClassDecl.defs = ownerClassDecl.defs.append(make.VarDef(clone, initExpr));

                                }
                            });
                        });
                        List<JCTree> removeMixin = ownerClassDecl.defs
                                .stream()
                                .filter(d -> {
                                    if (d instanceof JCTree.JCVariableDecl) {
                                        JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) d;
                                        Mixin[] mixins = decl.sym.getAnnotationsByType(Mixin.class);
                                        if (mixins == null || mixins.length == 0) {
                                            return true;
                                        } else {
                                            return false;
                                        }
                                    }
                                    return true;
                                })
                                .collect(Collectors.toList());

                        // ownerClassDecl.defs=com.sun.tools.javac.util.List.from(removeMixin);
                    }
                }
            }
        });
        return true;
    }

    private void mix(Element from,Element to,Map<String, Map<String, JCTree.JCExpression>> varDefsMap){
        JCTree fromTree = javacTrees.getTree(from);
        JCTree toTree = javacTrees.getTree(to);
        if (fromTree instanceof JCTree.JCClassDecl&&
                toTree instanceof JCTree.JCClassDecl) {
            JCTree.JCClassDecl fromClassDecl = (JCTree.JCClassDecl) fromTree;
            JCTree.JCClassDecl toClassDecl = (JCTree.JCClassDecl) toTree;
            fromClassDecl.defs.forEach(d -> {
                d.accept(new TreeTranslator() {
                    @Override
                    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                        super.visitVarDef(jcVariableDecl);
                        //Symbol.VarSymbol clone = jcVariableDecl.sym.clone(mVar.owner);
                        Symbol.VarSymbol tmpl = jcVariableDecl.sym;

                        Symbol.VarSymbol clone = new Symbol.VarSymbol(tmpl.flags_field, tmpl.name,
                                make.Type(tmpl.type).type, toClassDecl.sym);
                        clone.appendAttributes(jcVariableDecl.sym.getDeclarationAttributes());
                        JCTree.JCExpression initExpr = varDefsMap
                                .get(from.toString())
                                .get(clone.name.toString());
                        toClassDecl.defs = toClassDecl.defs.append(make.VarDef(clone, initExpr));

                    }
                });
            });
        }
    }

}
