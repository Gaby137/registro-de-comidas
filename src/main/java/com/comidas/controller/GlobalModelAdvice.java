package com.comidas.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Agrega variables globales al modelo para todas las vistas Thymeleaf.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @Value("${app.nombre}")
    private String appNombre;

    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /** Nombre de la app (app.nombre en application.properties), usado en header, footer y títulos. */
    @ModelAttribute("appNombre")
    public String getAppNombre() {
        return appNombre;
    }
}
