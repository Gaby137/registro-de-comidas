package com.comidas.controller.admin;

import com.comidas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsuarios", usuarioService.contarTodos());
        model.addAttribute("usuariosActivos", usuarioService.contarActivos());
        return "pages/admin/dashboard";
    }
}
