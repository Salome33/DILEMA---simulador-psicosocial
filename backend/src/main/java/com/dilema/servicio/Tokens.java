package com.dilema.servicio;

import com.dilema.modelo.Usuario;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Tokens {
    private final Map<String, Long> activos = new ConcurrentHashMap<>();

    public String emitir(Long usuarioId) {
        String token = UUID.randomUUID().toString();
        activos.put(token, usuarioId);
        return token;
    }

    public Optional<Long> leer(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return Optional.ofNullable(activos.get(token));
    }

    public void revocar(String token) {
        if (token != null) activos.remove(token);
    }
}
