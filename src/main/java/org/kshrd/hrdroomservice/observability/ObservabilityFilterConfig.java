package org.kshrd.hrdroomservice.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kshrd.hrdroomservice.config.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RateLimitProperties.class, ObservabilityProperties.class})
public class ObservabilityFilterConfig {

    @Bean
    FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration(
            ClientIpResolver clientIpResolver) {
        FilterRegistrationBean<RequestContextFilter> registration =
                new FilterRegistrationBean<>(new RequestContextFilter(clientIpResolver));
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

    @Bean
    FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(
            ObjectMapper objectMapper,
            RateLimitProperties rateLimitProperties,
            ClientIpResolver clientIpResolver) {
        FilterRegistrationBean<RateLimitingFilter> registration =
                new FilterRegistrationBean<>(
                        new RateLimitingFilter(
                                objectMapper, rateLimitProperties, clientIpResolver));
        registration.setOrder(-180);
        return registration;
    }
}
