package org.example.nicop.demo_actuator.config;

import org.example.nicop.demo_actuator.util.PageHandlerMethodArgumentResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
@Slf4j
public class WebConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        log.info("Configuring default argument resolver for page and size");
        PageHandlerMethodArgumentResolver pageResolver = new PageHandlerMethodArgumentResolver();
        argumentResolvers.add(pageResolver);
        pageResolver.setFallbackPageable(new PageRequest(0, 25));

        argumentResolvers.add(pageResolver);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        log.info("Configuring default content type negotiation to JSON");
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
}
