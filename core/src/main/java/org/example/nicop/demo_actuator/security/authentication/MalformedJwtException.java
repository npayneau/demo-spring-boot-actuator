package org.example.nicop.demo_actuator.security.authentication;

import org.springframework.security.core.AuthenticationException;

public class MalformedJwtException extends AuthenticationException {
    public MalformedJwtException(String message) {
        super(message);
    }
}
