# Registro de comidas — especificación

Nov 10, 2026 · @Gabriel Enrriquez

## Resumen

App web personal para registrar con fotos todo lo que como y mostrárselo a mi nutricionista por un link.

Hoy le muestro las fotos desde la galería del celular y tengo que explicarle qué es cada comida. La app resuelve eso: cada foto queda con fecha, hora y el plato identificado, ordenada en una vista semanal.

La usan dos personas: yo, que cargo las fotos desde el celular, y la nutricionista, que solo mira.

## Usuarios y acceso

| Rol | Cómo entra | Qué puede hacer |
| --- | --- | --- |
| Dueño (un solo usuario) | Login con email y contraseña | Todo: cargar fotos, administrar platos, editar registros, manejar el link |
| Nutricionista | Link con token secreto, sin cuenta | Solo ver la vista semanal y el detalle de cada foto |

- No hay registro público de usuarios: la cuenta del dueño se crea una sola vez por configuración o seed.
- El link compartido tiene un token largo y aleatorio en la URL, por ejemplo `/ver/<token>`.
- El dueño puede revocar el link y generar uno nuevo; el anterior deja de funcionar al instante.
- La vista compartida lleva `noindex` para que no aparezca en buscadores.

## Flujo principal: cargar una comida

Cargar una comida tiene que llevar dos toques: sacar la foto y elegir el plato.

1. Abro la app desde el ícono en la pantalla de inicio del celular (PWA instalada).
2. La pantalla principal tiene un botón grande **Sacar foto**, que abre la cámara trasera del celular.
3. Al confirmar la foto, se comprime en el celular y empieza a subirse sola, sin botón de guardar.
4. Mientras sube, aparece la **selección rápida**: mis platos precargados como botones grandes.
5. Toco un plato y el registro queda guardado con la fecha y hora de ese momento.
6. Vuelvo a la pantalla principal y veo la comida recién cargada en el día de hoy.

Reglas de la selección rápida:

- Los platos se ordenan por los más usados en esa franja horaria (2 horas antes y después de la hora actual, últimos 30 días). El resto va debajo, en orden alfabético.
- Hay un buscador por nombre para cuando la lista crece.
- **Plato nuevo**: pide nombre y descripción corta, lo guarda en la lista de platos y lo asigna al registro.
- **Después**: guarda el registro sin plato. Queda marcado como pendiente para completarlo más tarde.
- Si la subida falla, la app avisa y permite reintentar sin volver a sacar la foto.

## Funciones del dueño

Además de cargar comidas, el dueño administra tres cosas: platos, registros y el link.

**Platos**

- Crear, editar y archivar platos. Cada plato tiene nombre y descripción corta (qué lleva, porción aproximada).
- Un plato archivado no aparece en la selección rápida, pero los registros viejos lo siguen mostrando.
- Editar la descripción de un plato actualiza lo que se ve en todos sus registros.

**Registros**

- Ver mis comidas en la misma vista semanal que ve la nutricionista.
- Editar un registro: cambiar el plato, corregir fecha y hora, agregar o editar una nota.
- La nota es un texto corto propio de esa comida, por ejemplo "comí la mitad" o "con gaseosa".
- Borrar un registro, con confirmación. Se borra también la foto.
- Los registros pendientes (sin plato) se distinguen a simple vista y se completan con la misma selección rápida.

**Link compartido**

- Pantalla con el link actual y un botón para copiarlo.
- Botones para revocarlo y para generar uno nuevo.

## Vista de la nutricionista

La nutricionista abre el link y ve una semana completa de fotos, sin iniciar sesión ni instalar nada.

**Vista semanal**

- Siete columnas, de lunes a domingo, con la fecha en el encabezado de cada una.
- Dentro de cada día, las miniaturas de las fotos ordenadas por hora, de la más temprana a la más tardía.
- Debajo de cada miniatura van la hora y el nombre del plato.
- Un día sin registros se muestra vacío, no se oculta.
- En pantallas angostas, los días se apilan uno debajo del otro.
- Al abrir el link se muestra la semana actual.

**Navegación**

- Botones de semana anterior y semana siguiente, y un botón para volver a la semana actual.
- Un selector de fecha para saltar a cualquier semana, porque entre consultas pasan dos meses.

**Detalle de una comida (modal)**

- Se abre al tocar una miniatura.
- Muestra la foto en grande, el nombre del plato, la descripción, la fecha y hora, y la nota si tiene.
- Flechas para pasar a la comida anterior y a la siguiente sin cerrar el modal.
- Se cierra con la X, con Escape o tocando fuera.

La vista es de solo lectura: no hay ningún control para editar, borrar ni comentar.

## Modelo de datos

Son tres tablas: platos, registros y link compartido.

| Tabla | Campo | Tipo | Notas |
| --- | --- | --- | --- |
| `platos` | `id` | uuid | Clave primaria |
| `platos` | `nombre` | texto | Obligatorio |
| `platos` | `descripcion` | texto | Corta; qué lleva y porción aproximada |
| `platos` | `archivado` | booleano | Por defecto `false` |
| `platos` | `creado_en` | fecha y hora |  |
| `registros` | `id` | uuid | Clave primaria |
| `registros` | `fecha_hora` | fecha y hora | Momento de la foto; editable |
| `registros` | `foto_path` | texto | Ruta del archivo en el almacenamiento |
| `registros` | `plato_id` | uuid, opcional | Vacío = registro pendiente |
| `registros` | `nota` | texto, opcional | Propia de esa comida |
| `registros` | `creado_en` | fecha y hora |  |
| `links_compartidos` | `id` | uuid | Clave primaria |
| `links_compartidos` | `token` | texto, único | Aleatorio, 32 caracteres o más |
| `links_compartidos` | `activo` | booleano | Solo un link activo a la vez |
| `links_compartidos` | `creado_en` | fecha y hora |  |

- Las fechas se guardan en UTC y se muestran siempre en hora de Argentina (`America/Argentina/Buenos_Aires`).
- Borrar un plato no está permitido si tiene registros; se archiva.

## Requisitos técnicos

Es una PWA pensada primero para el celular del lado del dueño, y para cualquier pantalla del lado de la nutricionista.

- **PWA instalable**: manifest e ícono para agregarla a la pantalla de inicio y abrirla a pantalla completa.
- **Cámara**: captura con `<input type="file" accept="image/*" capture="environment">`, sin permisos especiales.
- **Compresión en el celular** antes de subir: lado mayor de 1600 px como máximo, JPEG con calidad cercana al 80 %.
- **Miniaturas**: la vista semanal carga versiones chicas de las fotos; la foto completa se pide recién al abrir el modal.
- **Fotos privadas**: el almacenamiento no es público. La vista compartida obtiene las fotos solo si el token es válido y está activo (URLs firmadas o un endpoint que valide el token).
- **Semana de lunes a domingo**, idioma español, formato de 24 horas.
- **Interfaz** simple y clara, con botones grandes en la carga desde el celular.

## Criterios de aceptación

La primera versión está lista cuando se cumple todo esto:

- [ ] Puedo instalar la app en la pantalla de inicio del celular y abrirla ya logueado.
- [ ] Desde la pantalla principal, cargar una comida con un plato existente lleva dos toques además de la cámara.
- [ ] La foto se sube sola al confirmarla y el registro queda con la fecha y hora correctas de Argentina.
- [ ] La selección rápida muestra primero los platos más usados en esa franja horaria.
- [ ] Puedo crear un plato nuevo desde la selección rápida y queda disponible para la próxima vez.
- [ ] Puedo guardar una foto sin plato y completarla después.
- [ ] Puedo editar el plato, la fecha y hora y la nota de un registro, y borrarlo.
- [ ] Puedo crear, editar y archivar platos.
- [ ] El link compartido abre la vista semanal sin pedir login.
- [ ] La vista semanal muestra los siete días con las fotos ordenadas por hora y permite navegar entre semanas.
- [ ] Al tocar una foto se abre el modal con foto, plato, descripción, fecha y hora, y nota.
- [ ] Desde el link compartido no se puede editar ni borrar nada.
- [ ] Al revocar el link, la URL anterior deja de mostrar datos y fotos.
- [ ] Las fotos no son accesibles sin sesión del dueño o sin un token activo.
