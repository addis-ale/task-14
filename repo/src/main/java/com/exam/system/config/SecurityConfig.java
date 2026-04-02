package com.exam.system.config;

import com.exam.system.job.JobShardProperties;
import com.exam.system.security.filter.AuthFilter;
import com.exam.system.security.filter.RateLimitFilter;
import com.exam.system.security.filter.ReplayGuardFilter;
import com.exam.system.security.filter.RequestSizeLimitFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SecurityProperties.class, JobShardProperties.class})
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<RequestSizeLimitFilter> requestSizeFilterRegistration(RequestSizeLimitFilter filter) {
        FilterRegistrationBean<RequestSizeLimitFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns("/api/*");
        bean.setOrder(1);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<ReplayGuardFilter> replayGuardFilterRegistration(ReplayGuardFilter filter) {
        FilterRegistrationBean<ReplayGuardFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns("/api/*");
        bean.setOrder(2);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns("/api/*");
        bean.setOrder(3);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthFilter filter) {
        FilterRegistrationBean<AuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns("/api/*");
        bean.setOrder(4);
        return bean;
    }
}
