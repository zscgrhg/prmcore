package com.zte.crm.prm.anno;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Qualifier(RemoteServiceProvider.QUALIFIER)
public @interface RemoteServiceProvider {
    String QUALIFIER = "RemoteServiceProvider";
}
