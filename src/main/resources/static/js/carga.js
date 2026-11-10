/*
 * Cargar una comida en dos toques: sacar la foto y elegir el plato.
 *
 * 1. "Sacar foto" abre la cámara (input file con capture).
 * 2. Al confirmar la foto se comprime acá mismo y empieza a subirse sola.
 * 3. Mientras sube aparece la selección rápida. Lo que se elija queda en cola
 *    y se aplica cuando la subida termina.
 * 4. Si la subida falla se puede reintentar sin volver a sacar la foto.
 *
 * El mismo panel sirve para completar un registro pendiente (ya tiene foto subida).
 */
(function () {
    'use strict';

    const LADO_MAYOR = 1600;
    const CALIDAD_JPEG = 0.8;

    const btnFoto = document.getElementById('btnFoto');
    const inputFoto = document.getElementById('inputFoto');
    const panel = document.getElementById('panel');
    const panelFoto = document.getElementById('panelFoto');
    const panelEstado = document.getElementById('panelEstado');
    const btnReintentar = document.getElementById('btnReintentar');
    const vistaLista = document.getElementById('vistaLista');
    const buscador = document.getElementById('buscador');
    const listaPlatos = document.getElementById('listaPlatos');
    const formNuevo = document.getElementById('formNuevo');
    const nuevoNombre = document.getElementById('nuevoNombre');
    const nuevoDescripcion = document.getElementById('nuevoDescripcion');
    const nuevoError = document.getElementById('nuevoError');
    const btnNuevo = document.getElementById('btnNuevo');
    const btnDespues = document.getElementById('btnDespues');
    const btnCancelarNuevo = document.getElementById('btnCancelarNuevo');

    // Estado de la carga en curso
    let foto = null;          // Blob ya comprimido (se conserva para reintentar)
    let capturadoEn = null;   // momento en que se confirmó la foto
    let registroId = null;    // id del registro una vez subida la foto
    let subiendo = false;
    let eleccion = null;      // { tipo: 'existente' | 'nuevo' | 'despues', ... } en cola
    let guardando = false;
    let urlPrevia = null;

    // === Paso 1: sacar la foto ===

    btnFoto.addEventListener('click', function () {
        inputFoto.click();
    });

    inputFoto.addEventListener('change', async function () {
        const archivo = inputFoto.files && inputFoto.files[0];
        inputFoto.value = '';
        if (!archivo) {
            return;
        }
        capturadoEn = new Date().toISOString();
        abrirPanel(URL.createObjectURL(archivo), true);
        cargarPlatos();
        mostrarEstado('Preparando la foto…');
        try {
            foto = await comprimir(archivo);
        } catch (e) {
            mostrarEstado('No se pudo leer la foto. Cerrá y sacala de nuevo.', 'error');
            return;
        }
        subir();
    });

    // === Completar un registro pendiente ===

    document.querySelectorAll('[data-completar]').forEach(function (boton) {
        boton.addEventListener('click', function () {
            abrirPanel(boton.dataset.mini, false);
            registroId = boton.dataset.completar;
            mostrarEstado('Elegí el plato de esta comida.');
            cargarPlatos();
        });
    });

    // === Compresión en el celular: lado mayor 1600 px, JPEG ~80 % ===

    async function comprimir(archivo) {
        const imagen = await decodificar(archivo);
        const ancho = imagen.width || imagen.naturalWidth;
        const alto = imagen.height || imagen.naturalHeight;
        const escala = Math.min(1, LADO_MAYOR / Math.max(ancho, alto));
        const lienzo = document.createElement('canvas');
        lienzo.width = Math.max(1, Math.round(ancho * escala));
        lienzo.height = Math.max(1, Math.round(alto * escala));
        lienzo.getContext('2d').drawImage(imagen, 0, 0, lienzo.width, lienzo.height);
        if (imagen.close) {
            imagen.close();
        }
        return new Promise(function (resolver, rechazar) {
            lienzo.toBlob(function (blob) {
                if (blob) {
                    resolver(blob);
                } else {
                    rechazar(new Error('No se pudo comprimir'));
                }
            }, 'image/jpeg', CALIDAD_JPEG);
        });
    }

    /** Decodifica respetando la orientación EXIF (la foto no queda acostada). */
    async function decodificar(archivo) {
        if (window.createImageBitmap) {
            try {
                return await createImageBitmap(archivo, { imageOrientation: 'from-image' });
            } catch (e) {
                // Navegador sin soporte para esa opción o ese formato: se prueba con <img>
            }
        }
        return new Promise(function (resolver, rechazar) {
            const url = URL.createObjectURL(archivo);
            const img = new Image();
            img.onload = function () {
                URL.revokeObjectURL(url);
                resolver(img);
            };
            img.onerror = function () {
                URL.revokeObjectURL(url);
                rechazar(new Error('Imagen ilegible'));
            };
            img.src = url;
        });
    }

    // === Paso 2: subida automática ===

    async function subir() {
        subiendo = true;
        btnReintentar.hidden = true;
        mostrarEstado('Subiendo la foto…');
        try {
            const datos = new FormData();
            datos.append('foto', foto, 'foto.jpg');
            datos.append('capturadoEn', capturadoEn);
            const respuesta = await Comidas.enviar('/registros', { method: 'POST', body: datos });
            if (!respuesta.ok) {
                throw new Error(await Comidas.mensajeDeError(respuesta, 'No se pudo subir la foto.'));
            }
            registroId = (await respuesta.json()).id;
            subiendo = false;
            mostrarEstado('Foto guardada.', 'listo');
            aplicarEleccion();
        } catch (e) {
            subiendo = false;
            const sinRed = e instanceof TypeError;
            mostrarEstado(sinRed ? 'No se pudo subir la foto. Revisá la conexión.' : e.message, 'error');
            btnReintentar.hidden = false;
        }
    }

    btnReintentar.addEventListener('click', function () {
        if (foto && !subiendo) {
            subir();
        }
    });

    // === Paso 3: selección rápida ===

    async function cargarPlatos() {
        listaPlatos.innerHTML = '<p class="ayuda mt-sm">Cargando platos…</p>';
        try {
            const respuesta = await fetch('/seleccion-rapida', { credentials: 'same-origin' });
            if (!respuesta.ok) {
                throw new Error();
            }
            // HTML generado por el servidor (Thymeleaf escapa los nombres)
            listaPlatos.innerHTML = await respuesta.text();
            filtrar();
        } catch (e) {
            listaPlatos.innerHTML = '<p class="alert alert-error mt-sm">No se pudieron cargar los platos. '
                + 'Podés tocar <strong>Después</strong> y completarlo más tarde.</p>';
        }
    }

    listaPlatos.addEventListener('click', function (evento) {
        const boton = evento.target.closest('.plato-btn');
        if (!boton || guardando) {
            return;
        }
        listaPlatos.querySelectorAll('.plato-btn.elegido').forEach(function (b) {
            b.classList.remove('elegido');
        });
        boton.classList.add('elegido');
        elegir({ tipo: 'existente', platoId: boton.dataset.platoId, nombre: boton.dataset.nombre });
    });

    buscador.addEventListener('input', filtrar);

    function normalizar(texto) {
        return texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    }

    function filtrar() {
        const buscado = normalizar(buscador.value.trim());
        listaPlatos.querySelectorAll('.plato-btn').forEach(function (boton) {
            boton.hidden = buscado !== '' && normalizar(boton.dataset.nombre).indexOf(buscado) === -1;
        });
    }

    // Plato nuevo: nombre y descripción corta
    btnNuevo.addEventListener('click', function () {
        vistaLista.hidden = true;
        formNuevo.hidden = false;
        nuevoError.hidden = true;
        nuevoNombre.value = buscador.value.trim();
        nuevoNombre.focus();
    });

    btnCancelarNuevo.addEventListener('click', mostrarLista);

    function mostrarLista() {
        formNuevo.hidden = true;
        vistaLista.hidden = false;
    }

    formNuevo.addEventListener('submit', function (evento) {
        evento.preventDefault();
        const nombre = nuevoNombre.value.trim();
        if (!nombre || guardando) {
            return;
        }
        nuevoError.hidden = true;
        elegir({ tipo: 'nuevo', nombre: nombre, descripcion: nuevoDescripcion.value.trim() });
    });

    // Después: el registro queda guardado sin plato, como pendiente
    btnDespues.addEventListener('click', function () {
        if (guardando) {
            return;
        }
        elegir({ tipo: 'despues' });
    });

    function elegir(nueva) {
        eleccion = nueva;
        if (registroId) {
            aplicarEleccion();
        } else if (subiendo) {
            mostrarEstado('Subiendo la foto… se guarda apenas termine.');
        } else if (nueva.tipo === 'despues') {
            // La subida falló y no se quiere reintentar: no queda nada guardado
            cerrarPanel();
        }
    }

    // === Paso 4: guardar el plato elegido ===

    async function aplicarEleccion() {
        if (!eleccion || !registroId || guardando) {
            return;
        }
        const actual = eleccion;
        if (actual.tipo === 'despues') {
            terminar();
            return;
        }
        guardando = true;
        bloquear(true);
        mostrarEstado('Guardando…');
        try {
            const datos = new URLSearchParams();
            let url = '/registros/' + registroId;
            if (actual.tipo === 'existente') {
                url += '/plato';
                datos.set('platoId', actual.platoId);
            } else {
                url += '/plato-nuevo';
                datos.set('nombre', actual.nombre);
                datos.set('descripcion', actual.descripcion);
            }
            const respuesta = await Comidas.enviar(url, { method: 'POST', body: datos });
            if (!respuesta.ok) {
                throw new Error(await Comidas.mensajeDeError(respuesta, 'No se pudo guardar el plato.'));
            }
            terminar();
        } catch (e) {
            guardando = false;
            eleccion = null;
            bloquear(false);
            const mensaje = e instanceof TypeError ? 'No se pudo guardar. Revisá la conexión y probá de nuevo.' : e.message;
            if (actual.tipo === 'nuevo') {
                nuevoError.textContent = mensaje;
                nuevoError.hidden = false;
                mostrarEstado('Foto guardada.', 'listo');
            } else {
                mostrarEstado(mensaje, 'error');
            }
            listaPlatos.querySelectorAll('.plato-btn.elegido').forEach(function (b) {
                b.classList.remove('elegido');
            });
        }
    }

    /** Vuelve a la pantalla principal: la comida recién cargada aparece en el día de hoy. */
    function terminar() {
        window.location.href = '/';
    }

    // === Panel ===

    function abrirPanel(urlFoto, esObjectUrl) {
        foto = null;
        registroId = null;
        eleccion = null;
        guardando = false;
        subiendo = false;
        liberarPrevia();
        if (esObjectUrl) {
            urlPrevia = urlFoto;
        }
        panelFoto.src = urlFoto;
        buscador.value = '';
        nuevoNombre.value = '';
        nuevoDescripcion.value = '';
        btnReintentar.hidden = true;
        bloquear(false);
        mostrarLista();
        panel.hidden = false;
        document.body.style.overflow = 'hidden';
    }

    function cerrarPanel() {
        panel.hidden = true;
        document.body.style.overflow = '';
        liberarPrevia();
    }

    function liberarPrevia() {
        if (urlPrevia) {
            URL.revokeObjectURL(urlPrevia);
            urlPrevia = null;
        }
    }

    function bloquear(bloqueado) {
        panel.querySelectorAll('.plato-btn, .panel-pie .btn, #formNuevo button').forEach(function (b) {
            b.disabled = bloqueado;
        });
    }

    function mostrarEstado(texto, tipo) {
        panelEstado.textContent = texto;
        panelEstado.className = 'panel-estado' + (tipo ? ' ' + tipo : '');
    }
})();
