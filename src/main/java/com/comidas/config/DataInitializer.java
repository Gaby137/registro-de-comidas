package com.comidas.config;

import com.comidas.entity.Usuario;
import com.comidas.enums.Rol;
import com.comidas.repository.UsuarioRepository;
import com.comidas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cuenta del dueño. No hay registro público: la cuenta se crea una sola vez al arrancar.
 * <ul>
 *   <li><b>Dev:</b> si la base está vacía, crea un dueño de prueba con contraseña conocida.</li>
 *   <li><b>Todos los perfiles:</b> si no existe ningún ADMIN y están definidas ADMIN_EMAIL y
 *       ADMIN_PASSWORD, lo crea con esos datos.</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    /** Corre primero: así, en dev, el chequeo del admin inicial ya encuentra al dueño de prueba. */
    @Bean
    @Order(1)
    @Profile("dev")
    public CommandLineRunner crearDuenoDemo() {
        return args -> {
            log.warn("Perfil DEV activo. En producción usá SPRING_PROFILES_ACTIVE=prod.");
            // Solo sobre una base vacía: nunca agrega una cuenta con contraseña conocida a una base con datos
            if (usuarioRepo.count() > 0) {
                return;
            }
            crear("Dueño", "Demo", "admin@app.com", "admin123");
            log.info("Dueño de prueba creado: admin@app.com / admin123");
        };
    }

    @Bean
    @Order(2)
    public CommandLineRunner crearAdminInicial(@Value("${app.admin.email:}") String email,
                                               @Value("${app.admin.password:}") String password) {
        return args -> {
            if (usuarioRepo.existsByRol(Rol.ADMIN)) {
                return;
            }
            if (email.isBlank() || password.isBlank()) {
                log.warn("No hay ninguna cuenta de dueño. Definí ADMIN_EMAIL y ADMIN_PASSWORD para crearla al arrancar.");
                return;
            }
            if (password.length() < 8) {
                throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 8 caracteres");
            }
            String emailNormalizado = UsuarioService.normalizarEmail(email);
            if (usuarioRepo.existsByEmail(emailNormalizado)) {
                log.warn("ADMIN_EMAIL ({}) ya existe con otro rol; no se creó el dueño. "
                        + "Usá otro email o cambiale el rol a mano en la base.", emailNormalizado);
                return;
            }
            crear("Dueño", "App", emailNormalizado, password);
            log.info("Cuenta del dueño creada: {}", emailNormalizado);
        };
    }

    private void crear(String nombre, String apellido, String email, String password) {
        usuarioRepo.save(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .email(UsuarioService.normalizarEmail(email))
                .contrasena(passwordEncoder.encode(password))
                .rol(Rol.ADMIN)
                .build());
    }
}
