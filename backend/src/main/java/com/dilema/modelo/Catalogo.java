package com.dilema.modelo;

public final class Catalogo {
    private Catalogo() {}

    public enum Rol { ADMIN, PROFESOR, ESTUDIANTE }

    public enum EstadoCaso { BORRADOR, PUBLICADO, ARCHIVADO }

    /** No se guarda. Se calcula con inicio_real y cierre_real. */
    public enum EstadoSesion { SIN_INICIAR, EN_CURSO, FINALIZADO }
}
