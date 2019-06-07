package com.zte.crm.prm.anno;

import com.zte.crm.prm.RemoteServiceQualifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Qualifier(RemoteServiceQualifier.PROVIDER)
public @interface RemoteServiceProvider {

}
