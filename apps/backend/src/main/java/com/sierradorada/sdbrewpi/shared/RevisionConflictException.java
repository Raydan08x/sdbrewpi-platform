package com.sierradorada.sdbrewpi.shared;

public class RevisionConflictException extends RuntimeException {
    public RevisionConflictException() {
        super("El estado cambió; actualiza la pantalla antes de reintentar");
    }
}
