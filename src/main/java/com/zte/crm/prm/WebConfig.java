package com.zte.crm.prm;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class WebConfig extends DelegatingWebMvcConfiguration {


    @Override
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override
            protected boolean isHandler(Class<?> beanType) {
                return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class));
            }
        };
    }
}
