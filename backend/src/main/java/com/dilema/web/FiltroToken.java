package com.dilema.web;

import com.dilema.modelo.Usuario;
import com.dilema.repo.UsuarioRepo;
import com.dilema.servicio.Tokens;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Lee el Bearer que guarda Angular y deja al usuario en el contexto de Spring Security. */
public class FiltroToken extends OncePerRequestFilter {
    private final Tokens tokens;
    private final UsuarioRepo usuarios;

    public FiltroToken(Tokens tokens, UsuarioRepo usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/") || uri.startsWith("/api/auth/ingresar") || uri.startsWith("/api/salud")
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            rechazar(response);
            return;
        }
        Usuario usuario = tokens.leer(header.substring(7))
                .flatMap(usuarios::findById)
                .filter(u -> u.activo)
                .orElse(null);
        if (usuario == null) {
            rechazar(response);
            return;
        }
        var autenticacion = new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol.name())));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
        request.setAttribute("usuario", usuario);
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
