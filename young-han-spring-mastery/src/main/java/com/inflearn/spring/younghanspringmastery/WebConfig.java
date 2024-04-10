package com.inflearn.spring.younghanspringmastery;

import com.inflearn.spring.younghanspringmastery.web.argumentresolver.LoginMemberArgumentResolver;
import com.inflearn.spring.younghanspringmastery.web.exception.resolver.UserHandlerExceptionResolver;
import com.inflearn.spring.younghanspringmastery.web.interceptor.LogInterceptor;
import com.inflearn.spring.younghanspringmastery.web.interceptor.LoginCheckInterceptor;
import com.inflearn.spring.younghanspringmastery.web.exception.resolver.MyHandlerExceptionResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor())
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**","/*.ico","/error");

        registry.addInterceptor(new LoginCheckInterceptor())
                .order(2)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/members/add", "/login", "/logout", "/css/**","/*.ico","/error","/api/**");
    }
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add(new MyHandlerExceptionResolver());
        resolvers.add(new UserHandlerExceptionResolver());
    }
}
