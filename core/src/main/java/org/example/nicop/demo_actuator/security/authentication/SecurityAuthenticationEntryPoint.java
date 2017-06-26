package org.example.nicop.demo_actuator.security.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.nicop.demo_actuator.util.ErrorMessage;
import org.example.nicop.demo_actuator.util.ResponseWrapper;
import org.example.nicop.demo_actuator.util.RestErrorList;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

import static java.util.Collections.singletonMap;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * SecurityAuthenticationEntryPoint is called by ExceptionTranslationFilter to handle all AuthenticationException.
 * These exceptions are thrown when authentication failed : wrong login/password, authentication unavailable, invalid token
 * authentication expired, etc.
 *
 * For problems related to access (roles), see RestAccessDeniedHandler.
 */
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        if(authException instanceof AccountExpiredException) {
            doInternal(request, response, (AccountExpiredException)authException);
        } else if(authException instanceof InvalidGrantException) {
            doInternal(request, response, (InvalidGrantException)authException);
        } else if(authException instanceof MalformedJwtException) {
            doInternal(request, response, (MalformedJwtException)authException);
        }
        else {
            doGenericInternal(request, response, SC_UNAUTHORIZED, new ErrorMessage(authException.getMessage(),  "TOKEN_UNAVAILABLE", "Please provide an access Token to access this request"));
        }
    }

    private void doInternal(HttpServletRequest request, HttpServletResponse response, InvalidGrantException authException)  throws IOException, ServletException {
        doGenericInternal(request, response, SC_UNAUTHORIZED, new ErrorMessage(authException.getMessage(),  "INVALID_GRANT", "Token has invalid grant"));
    }

    private void doInternal(HttpServletRequest request, HttpServletResponse response, AccountExpiredException authException)  throws IOException, ServletException {
        doGenericInternal(request, response, SC_UNAUTHORIZED, new ErrorMessage(authException.getMessage(),  "TOKEN_EXPIRED", "Token has expired"));
    }

    private void doInternal(HttpServletRequest request, HttpServletResponse response, MalformedJwtException authException)  throws IOException, ServletException {
        doGenericInternal(request, response, SC_UNAUTHORIZED, new ErrorMessage(authException.getMessage(), "TOKEN_INVALID", "Token is invalid"));
    }

    private void doGenericInternal(HttpServletRequest request, HttpServletResponse response, int status, ErrorMessage message)  throws IOException, ServletException {
        RestErrorList errorList = new RestErrorList(status, message);
        ResponseWrapper responseWrapper = new ResponseWrapper(null, singletonMap("status", status), errorList);
        ObjectMapper objMapper = new ObjectMapper();

        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
        wrapper.setStatus(status);
        wrapper.setContentType(APPLICATION_JSON_VALUE);
        wrapper.getWriter().println(objMapper.writeValueAsString(responseWrapper));
        wrapper.getWriter().flush();
    }


}
