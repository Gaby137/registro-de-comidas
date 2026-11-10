package com.comidas.controller.admin;

import com.comidas.enums.Rol;
import com.comidas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Listado de usuarios del panel admin (MVC). Copiá este controller como plantilla
 * para tus ABMs: listado con filtros + paginación, y acciones POST con redirect + flash.
 */
@Controller
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Rol rol,
                         @RequestParam(required = false) Boolean activo,
                         @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        model.addAttribute("pagina", usuarioService.buscar(q, rol, activo, pageable));
        model.addAttribute("q", q);
        model.addAttribute("rol", rol);
        model.addAttribute("activo", activo);
        model.addAttribute("roles", Rol.values());
        return "pages/admin/usuarios";
    }

    /** Patrón POST-Redirect-GET: la acción redirige al listado con un mensaje flash. */
    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam boolean activo,
                                @AuthenticationPrincipal UserDetails principal,
                                RedirectAttributes redirectAttributes) {
        var usuario = usuarioService.cambiarEstado(id, activo, principal.getUsername());
        redirectAttributes.addFlashAttribute("success",
                "Usuario " + usuario.getEmail() + (activo ? " activado." : " desactivado."));
        return "redirect:/admin/usuarios";
    }
}
