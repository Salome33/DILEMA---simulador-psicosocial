package com.dilema.servicio;

public class ReglaNegocio extends RuntimeException {
    public ReglaNegocio(String mensaje) { super(mensaje); }
}
