package org.kshrd.hrdroomservice.observability;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityFilterConfig {

    @Bean
    FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration() {
        FilterRegistrationBean<RequestContextFilter> registration =
                new FilterRegistrationBean<>(new RequestContextFilter());
        registration.setOrder(-200);
        return registration;
    }

    @Bean
    FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() {
        FilterRegistrationBean<UserContextFilter> registration =
                new FilterRegistrationBean<>(new UserContextFilter());
        registration.setOrder(0);
        return registration;
    }
}
