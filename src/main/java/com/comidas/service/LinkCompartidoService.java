package com.comidas.service;

import com.comidas.entity.LinkCompartido;
import com.comidas.exception.ResourceNotFoundException;
import com.comidas.repository.LinkCompartidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Link compartido con la nutricionista. Solo hay uno activo a la vez:
 * generar uno nuevo o revocar deja al anterior sin efecto al instante.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkCompartidoService {

    /** 32 bytes aleatorios = 43 caracteres en base64url. */
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LinkCompartidoRepository linkRepository;

    public Optional<LinkCompartido> actual() {
        return linkRepository.findFirstByActivoTrueOrderByCreadoEnDesc();
    }

    @Transactional
    public LinkCompartido generar() {
        desactivarActivos();
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        LinkCompartido link = linkRepository.save(LinkCompartido.builder()
                .token(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
                .build());
        log.info("Link compartido generado");
        return link;
    }

    @Transactional
    public void revocar() {
        desactivarActivos();
        log.info("Link compartido revocado");
    }

    public boolean esValido(String token) {
        return token != null && !token.isBlank() && linkRepository.existsByTokenAndActivoTrue(token);
    }

    /** Un token inexistente y uno revocado responden lo mismo: 404. */
    public void exigirValido(String token) {
        if (!esValido(token)) {
            throw new ResourceNotFoundException("Este link no está disponible.");
        }
    }

    private void desactivarActivos() {
        linkRepository.findByActivoTrue().forEach(link -> link.setActivo(false));
        // Antes de insertar el nuevo: que nunca haya dos activos, ni dentro de la transacción
        linkRepository.flush();
    }
}
