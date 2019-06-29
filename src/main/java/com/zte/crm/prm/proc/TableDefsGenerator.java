package com.zte.crm.prm.proc;

import com.baomidou.mybatisplus.annotation.TableField;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.zte.crm.prm.AbstractJavacHelper;
import com.zte.crm.prm.anno.TableDefs;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SupportedAnnotationTypes("com.zte.crm.prm.anno.TableDefs")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TableDefsGenerator extends AbstractJavacHelper {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(TableDefs.class);
        List<JCTree.JCVariableDecl> columns=new ArrayList<>();
        annotated.forEach(element -> {
            if (element instanceof Symbol.ClassSymbol) {

                JCTree tree = javacTrees.getTree(element);
                tree.accept(new TreeTranslator(){
                    @Override
                    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                        super.visitVarDef(jcVariableDecl);
                        Symbol.VarSymbol sym = jcVariableDecl.sym;
                        if(!Flags.isStatic(sym)){
                            columns.add(jcVariableDecl);
                        }
                    }
                });
                tree.accept(new TreeTranslator(){
                    @Override
                    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                        super.visitClassDef(jcClassDecl);
                        final String simpleName = "Column";
                        final long genClassFlag = Flags.PUBLIC | Flags.STATIC|Flags.FINAL;
                        JCTree.JCClassDecl columnsClassDecl = make
                                .ClassDef(make.Modifiers(genClassFlag, com.sun.tools.javac.util.List.nil()),
                                        javacNames.fromString(simpleName),
                                        com.sun.tools.javac.util.List.nil(),
                                        null,
                                        com.sun.tools.javac.util.List.nil(),
                                        com.sun.tools.javac.util.List.nil());

                        final long publicStaticFinale = Flags.PUBLIC | Flags.STATIC|Flags.FINAL;
                        for (JCTree.JCVariableDecl column : columns) {

//
//                            column.sym.getAnnotationMirrors().get(0).
//                                    member(javacNames.fromString("value")).getValue()



                            Optional<Attribute.Compound> compound = column.sym
                                    .getAnnotationMirrors().stream().findFirst();
                            String name = compound
                                    .map(ac -> ac.member(javacNames.fromString("value")))
                                    .map(acc -> String.valueOf(acc.getValue())).orElse(column.name.toString());

                            boolean exist= compound
                                    .map(ac -> ac.member(javacNames.fromString("exist")))
                                    .map(acc -> (boolean)acc.getValue()).orElse(true);


                            //


                            if(!exist){
                                continue;
                            }

                            String CNAME=toConstStyle(name);

                            JCTree.JCVariableDecl colDecl = varDecl(make.Modifiers(publicStaticFinale, com.sun.tools.javac.util.List.nil()),
                                    CNAME, javaTypeExpr("java.lang.String"), make.Literal(CNAME));
                            columnsClassDecl.defs=columnsClassDecl.defs.prepend(colDecl);
                        }

                        Symbol.ClassSymbol classSymbol = new Symbol.ClassSymbol(publicStaticFinale,
                                columnsClassDecl.name, jcClassDecl.sym);


                        Symbol.MethodSymbol init = new Symbol.MethodSymbol(
                                Flags.PRIVATE,
                                javacNames.fromString("<init>"),
                                new Type.JCVoidType(),
                                classSymbol
                        );

                        JCTree.JCMethodDecl empty =
                                make.MethodDef(init, block(throwByName(RuntimeException.class.getCanonicalName())));

                        columnsClassDecl.defs=columnsClassDecl.defs.prepend(empty);

                        jcClassDecl.defs = jcClassDecl.defs.prepend(columnsClassDecl);
                    }
                });
            }
        });
        return true;
    }

    private String toConstStyle(String name){
        return name
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
    }
}
