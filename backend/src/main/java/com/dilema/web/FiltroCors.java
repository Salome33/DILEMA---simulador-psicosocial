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
@Order(0)
public class FiltroCors implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse http = (HttpServletResponse) response;
        http.setHeader("Access-Control-Allow-Origin", "*");
        http.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        http.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        if ("OPTIONS".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {
            http.setStatus(204);
            return;
        }
        chain.doFilter(request, response);
    }
}
