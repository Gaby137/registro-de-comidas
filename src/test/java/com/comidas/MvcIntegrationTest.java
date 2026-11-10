package com.comidas;

import com.comidas.entity.Plato;
import com.comidas.entity.Registro;
import com.comidas.entity.Usuario;
import com.comidas.enums.Rol;
import com.comidas.repository.LinkCompartidoRepository;
import com.comidas.repository.PlatoRepository;
import com.comidas.repository.RegistroRepository;
import com.comidas.repository.UsuarioRepository;
import com.comidas.util.ZonaHoraria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre la app por HTTP: acceso, carga de una comida, edición, platos y link compartido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MvcIntegrationTest {

    static final RequestPostProcessor DUENO = user("dueno@test.com").roles("ADMIN");

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    PlatoRepository platoRepository;
    @Autowired
    RegistroRepository registroRepository;
    @Autowired
    LinkCompartidoRepository linkRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void crearDueno() {
        usuarioRepository.save(Usuario.builder()
                .nombre("Dueño").apellido("Test").email("dueno@test.com")
                .contrasena(passwordEncoder.encode("password123")).rol(Rol.ADMIN).build());
    }

    // === Acceso ===

    @Test
    void sinSesionTodoLlevaAlLogin() throws Exception {
        for (String ruta : new String[]{"/", "/semana", "/platos", "/link", "/seleccion-rapida", "/registro"}) {
            mvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
        }
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void elManifestDeLaPwaEsPublicoYSaleConSuTipoMime() throws Exception {
        mvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/manifest+json"))
                .andExpect(jsonPath("$.display").value("standalone"))
                .andExpect(jsonPath("$.start_url").value("/"))
                .andExpect(jsonPath("$.icons.length()").value(3));
        mvc.perform(get("/icons/icono-192.png")).andExpect(status().isOk());
        mvc.perform(get("/icons/icono-512.png")).andExpect(status().isOk());
        mvc.perform(get("/icons/icono-maskable-512.png")).andExpect(status().isOk());
        mvc.perform(get("/icons/apple-touch-icon.png")).andExpect(status().isOk());
    }

    @Test
    void noHayRegistroPublico() throws Exception {
        mvc.perform(get("/registro").with(DUENO)).andExpect(status().isNotFound());
        mvc.perform(get("/login")).andExpect(content().string(not(containsString("/registro"))));
    }

    @Test
    void loginDelDuenoVaALaPantallaPrincipalYQuedaRecordado() throws Exception {
        mvc.perform(formLogin("/login").user("email", "DUENO@test.com").password("contrasena", "password123"))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/"))
                // La app instalada tiene que abrir ya logueada
                .andExpect(cookie().exists("remember-me"))
                .andExpect(cookie().maxAge("remember-me", 180 * 24 * 60 * 60));
    }

    @Test
    void loginIncorrectoVuelveAlLoginConError() throws Exception {
        mvc.perform(formLogin("/login").user("email", "dueno@test.com").password("contrasena", "mala"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unaCuentaQueNoEsElDuenoNoEntra() throws Exception {
        mvc.perform(get("/").with(user("otro@test.com").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/platos").with(user("otro@test.com").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void elDuenoVeTodasSusPantallas() throws Exception {
        mvc.perform(get("/").with(DUENO)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Sacar foto")))
                .andExpect(content().string(containsString("capture=\"environment\"")))
                .andExpect(content().string(containsString("manifest.webmanifest")));
        mvc.perform(get("/semana").with(DUENO)).andExpect(status().isOk());
        mvc.perform(get("/platos").with(DUENO)).andExpect(status().isOk());
        mvc.perform(get("/platos/nuevo").with(DUENO)).andExpect(status().isOk());
        mvc.perform(get("/link").with(DUENO)).andExpect(status().isOk());
        mvc.perform(get("/login").with(DUENO)).andExpect(redirectedUrl("/"));
    }

    @Test
    void postSinCsrfEsRechazado() throws Exception {
        mvc.perform(post("/platos").with(DUENO).param("nombre", "Milanesa")).andExpect(status().isForbidden());
        mvc.perform(multipart("/registros").file(Fotos.parteJpeg(400, 300)).with(DUENO))
                .andExpect(status().isForbidden());
    }

    @Test
    void semanaConFechaInvalidaDevuelve400() throws Exception {
        mvc.perform(get("/semana").param("fecha", "ayer").with(DUENO)).andExpect(status().isBadRequest());
    }

    // === Cargar una comida ===

    @Test
    void cargarUnaComidaConUnPlatoExistente() throws Exception {
        Plato milanesa = platoRepository.save(
                Plato.builder().nombre("Milanesa con ensalada").descripcion("Una milanesa al horno").build());
        Instant capturadoEn = Instant.now().minusSeconds(5).truncatedTo(ChronoUnit.SECONDS);

        // Toque 1: la foto se sube sola y queda un registro pendiente con la hora de la captura
        UUID id = subirFoto(capturadoEn);
        Registro registro = registroRepository.findById(id).orElseThrow();
        assertThat(registro.getFechaHora()).isEqualTo(capturadoEn);
        assertThat(registro.getPlato()).isNull();

        // Mientras tanto, la selección rápida ofrece los platos
        mvc.perform(get("/seleccion-rapida").with(DUENO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Milanesa con ensalada")))
                .andExpect(content().string(containsString("data-plato-id=\"" + milanesa.getId() + "\"")));

        // Toque 2: elegir el plato
        mvc.perform(post("/registros/{id}/plato", id).param("platoId", milanesa.getId().toString())
                        .with(DUENO).with(csrf()))
                .andExpect(status().isOk());
        assertThat(registroRepository.findById(id).orElseThrow().getPlato().getId()).isEqualTo(milanesa.getId());

        // La comida aparece en el día de hoy
        mvc.perform(get("/").with(DUENO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Milanesa con ensalada")))
                .andExpect(content().string(containsString("/fotos/" + id + "/mini")));

        // Y sus fotos se pueden pedir con la sesión del dueño
        mvc.perform(get("/fotos/{id}", id).with(DUENO))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG));
        mvc.perform(get("/fotos/{id}/mini", id).with(DUENO))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void platoNuevoDesdeLaSeleccionRapidaQuedaParaLaProximaVez() throws Exception {
        UUID id = subirFoto(Instant.now());

        mvc.perform(post("/registros/{id}/plato-nuevo", id)
                        .param("nombre", "  Tarta de   verdura ").param("descripcion", "Dos porciones")
                        .with(DUENO).with(csrf()))
                .andExpect(status().isOk());

        Plato plato = registroRepository.findById(id).orElseThrow().getPlato();
        assertThat(plato.getNombre()).isEqualTo("Tarta de verdura");
        assertThat(plato.getDescripcion()).isEqualTo("Dos porciones");
        mvc.perform(get("/seleccion-rapida").with(DUENO))
                .andExpect(content().string(containsString("Tarta de verdura")));

        // Repetir el nombre es un error de negocio: vuelve como JSON para mostrarlo en el panel
        UUID otro = subirFoto(Instant.now());
        mvc.perform(post("/registros/{id}/plato-nuevo", otro).param("nombre", "tarta de verdura")
                        .with(DUENO).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tipo").value("ERROR"))
                .andExpect(jsonPath("$.mensaje").value(containsString("Ya existe")));
        mvc.perform(post("/registros/{id}/plato-nuevo", otro).param("nombre", " ").with(DUENO).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void guardarSinPlatoYCompletarloDespues() throws Exception {
        UUID id = subirFoto(Instant.now());

        // "Después": no se asigna nada y el registro queda marcado como pendiente
        mvc.perform(get("/").with(DUENO))
                .andExpect(content().string(containsString("Pendiente")))
                .andExpect(content().string(containsString("data-completar=\"" + id + "\"")));

        Plato yogur = platoRepository.save(Plato.builder().nombre("Yogur con granola").build());
        mvc.perform(post("/registros/{id}/plato", id).param("platoId", yogur.getId().toString())
                        .with(DUENO).with(csrf()))
                .andExpect(status().isOk());

        mvc.perform(get("/").with(DUENO))
                .andExpect(content().string(containsString("Yogur con granola")))
                .andExpect(content().string(not(containsString("data-completar"))));
    }

    @Test
    void unaFotoInvalidaNoCreaRegistro() throws Exception {
        MockMultipartFile png = new MockMultipartFile("foto", "foto.png", "image/png", Fotos.png(40, 40));

        mvc.perform(multipart("/registros").file(png).with(DUENO).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());

        assertThat(registroRepository.count()).isZero();
    }

    @Test
    void unaHoraDeCapturaAbsurdaSeReemplazaPorLaDelServidor() throws Exception {
        UUID futuro = subirFoto(Instant.now().plus(3, ChronoUnit.DAYS));
        UUID viejo = subirFoto(Instant.now().minus(60, ChronoUnit.DAYS));

        for (UUID id : new UUID[]{futuro, viejo}) {
            assertThat(registroRepository.findById(id).orElseThrow().getFechaHora())
                    .isBetween(Instant.now().minusSeconds(60), Instant.now().plusSeconds(1));
        }
    }

    // === Editar y borrar ===

    @Test
    void editarPlatoFechaYNotaYBorrar() throws Exception {
        Plato milanesa = platoRepository.save(Plato.builder().nombre("Milanesa").build());
        Plato ensalada = platoRepository.save(Plato.builder().nombre("Ensalada").build());
        UUID id = subirFoto(Instant.now());
        mvc.perform(post("/registros/{id}/plato", id).param("platoId", milanesa.getId().toString())
                .with(DUENO).with(csrf())).andExpect(status().isOk());

        mvc.perform(get("/registros/{id}/editar", id).with(DUENO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("type=\"datetime-local\"")))
                .andExpect(content().string(containsString("Ensalada")));

        // Tres días atrás a las 13:05, hora de Argentina
        LocalDateTime nueva = LocalDateTime.now(ZonaHoraria.AR).minusDays(3).withHour(13).withMinute(5)
                .truncatedTo(ChronoUnit.MINUTES);
        mvc.perform(post("/registros/{id}/editar", id).with(DUENO).with(csrf())
                        .param("platoId", ensalada.getId().toString())
                        .param("fechaHora", nueva.toString())
                        .param("nota", " comí la mitad "))
                .andExpect(redirectedUrl("/semana?fecha=" + nueva.toLocalDate()))
                .andExpect(flash().attributeExists("success"));

        Registro editado = registroRepository.findById(id).orElseThrow();
        assertThat(editado.getPlato().getId()).isEqualTo(ensalada.getId());
        assertThat(editado.getFechaHora()).isEqualTo(ZonaHoraria.instante(nueva));
        assertThat(editado.getNota()).isEqualTo("comí la mitad");

        // La vista semanal de esa fecha muestra hora, plato y nota
        mvc.perform(get("/semana").param("fecha", nueva.toLocalDate().toString()).with(DUENO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("13:05")))
                .andExpect(content().string(containsString("Ensalada")))
                .andExpect(content().string(containsString("comí la mitad")))
                .andExpect(content().string(containsString("/registros/" + id + "/editar")));

        // Una fecha futura se rechaza y vuelve al formulario con el mensaje
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/registros/{id}/editar", id).with(DUENO).with(csrf()).session(sesion)
                        .header("Referer", "http://localhost/registros/" + id + "/editar")
                        .param("fechaHora", LocalDateTime.now(ZonaHoraria.AR).plusDays(2)
                                .truncatedTo(ChronoUnit.MINUTES).toString()))
                .andExpect(redirectedUrl("/registros/" + id + "/editar"));
        mvc.perform(get("/registros/{id}/editar", id).with(DUENO).session(sesion))
                .andExpect(content().string(containsString("no pueden ser futuras")));
        assertThat(registroRepository.findById(id).orElseThrow().getFechaHora())
                .isEqualTo(ZonaHoraria.instante(nueva));

        // Borrar se lleva también la foto
        String fotoPath = editado.getFotoPath();
        mvc.perform(post("/registros/{id}/borrar", id).with(DUENO).with(csrf()))
                .andExpect(redirectedUrl("/semana?fecha=" + nueva.toLocalDate()));
        assertThat(registroRepository.findById(id)).isEmpty();
        assertThat(java.nio.file.Path.of("target/test-fotos", fotoPath)).doesNotExist();
        mvc.perform(get("/fotos/{id}", id).with(DUENO)).andExpect(status().isNotFound());
    }

    // === Platos ===

    @Test
    void crearEditarYArchivarPlatos() throws Exception {
        mvc.perform(post("/platos").with(DUENO).with(csrf())
                        .param("nombre", "Tostado").param("descripcion", "Jamón y queso"))
                .andExpect(redirectedUrl("/platos"));
        Plato plato = platoRepository.findAll().get(0);
        assertThat(plato.getNombre()).isEqualTo("Tostado");
        assertThat(plato.isArchivado()).isFalse();

        // Un registro viejo de ese plato
        UUID id = subirFoto(Instant.now());
        mvc.perform(post("/registros/{id}/plato", id).param("platoId", plato.getId().toString())
                .with(DUENO).with(csrf())).andExpect(status().isOk());

        // Editar la descripción cambia lo que se ve en sus registros
        mvc.perform(get("/platos/{id}/editar", plato.getId()).with(DUENO)).andExpect(status().isOk());
        mvc.perform(post("/platos/{id}/editar", plato.getId()).with(DUENO).with(csrf())
                        .param("nombre", "Tostado").param("descripcion", "Jamón, queso y tomate"))
                .andExpect(redirectedUrl("/platos"));
        mvc.perform(get("/semana").with(DUENO))
                .andExpect(content().string(containsString("Jamón, queso y tomate")));

        // Archivado: sale de la selección rápida, pero el registro lo sigue mostrando
        mvc.perform(post("/platos/{id}/archivar", plato.getId()).with(DUENO).with(csrf())
                        .param("archivado", "true"))
                .andExpect(redirectedUrl("/platos"));
        mvc.perform(get("/seleccion-rapida").with(DUENO))
                .andExpect(content().string(not(containsString("Tostado"))));
        mvc.perform(get("/platos").with(DUENO))
                .andExpect(content().string(containsString("Archivados")));
        mvc.perform(get("/semana").with(DUENO))
                .andExpect(content().string(containsString("Tostado")));
        mvc.perform(get("/registros/{id}/editar", id).with(DUENO))
                .andExpect(content().string(containsString("Tostado (archivado)")));

        // No existe forma de borrar un plato
        mvc.perform(post("/platos/{id}/borrar", plato.getId()).with(DUENO).with(csrf()))
                .andExpect(status().is4xxClientError());
        assertThat(platoRepository.findById(plato.getId())).isPresent();
    }

    // === Link compartido y vista de la nutricionista ===

    @Test
    void elLinkCompartidoAbreLaVistaSemanalSinLoginYDeSoloLectura() throws Exception {
        Plato milanesa = platoRepository.save(
                Plato.builder().nombre("Milanesa").descripcion("Al horno, con puré").build());
        UUID id = subirFoto(Instant.now());
        mvc.perform(post("/registros/{id}/plato", id).param("platoId", milanesa.getId().toString())
                .with(DUENO).with(csrf())).andExpect(status().isOk());

        mvc.perform(post("/link/generar").with(DUENO).with(csrf())).andExpect(redirectedUrl("/link"));
        String token = linkRepository.findByActivoTrue().get(0).getToken();
        mvc.perform(get("/link").with(DUENO))
                .andExpect(content().string(containsString("http://localhost/ver/" + token)));

        // Sin sesión: se ve la semana, con noindex y sin ningún control de edición
        mvc.perform(get("/ver/{token}", token).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", containsString("noindex")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(content().string(containsString("name=\"robots\" content=\"noindex")))
                .andExpect(content().string(containsString("Milanesa")))
                .andExpect(content().string(containsString("Al horno, con puré")))
                .andExpect(content().string(containsString("/ver/" + token + "/fotos/" + id + "/mini")))
                .andExpect(content().string(containsString("lunes")))
                .andExpect(content().string(containsString("domingo")))
                .andExpect(content().string(not(containsString("Editar"))))
                .andExpect(content().string(not(containsString("/registros/"))))
                .andExpect(content().string(not(containsString("<form method=\"post\""))))
                .andExpect(content().string(not(containsString("/logout"))));

        // Navegación entre semanas sin salir del link
        mvc.perform(get("/ver/{token}", token).param("fecha", "2026-01-07").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("5 al 11 de enero de 2026")))
                .andExpect(content().string(containsString("/ver/" + token + "?fecha=2025-12-29")));

        // Las fotos salen solo con el token
        mvc.perform(get("/ver/{token}/fotos/{id}", token, id).with(anonymous()))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mvc.perform(get("/ver/{token}/fotos/{id}/mini", token, id).with(anonymous()))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG));
        mvc.perform(get("/fotos/{id}", id).with(anonymous())).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/fotos/{id}/mini", id).with(anonymous())).andExpect(redirectedUrl("/login"));

        // Desde el link no se puede cambiar nada
        mvc.perform(post("/ver/{token}", token).with(anonymous())).andExpect(status().isForbidden());
        mvc.perform(post("/ver/{token}", token).with(anonymous()).with(csrf()))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/ver/{token}/fotos/{id}", token, id).with(anonymous()).with(csrf()))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/registros/{id}/borrar", id).with(anonymous()).with(csrf()))
                .andExpect(redirectedUrl("/login"));
        assertThat(registroRepository.findById(id)).isPresent();

        // Al revocar, la URL anterior deja de mostrar datos y fotos al instante
        mvc.perform(post("/link/revocar").with(DUENO).with(csrf())).andExpect(redirectedUrl("/link"));
        mvc.perform(get("/ver/{token}", token).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Milanesa"))));
        mvc.perform(get("/ver/{token}/fotos/{id}", token, id).with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/ver/{token}/fotos/{id}/mini", token, id).with(anonymous()))
                .andExpect(status().isNotFound());
    }

    @Test
    void generarUnLinkNuevoInvalidaElAnterior() throws Exception {
        mvc.perform(post("/link/generar").with(DUENO).with(csrf()));
        String viejo = linkRepository.findByActivoTrue().get(0).getToken();
        mvc.perform(post("/link/generar").with(DUENO).with(csrf()));
        String nuevo = linkRepository.findByActivoTrue().get(0).getToken();

        assertThat(nuevo).isNotEqualTo(viejo);
        mvc.perform(get("/ver/{token}", viejo).with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/ver/{token}", nuevo).with(anonymous())).andExpect(status().isOk());
    }

    @Test
    void unTokenInventadoDevuelve404() throws Exception {
        mvc.perform(get("/ver/{token}", "no-existe").with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/ver/{token}/fotos/{id}", "no-existe", UUID.randomUUID()).with(anonymous()))
                .andExpect(status().isNotFound());
    }

    private UUID subirFoto(Instant capturadoEn) throws Exception {
        String respuesta = mvc.perform(multipart("/registros").file(Fotos.parteJpeg(1600, 1200))
                        .param("capturadoEn", capturadoEn.toString())
                        .with(DUENO).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(respuesta.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
    }
}
