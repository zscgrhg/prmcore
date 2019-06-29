package com.zte.crm.prm.proc;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

public class MixinClassVisitor extends ClassVisitor {
    public MixinClassVisitor(int api) {
        super(api);
    }

    public MixinClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        System.out.println(value);
        return super.visitField(access, name, descriptor, signature, value);
    }
}
