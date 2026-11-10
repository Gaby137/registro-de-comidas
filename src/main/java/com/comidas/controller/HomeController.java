package com.comidas.controller;

import com.comidas.service.RegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantalla principal del dueño: botón "Sacar foto", comidas de hoy y pendientes de días anteriores.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final RegistroService registroService;

    @GetMapping("/")
    public String hoy(Model model) {
        model.addAttribute("registrosHoy", registroService.deHoy());
        model.addAttribute("pendientes", registroService.pendientesAnteriores());
        return "pages/hoy";
    }
}
