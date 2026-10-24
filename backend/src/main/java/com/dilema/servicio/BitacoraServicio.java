package com.dilema.servicio;

import com.dilema.modelo.Usuario;
import org.springframework.stereotype.Service;

/** Los avisos y la auditoría no se guardan: la justificación de la base los deja fuera. */
@Service
public class BitacoraServicio {
    public void auditar(Usuario usuario, String accion, String detalle) {
    }

    public void avisar(Usuario destinatario, String canal, String titulo, String cuerpo) {
    }
}
