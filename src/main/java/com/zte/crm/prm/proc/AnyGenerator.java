package com.zte.crm.prm.proc;

import com.zte.crm.prm.AbstractJavacHelper;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnyGenerator  extends AbstractJavacHelper {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println(this+"  round="+roundEnv.hashCode()+" // "+roundEnv);
        return true;
    }
}
