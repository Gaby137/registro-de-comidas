package com.comidas.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Login del dueño. No hay registro público.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String mostrarLogin(Authentication authentication) {
        // Con sesión abierta (o cookie remember-me válida) no tiene sentido mostrar el login
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "pages/login";
    }
}
