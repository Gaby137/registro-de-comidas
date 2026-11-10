package com.comidas;

import com.comidas.entity.Usuario;
import com.comidas.enums.Rol;
import com.comidas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la capa MVC: páginas públicas, login por formulario, CSRF y acceso al panel admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MvcIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void crearUsuarios() {
        usuarioRepository.save(Usuario.builder()
                .nombre("Admin").apellido("Test").email("admin@test.com")
                .contrasena(passwordEncoder.encode("password123")).rol(Rol.ADMIN).build());
        usuarioRepository.save(Usuario.builder()
                .nombre("User").apellido("Test").email("user@test.com")
                .contrasena(passwordEncoder.encode("password123")).rol(Rol.USER).build());
    }

    @Test
    void paginasPublicasRenderizan() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Comidas")));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/registro")).andExpect(status().isOk());
    }

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void loginAdminRedirigeAlPanel() throws Exception {
        mvc.perform(formLogin("/login").user("email", "ADMIN@test.com").password("contrasena", "password123"))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginUsuarioRedirigeAlHome() throws Exception {
        mvc.perform(formLogin("/login").user("email", "user@test.com").password("contrasena", "password123"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginIncorrectoVuelveAlLoginConError() throws Exception {
        mvc.perform(formLogin("/login").user("email", "user@test.com").password("contrasena", "mala"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void adminRequiereLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginVuelveALaPaginaPedidaOriginalmente() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/admin/usuarios").session(session)).andExpect(redirectedUrl("/login"));

        mvc.perform(post("/login").session(session).with(csrf())
                        .param("email", "admin@test.com").param("contrasena", "password123"))
                .andExpect(redirectedUrl("http://localhost/admin/usuarios?continue"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void usuarioComunRecibe403EnAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void adminVeDashboardYListado() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalUsuarios", 2L));
        mvc.perform(get("/admin/usuarios").param("q", "user"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("user@test.com")));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void adminDesactivaUsuarioConPostRedirectGet() throws Exception {
        Long id = usuarioRepository.findByEmail("user@test.com").orElseThrow().getId();
        mvc.perform(post("/admin/usuarios/{id}/estado", id).param("activo", "false").with(csrf()))
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void parametroInvalidoEnVistaDevuelve400() throws Exception {
        mvc.perform(get("/admin/usuarios").param("rol", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/usuarios").param("sort", "noExiste"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postSinCsrfEsRechazado() throws Exception {
        mvc.perform(post("/registro").param("nombre", "Juan"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroExitosoRedirigeAlLogin() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Juan").param("apellido", "Pérez")
                        .param("email", "juan@mail.com").param("contrasena", "secreta123"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void registroConErroresVuelveAlFormulario() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "").param("apellido", "P")
                        .param("email", "mal").param("contrasena", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/registro"))
                .andExpect(model().attributeHasFieldErrors("registroRequest", "nombre", "email", "contrasena"));
    }

    @Test
    void registroConEmailDuplicadoMuestraError() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Otro").param("apellido", "User")
                        .param("email", "user@test.com").param("contrasena", "secreta123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }
}
