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
 * Datos iniciales.
 * <ul>
 *   <li><b>Datos demo (solo dev):</b> admin y usuario de prueba con contraseñas conocidas,
 *       únicamente si la base está vacía.</li>
 *   <li><b>Admin inicial (todos los perfiles):</b> si no existe ningún ADMIN y están definidas
 *       ADMIN_EMAIL y ADMIN_PASSWORD, lo crea. Así un deploy nuevo tiene con qué entrar al panel.</li>
 * </ul>
 * Los datos de catálogo (que deben existir en todos los entornos) van en migraciones Flyway, no acá.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    /** Corre primero: así, en dev, el chequeo del admin inicial ya encuentra al admin demo. */
    @Bean
    @Order(1)
    @Profile("dev")
    public CommandLineRunner crearDatosDemo() {
        return args -> {
            log.warn("Perfil DEV activo (usuarios demo). "
                    + "En producción usá SPRING_PROFILES_ACTIVE=prod.");
            // Solo sobre una base vacía: nunca agrega cuentas con contraseña conocida a una base con datos
            if (usuarioRepo.count() > 0) {
                return;
            }
            crear("Admin", "Demo", "admin@app.com", "admin123", Rol.ADMIN);
            crear("Usuario", "Demo", "usuario@app.com", "user1234", Rol.USER);
            log.info("Usuarios demo creados: admin@app.com / admin123 y usuario@app.com / user1234");
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
                log.warn("No hay ningún usuario ADMIN. Definí ADMIN_EMAIL y ADMIN_PASSWORD para crearlo al arrancar.");
                return;
            }
            if (password.length() < 8) {
                throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 8 caracteres");
            }
            String emailNormalizado = UsuarioService.normalizarEmail(email);
            // Nunca se promueve una cuenta existente: alguien podría haberse registrado con ese email
            if (usuarioRepo.existsByEmail(emailNormalizado)) {
                log.warn("ADMIN_EMAIL ({}) ya está registrado como usuario común; no se creó el admin. "
                        + "Usá otro email o cambiale el rol a mano en la base.", emailNormalizado);
                return;
            }
            crear("Admin", "Sistema", emailNormalizado, password, Rol.ADMIN);
            log.info("Admin inicial creado: {}", emailNormalizado);
        };
    }

    private void crear(String nombre, String apellido, String email, String password, Rol rol) {
        usuarioRepo.save(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .email(UsuarioService.normalizarEmail(email))
                .contrasena(passwordEncoder.encode(password))
                .rol(rol)
                .build());
    }
}
