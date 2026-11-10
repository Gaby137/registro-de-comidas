package com.comidas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la app: un solo dueño (rol ADMIN) con form login, sesión, CSRF y remember-me.
 * <p>
 * Lo único público es el login, los estáticos y la vista compartida de solo lectura
 * ({@code GET /ver/<token>}), que valida su token en el controller.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** La app instalada en el celular tiene que abrir ya logueada: la cookie dura 180 días. */
    private static final int REMEMBER_ME_SEGUNDOS = 180 * 24 * 60 * 60;
    private static final int REMEMBER_ME_KEY_MINIMO = 32;

    @Bean
    public SecurityFilterChain mvcSecurityFilterChain(HttpSecurity http,
                                                      UserDetailsService userDetailsService,
                                                      @Value("${app.remember-me.key:}") String rememberMeKey)
            throws Exception {
        if (rememberMeKey.length() < REMEMBER_ME_KEY_MINIMO) {
            throw new IllegalStateException("app.remember-me.key (REMEMBER_ME_KEY) debe tener al menos "
                    + REMEMBER_ME_KEY_MINIMO + " caracteres");
        }

        http
                .authorizeHttpRequests(auth -> auth
                        // Estáticos, manifest de la PWA y health check
                        .requestMatchers("/css/**", "/js/**", "/icons/**", "/manifest.webmanifest", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        // Vista de la nutricionista: solo lectura, el token se valida en VerController
                        .requestMatchers(HttpMethod.GET, "/ver/**").permitAll()
                        // Todo lo demás es del dueño
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("contrasena")
                        .defaultSuccessUrl("/")
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .userDetailsService(userDetailsService)
                        .alwaysRemember(true)
                        .tokenValiditySeconds(REMEMBER_ME_SEGUNDOS)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
