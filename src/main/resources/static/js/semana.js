/*
 * Vista semanal: detalle de una comida en un modal.
 *
 * - Se abre al tocar una miniatura. La foto completa se pide recién en ese momento.
 * - Flechas (en pantalla o del teclado) para pasar a la comida anterior y a la siguiente
 *   sin cerrar el modal, en orden cronológico dentro de la semana.
 * - Se cierra con la X, con Escape o tocando fuera.
 *
 * Lo usan el dueño y la nutricionista. Para ella el botón "Editar" no existe en el HTML.
 */
(function () {
    'use strict';

    const modal = document.getElementById('modal');
    if (!modal) {
        return;
    }
    // El orden del DOM ya es cronológico: días de lunes a domingo y, dentro de cada día, por hora
    const miniaturas = Array.prototype.slice.call(document.querySelectorAll('.mini'));

    const foto = document.getElementById('modalFoto');
    const plato = document.getElementById('modalPlato');
    const fecha = document.getElementById('modalFecha');
    const descripcion = document.getElementById('modalDescripcion');
    const nota = document.getElementById('modalNota');
    const notaTexto = document.getElementById('modalNotaTexto');
    const editar = document.getElementById('modalEditar');
    const anterior = document.getElementById('modalAnterior');
    const siguiente = document.getElementById('modalSiguiente');
    const cerrar = document.getElementById('modalCerrar');

    let indice = -1;

    function mostrar(nuevoIndice) {
        if (nuevoIndice < 0 || nuevoIndice >= miniaturas.length) {
            return;
        }
        indice = nuevoIndice;
        const datos = miniaturas[indice].dataset;

        // Primero la miniatura (ya está en caché) y encima la foto completa cuando llega
        foto.src = miniaturas[indice].querySelector('img').src;
        const completa = new Image();
        const pedido = indice;
        completa.onload = function () {
            if (indice === pedido && modal.open) {
                foto.src = completa.src;
            }
        };
        completa.src = datos.foto;
        foto.alt = datos.plato ? 'Foto de ' + datos.plato : 'Foto de la comida';

        plato.textContent = datos.plato || 'Sin identificar';
        fecha.textContent = datos.fecha;
        descripcion.textContent = datos.descripcion || '';
        descripcion.hidden = !datos.descripcion;
        notaTexto.textContent = datos.nota || '';
        nota.hidden = !datos.nota;
        if (editar) {
            editar.href = datos.editar;
        }

        anterior.disabled = indice === 0;
        siguiente.disabled = indice === miniaturas.length - 1;
    }

    miniaturas.forEach(function (miniatura, i) {
        miniatura.addEventListener('click', function () {
            mostrar(i);
            modal.showModal();
        });
    });

    anterior.addEventListener('click', function () {
        mostrar(indice - 1);
    });
    siguiente.addEventListener('click', function () {
        mostrar(indice + 1);
    });
    cerrar.addEventListener('click', function () {
        modal.close();
    });

    // Tocar fuera: el clic cae en el <dialog> mismo (su fondo), no en la caja de adentro
    modal.addEventListener('click', function (evento) {
        if (evento.target === modal) {
            modal.close();
        }
    });

    // Flechas del teclado y Escape. El <dialog> ya cierra solo con Escape en la mayoría de los
    // navegadores; se maneja también acá para no depender de eso.
    modal.addEventListener('keydown', function (evento) {
        if (evento.key === 'ArrowLeft') {
            mostrar(indice - 1);
        } else if (evento.key === 'ArrowRight') {
            mostrar(indice + 1);
        } else if (evento.key === 'Escape') {
            evento.preventDefault();
            modal.close();
        }
    });

    // Al cerrar, el foco vuelve a la miniatura que se estaba viendo
    modal.addEventListener('close', function () {
        if (indice >= 0 && !modal.open) {
            miniaturas[indice].focus();
        }
    });
})();
