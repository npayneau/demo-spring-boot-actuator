package org.example.nicop.demo_actuator.config;


import org.example.nicop.demo_actuator.util.RestServiceException;
import org.example.nicop.demo_actuator.util.RestErrorList;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class RestServiceExceptionHandler {

     @ExceptionHandler
       @ResponseBody
       public RestErrorList handleException(RestServiceException ex) {

           return ex.getErrors();
       }
}
