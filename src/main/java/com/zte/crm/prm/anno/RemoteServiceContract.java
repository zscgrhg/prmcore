package com.zte.crm.prm.anno;

import com.zte.crm.prm.RemoteServiceQualifier;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteServiceContract {

    String name();
    String url() default "";
    String qualifier() default RemoteServiceQualifier.CLIENT_STUB;
}
