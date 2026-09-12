package com.sierradorada.sdbrewpi.shared;

public class TankNotFoundException extends RuntimeException {
    public TankNotFoundException(String id) {
        super("No existe el tanque " + id);
    }
}

