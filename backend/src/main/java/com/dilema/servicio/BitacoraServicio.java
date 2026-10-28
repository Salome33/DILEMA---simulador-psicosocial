package com.dilema.servicio;

import com.dilema.modelo.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Envía la convocatoria y la nota. Si el correo falla, la operación de negocio sigue. */
@Service
public class BitacoraServicio {
    private static final Logger log = LoggerFactory.getLogger(BitacoraServicio.class);
    private final JavaMailSender correo;
    private final String desde;

    public BitacoraServicio(JavaMailSender correo, @Value("${dilema.correo.desde}") String desde) {
        this.correo = correo;
        this.desde = desde;
    }

    public void auditar(Usuario usuario, String accion, String detalle) {
        log.info("auditoria usuario={} accion={} detalle={}", usuario == null ? null : usuario.email, accion, detalle);
    }

    public void avisar(Usuario destinatario, String canal, String titulo, String cuerpo) {
        if (!"CORREO".equals(canal) || destinatario == null || destinatario.email == null || destinatario.email.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(desde);
            mensaje.setTo(destinatario.email);
            mensaje.setSubject(titulo);
            mensaje.setText(cuerpo == null ? "" : cuerpo);
            correo.send(mensaje);
        } catch (RuntimeException error) {
            log.warn("No se pudo enviar el correo a {}: {}", destinatario.email, error.getMessage());
        }
    }
}
