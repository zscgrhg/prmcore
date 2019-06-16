package com.zte.crm.prm.proc;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.zte.crm.prm.AbstractJavacHelper;
import com.zte.crm.prm.anno.Mixin;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("com.zte.crm.prm.anno.Mixin")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MixinGenerator extends AbstractJavacHelper {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> rootElements = roundEnv.getRootElements();

        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(Mixin.class);
        Map<String, Element> eleMap = new HashMap<>();
        for (Element element : rootElements) {
            eleMap.put(element.toString(), element);
        }
        Map<String, List<String>> mixinMap = new HashMap<>();
        for (Element element : annotated) {
            List<String> av = getAnnotationValues(element, Mixin.class);
            mixinMap.put(element.toString(), av);
        }

        Map<String, ? extends List<? extends Element>> rootElementMap = rootElements
                .stream()
                .collect(Collectors.groupingBy(e -> e.toString(), Collectors.toList()));
        Map<String, Map<String, JCTree.JCExpression>> varDefsMap = new HashMap<>();
        Map<String, Map<String, Boolean>> varDefsMapNoInit = new HashMap<>();
        rootElementMap
                .entrySet()
                .forEach(en -> {
                    String key = en.getKey();
                    en
                            .getValue()
                            .forEach(env -> {
                                varDefsMap.putIfAbsent(key, new HashMap<>());
                                varDefsMapNoInit.putIfAbsent(key, new HashMap<>());
                                Map<String, JCTree.JCExpression> defMap = varDefsMap.get(key);
                                Map<String, Boolean> noinit=varDefsMapNoInit.get(key);
                                JCTree tree = javacTrees.getTree(env);
                                if (tree instanceof JCTree.JCClassDecl) {
                                    JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
                                    classDecl.defs.forEach(d -> {
                                        d.accept(new TreeTranslator() {
                                            @Override
                                            public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                                                super.visitVarDef(jcVariableDecl);
                                                if(jcVariableDecl.init==null){
                                                    noinit.put(jcVariableDecl.name.toString(),true);
                                                }else {
                                                    defMap.put(jcVariableDecl.name.toString(), jcVariableDecl.init);
                                                }

                                            }
                                        });
                                    });
                                }

                            });
                });


        while (!mixinMap.isEmpty()) {
            int c = 0;
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
                    mix(eleMap.get(s), eleMap.get(m), varDefsMap,varDefsMapNoInit);
                }
                mixinMap.remove(m);
                c++;
            }
            if (c == 0) {
                break;
            }
        }

        return true;
    }

    private void mix(Element from, Element to, Map<String, Map<String, JCTree.JCExpression>> varDefsMap,
                     Map<String, Map<String, Boolean>> varDefsMapNoInit) {
        JCTree fromTree = javacTrees.getTree(from);
        JCTree toTree = javacTrees.getTree(to);
        if (fromTree instanceof JCTree.JCClassDecl &&
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

                        if(varDefsMap
                                .get(to.toString()).containsKey(tmpl.name.toString())||
                                varDefsMapNoInit
                                        .get(to.toString()).containsKey(tmpl.name.toString())){
                            return;
                        }

                        Symbol.VarSymbol clone = new Symbol.VarSymbol(tmpl.flags_field, tmpl.name,
                                make.Type(tmpl.type.tsym.type).type, toClassDecl.sym);
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
