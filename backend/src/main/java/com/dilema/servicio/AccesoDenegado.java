package com.dilema.servicio;

public class AccesoDenegado extends RuntimeException {
    public AccesoDenegado(String mensaje) { super(mensaje); }
}
