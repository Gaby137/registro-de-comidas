/*
 * Utilidades compartidas por las pantallas del dueño.
 *
 * enviar(url, opciones): fetch con el token CSRF del layout. La app instalada puede quedar
 * abierta más tiempo que la sesión; si el servidor responde 403, pide un token nuevo
 * (GET /csrf, que la cookie remember-me vuelve a autenticar) y reintenta una vez.
 */
(function () {
    'use strict';

    function leerMeta(nombre) {
        const meta = document.querySelector('meta[name="' + nombre + '"]');
        return meta ? meta.content : null;
    }

    let csrf = { header: leerMeta('_csrf_header'), token: leerMeta('_csrf') };

    async function renovarCsrf() {
        const respuesta = await fetch('/csrf', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
        if (respuesta.redirected || !respuesta.ok) {
            // Sin sesión ni cookie remember-me válida: hay que volver a entrar
            window.location.href = '/login';
            throw new Error('La sesión venció. Volvé a iniciar sesión.');
        }
        csrf = await respuesta.json();
    }

    function conCsrf(opciones) {
        const headers = new Headers(opciones.headers || {});
        headers.set('Accept', 'application/json');
        if (csrf.header && csrf.token) {
            headers.set(csrf.header, csrf.token);
        }
        return Object.assign({}, opciones, { headers: headers, credentials: 'same-origin' });
    }

    async function enviar(url, opciones) {
        opciones = opciones || {};
        let respuesta = await fetch(url, conCsrf(opciones));
        if (respuesta.status === 403) {
            await renovarCsrf();
            respuesta = await fetch(url, conCsrf(opciones));
        }
        // Sesión vencida sin remember-me: el servidor redirige al login
        if (respuesta.redirected && new URL(respuesta.url).pathname === '/login') {
            window.location.href = '/login';
            throw new Error('Sesión vencida');
        }
        return respuesta;
    }

    /** Mensaje de error apto para mostrar, a partir de una respuesta con MensajeDTO. */
    async function mensajeDeError(respuesta, porDefecto) {
        try {
            const cuerpo = await respuesta.clone().json();
            if (cuerpo && cuerpo.mensaje) {
                return cuerpo.mensaje;
            }
        } catch (e) {
            // La respuesta no era JSON (página de error): queda el mensaje por defecto
        }
        return porDefecto;
    }

    window.Comidas = { enviar: enviar, mensajeDeError: mensajeDeError };
})();
