package com.comidas.controller;

import com.comidas.entity.Plato;
import com.comidas.service.PlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Administración de platos: crear, editar y archivar. No hay borrado.
 */
@Controller
@RequestMapping("/platos")
@RequiredArgsConstructor
public class PlatoController {

    private final PlatoService platoService;

    @GetMapping
    public String listar(Model model) {
        List<Plato> todos = platoService.listarTodos();
        model.addAttribute("activos", todos.stream().filter(p -> !p.isArchivado()).toList());
        model.addAttribute("archivados", todos.stream().filter(Plato::isArchivado).toList());
        return "pages/platos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("plato", new Plato());
        return "pages/plato-form";
    }

    @PostMapping
    public String crear(@RequestParam String nombre, @RequestParam(required = false) String descripcion,
                        RedirectAttributes redirect) {
        platoService.crear(nombre, descripcion);
        redirect.addFlashAttribute("success", "Plato creado.");
        return "redirect:/platos";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable UUID id, Model model) {
        model.addAttribute("plato", platoService.obtener(id));
        return "pages/plato-form";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable UUID id, @RequestParam String nombre,
                             @RequestParam(required = false) String descripcion, RedirectAttributes redirect) {
        platoService.actualizar(id, nombre, descripcion);
        redirect.addFlashAttribute("success", "Plato actualizado.");
        return "redirect:/platos";
    }

    @PostMapping("/{id}/archivar")
    public String archivar(@PathVariable UUID id, @RequestParam boolean archivado, RedirectAttributes redirect) {
        Plato plato = platoService.cambiarArchivado(id, archivado);
        redirect.addFlashAttribute("success", archivado
                ? "\"" + plato.getNombre() + "\" archivado. Sus registros lo siguen mostrando."
                : "\"" + plato.getNombre() + "\" volvió a la selección rápida.");
        return "redirect:/platos";
    }
}
