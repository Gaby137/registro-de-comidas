package com.comidas.controller;

import com.comidas.service.LinkCompartidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Link compartido con la nutricionista: verlo, copiarlo, revocarlo y generar uno nuevo.
 */
@Controller
@RequestMapping("/link")
@RequiredArgsConstructor
public class LinkController {

    private final LinkCompartidoService linkService;

    @GetMapping
    public String ver(Model model) {
        linkService.actual().ifPresent(link -> model.addAttribute("url",
                // Respeta X-Forwarded-* del proxy: en producción arma la URL https del dominio real
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/ver/{token}").buildAndExpand(link.getToken()).toUriString()));
        return "pages/link";
    }

    @PostMapping("/generar")
    public String generar(RedirectAttributes redirect) {
        boolean habiaLink = linkService.actual().isPresent();
        linkService.generar();
        redirect.addFlashAttribute("success", habiaLink
                ? "Link nuevo generado. El anterior ya no funciona."
                : "Link generado.");
        return "redirect:/link";
    }

    @PostMapping("/revocar")
    public String revocar(RedirectAttributes redirect) {
        linkService.revocar();
        redirect.addFlashAttribute("success", "Link revocado. Ya no muestra datos ni fotos.");
        return "redirect:/link";
    }
}
