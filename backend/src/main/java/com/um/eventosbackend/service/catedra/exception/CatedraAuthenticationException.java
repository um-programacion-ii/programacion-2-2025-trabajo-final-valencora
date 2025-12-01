package com.um.eventosbackend.service.catedra.exception;

import org.springframework.http.HttpStatus;

/**
 * Error genérico de autenticación al hablar con la cátedra.
 */
public class CatedraAuthenticationException extends RuntimeException {

    private final HttpStatus status;

    public CatedraAuthenticationException(String message) {
        this(HttpStatus.UNAUTHORIZED, message);
    }

    public CatedraAuthenticationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

