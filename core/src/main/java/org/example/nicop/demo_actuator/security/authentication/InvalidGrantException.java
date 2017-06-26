package org.example.nicop.demo_actuator.security.authentication;


import org.springframework.security.core.AuthenticationException;

public class InvalidGrantException extends AuthenticationException {
    public InvalidGrantException(String message) {
        super(message);
    }
}
