package com.zte.crm.prm.anno;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteServiceContract {

    String name();

    String context() default "";

    String path() default "";

    String url() default "";

    String qualifier() default "";
}
