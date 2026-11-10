package com.comidas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * Configuración de seguridad MVC: form login + sesiones + CSRF.
 * Para proteger rutas nuevas: sumá requestMatchers en la cadena
 * o usá @PreAuthorize en el controller (method security habilitado).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain mvcSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos y health check
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/uploads/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Páginas públicas
                        .requestMatchers("/", "/error").permitAll()
                        .requestMatchers("/login", "/registro").permitAll()
                        // Panel admin
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("contrasena")
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Después del login: vuelve a la página protegida que se pidió originalmente;
     * si no había ninguna, admins al panel y usuarios al home.
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        RequestCache requestCache = new HttpSessionRequestCache();
        return (request, response, authentication) -> {
            SavedRequest saved = requestCache.getRequest(request, response);
            if (saved != null) {
                requestCache.removeRequest(request, response);
                response.sendRedirect(saved.getRedirectUrl());
                return;
            }
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            response.sendRedirect(request.getContextPath() + (isAdmin ? "/admin" : "/"));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
