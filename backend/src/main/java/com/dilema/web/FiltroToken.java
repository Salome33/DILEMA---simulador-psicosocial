package com.dilema.web;

import com.dilema.modelo.Usuario;
import com.dilema.repo.UsuarioRepo;
import com.dilema.servicio.Tokens;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class FiltroToken implements Filter {
    private final Tokens tokens;
    private final UsuarioRepo usuarios;

    public FiltroToken(Tokens tokens, UsuarioRepo usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String uri = http.getRequestURI();
        if (!uri.startsWith("/api/") || uri.startsWith("/api/auth/ingresar") || uri.startsWith("/api/salud")) {
            chain.doFilter(request, response);
            return;
        }
        String header = http.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            rechazar((HttpServletResponse) response);
            return;
        }
        Usuario usuario = tokens.leer(header.substring(7))
                .flatMap(usuarios::findById)
                .filter(u -> u.activo)
                .orElse(null);
        if (usuario == null) {
            rechazar((HttpServletResponse) response);
            return;
        }
        http.setAttribute("usuario", usuario);
        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"mensaje\":\"Inicia sesión para continuar.\"}");
    }

    public static Usuario actual(HttpServletRequest request) {
        return (Usuario) request.getAttribute("usuario");
    }
}
