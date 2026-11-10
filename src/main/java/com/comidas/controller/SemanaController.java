package com.comidas.controller;

import com.comidas.service.SemanaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Vista semanal del dueño: la misma que ve la nutricionista, con acceso a editar cada comida.
 */
@Controller
@RequiredArgsConstructor
public class SemanaController {

    private final SemanaService semanaService;

    @GetMapping("/semana")
    public String semana(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                         Model model) {
        model.addAttribute("semana", semanaService.armar(fecha));
        return "pages/semana";
    }
}
