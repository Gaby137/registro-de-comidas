package com.comidas.controller;

import com.comidas.dto.RegistroForm;
import com.comidas.entity.Plato;
import com.comidas.entity.Registro;
import com.comidas.service.PlatoService;
import com.comidas.service.RegistroService;
import com.comidas.util.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Edición y borrado de un registro (formularios del dueño).
 * Las reglas de negocio que fallan vuelven al formulario con el mensaje (GlobalExceptionHandler).
 */
@Controller
@RequestMapping("/registros/{id}")
@RequiredArgsConstructor
public class RegistroController {

    private final RegistroService registroService;
    private final PlatoService platoService;

    @GetMapping("/editar")
    public String editar(@PathVariable UUID id, Model model) {
        Registro registro = registroService.obtener(id);

        RegistroForm form = new RegistroForm();
        form.setPlatoId(registro.getPlato() != null ? registro.getPlato().getId() : null);
        form.setFechaHora(ZonaHoraria.local(registro.getFechaHora()).truncatedTo(ChronoUnit.MINUTES));
        form.setNota(registro.getNota());

        // Si el registro tiene un plato archivado, tiene que seguir estando en el selector
        List<Plato> platos = new ArrayList<>(platoService.listarActivos());
        if (registro.getPlato() != null && registro.getPlato().isArchivado()) {
            platos.add(registro.getPlato());
        }

        model.addAttribute("registro", registroService.obtenerVista(id));
        model.addAttribute("registroForm", form);
        model.addAttribute("platos", platos);
        return "pages/registro-form";
    }

    @PostMapping("/editar")
    public String guardar(@PathVariable UUID id, @ModelAttribute RegistroForm registroForm,
                          RedirectAttributes redirect) {
        Registro registro = registroService.actualizar(
                id, registroForm.getPlatoId(), registroForm.getFechaHora(), registroForm.getNota());
        redirect.addFlashAttribute("success", "Comida actualizada.");
        return "redirect:/semana?fecha=" + fechaDe(registro);
    }

    @PostMapping("/borrar")
    public String borrar(@PathVariable UUID id, RedirectAttributes redirect) {
        LocalDate fecha = fechaDe(registroService.obtener(id));
        registroService.borrar(id);
        redirect.addFlashAttribute("success", "Comida borrada.");
        return "redirect:/semana?fecha=" + fecha;
    }

    private static LocalDate fechaDe(Registro registro) {
        return ZonaHoraria.local(registro.getFechaHora()).toLocalDate();
    }
}
