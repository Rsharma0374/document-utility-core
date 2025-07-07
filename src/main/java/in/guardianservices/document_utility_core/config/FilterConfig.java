package in.guardianservices.document_utility_core.config;

import in.guardianservices.document_utility_core.filter.RateLimiterFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RateLimiterFilter> rateLimiterFilterRegistration(RateLimiterFilter filter) {
        FilterRegistrationBean<RateLimiterFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
