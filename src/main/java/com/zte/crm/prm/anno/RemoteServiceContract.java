package com.zte.crm.prm.anno;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteServiceContract {
    String CLIENT_STUB="RemoteServiceContractClientStub";
    String name();
    String url() default "";
    String qualifier() default CLIENT_STUB;
}
