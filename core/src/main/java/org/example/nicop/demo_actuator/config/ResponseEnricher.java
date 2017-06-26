package org.example.nicop.demo_actuator.config;


import org.example.nicop.demo_actuator.util.ResponseWrapperProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.Map;

import static org.example.nicop.demo_actuator.util.UriUtils.splitQuery;

@Slf4j
@ControllerAdvice
public class ResponseEnricher implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        if(methodParameter.getContainingClass().getName().startsWith("org.springframework.boot.actuate")){
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType mediaType,
                                  Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest,
                                  ServerHttpResponse serverHttpResponse) {

        ServletServerHttpRequest sshr = (ServletServerHttpRequest) serverHttpRequest;
        Map<String, String> params = splitQuery(sshr.getURI());

        ResponseWrapperProcessor processor = new ResponseWrapperProcessor(body, params);

        processor.process();

        // positionnement des headers
        HttpHeaders headers = processor.getResponseHeaders();
        for(Map.Entry<String, List<String>> entry : headers.entrySet()) {
            serverHttpResponse.getHeaders().put(entry.getKey(), entry.getValue());
        }

        // positionnement du statut
        serverHttpResponse.setStatusCode(processor.getStatus());

        return processor.getMappingJacksonValue();
    }





}
