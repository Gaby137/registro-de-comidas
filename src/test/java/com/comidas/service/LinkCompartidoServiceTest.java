package com.comidas.service;

import com.comidas.entity.LinkCompartido;
import com.comidas.exception.ResourceNotFoundException;
import com.comidas.repository.LinkCompartidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LinkCompartidoServiceTest {

    @Autowired
    LinkCompartidoService linkService;
    @Autowired
    LinkCompartidoRepository linkRepository;

    @Test
    void sinLinkNoHayNadaValido() {
        assertThat(linkService.actual()).isEmpty();
        assertThat(linkService.esValido("cualquiera")).isFalse();
        assertThat(linkService.esValido("")).isFalse();
        assertThat(linkService.esValido(null)).isFalse();
    }

    @Test
    void generarUnoNuevoDejaSinEfectoAlAnterior() {
        LinkCompartido primero = linkService.generar();
        assertThat(primero.getToken()).hasSizeGreaterThanOrEqualTo(32).matches("[A-Za-z0-9_-]+");
        assertThat(linkService.esValido(primero.getToken())).isTrue();

        LinkCompartido segundo = linkService.generar();

        assertThat(segundo.getToken()).isNotEqualTo(primero.getToken());
        assertThat(linkService.esValido(primero.getToken())).isFalse();
        assertThat(linkService.esValido(segundo.getToken())).isTrue();
        assertThat(linkService.actual()).map(LinkCompartido::getToken).contains(segundo.getToken());
        // Solo un link activo a la vez
        assertThat(linkRepository.findByActivoTrue()).hasSize(1);
    }

    @Test
    void revocarDejaAlLinkSinEfecto() {
        String token = linkService.generar().getToken();

        linkService.revocar();

        assertThat(linkService.esValido(token)).isFalse();
        assertThat(linkService.actual()).isEmpty();
        assertThatThrownBy(() -> linkService.exigirValido(token)).isInstanceOf(ResourceNotFoundException.class);
    }
}
