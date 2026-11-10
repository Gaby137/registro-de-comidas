package com.comidas.service;

import java.util.Locale;

/**
 * Utilidades de la cuenta del dueño. No hay registro ni administración de usuarios:
 * la cuenta se crea al arrancar (ver DataInitializer).
 */
public final class UsuarioService {

    private UsuarioService() {}

    /** Emails siempre en minúsculas y sin espacios, para que "Juan@Mail.com" y "juan@mail.com" sean el mismo. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
