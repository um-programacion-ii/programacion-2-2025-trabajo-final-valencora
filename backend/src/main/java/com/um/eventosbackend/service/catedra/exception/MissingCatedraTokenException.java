package com.um.eventosbackend.service.catedra.exception;

/**
 * Se lanza cuando se intenta invocar a la cátedra sin contar con un token registrado.
 */
public class MissingCatedraTokenException extends RuntimeException {

    public MissingCatedraTokenException() {
        super("No hay token configurado para invocar al servicio de la cátedra");
    }
}

