# Plan: Registro de comidas — implementación completa

## Estado

- [x] 0. Preparación
- [x] 1. Limpieza y acceso
- [x] 2. Modelo de datos
- [x] 3. Platos
- [x] 4. Cargar una comida
- [x] 5. Vista semanal y modal
- [x] 6. Editar y borrar registros
- [x] 7. Link compartido y vista de la nutricionista
- [x] 8. PWA
- [x] 9. Documentación y deploy

Verificado el 10/11/2026: 34 tests en verde y recorrido completo en Chrome (pantalla ancha y 390 px) contra una instancia con base en memoria.

**Pendiente (necesita el celular real y HTTPS):**

- [ ] Abrir la cámara trasera desde **Sacar foto** y cargar una comida real.
- [ ] Instalar la app en la pantalla de inicio, cerrarla y reabrirla ya logueada.
- [ ] Deploy con Docker Compose + Caddy y `REMEMBER_ME_KEY` propia.

**Diferencias con el plan original:**

- Los archivos del kit que se sacaban no se borraron: se movieron a `_kit-removido/` (no se compila; se puede borrar).
- Se quitaron HTMX y Google Fonts: la app no carga nada de terceros. La selección rápida se pide con `fetch`.
- El manifest lo sirve `PwaController` (desde `resources/pwa/`) para que salga con su tipo MIME; como estático salía `application/octet-stream`.
- Se agregó `GET /csrf` y un reintento en `app.js`: si la app queda abierta más que la sesión, el primer POST renueva el token en vez de fallar.
- Todo exige rol ADMIN (no solo sesión), por si quedó alguna cuenta `USER` del kit en la base.

## Contexto

`registro_de_comidas_especificacion.md` describe una PWA personal: el dueño saca fotos de lo que come desde el celular (dos toques: foto + plato) y la nutricionista las ve en una vista semanal por un link con token, sin cuenta.

La carpeta `comidas/` ya contiene el **starter kit** renombrado a `com.comidas` (Spring Boot 4.1, Java 25, Thymeleaf + HTMX, Spring Security con form login + CSRF, JPA + Flyway, MySQL 8.4 en Docker, tests con H2). Los 15 tests de `MvcIntegrationTest` pasan, hay JDK 25 y el contenedor `comidas-db-1` está corriendo. Todavía no hay nada de la app: solo usuarios, login, registro público y un panel admin de ejemplo. No hay `CLAUDE.md` ni repo git.

El plan implementa la especificación completa sobre esa base, sin dependencias nuevas.

## Decisiones

| Tema | Decisión | Motivo |
| --- | --- | --- |
| Stack | El kit tal cual: Thymeleaf + JS vanilla (+ HTMX que ya está) | Ya está armado y probado; la app son pocas pantallas |
| Fotos | Disco, en un directorio **privado** (`app.fotos.dir`, volumen Docker), servidas solo por controllers | La spec pide almacenamiento no público; `/uploads/**` del kit es público y se elimina |
| Miniaturas | Las genera el servidor con `ImageIO` al subir (lado mayor 400 px) | Sin dependencias; no se confía en una miniatura enviada por el cliente |
| IDs | `UUID` en las tres tablas nuevas, columna `VARCHAR(36)` con `@JdbcTypeCode(SqlTypes.VARCHAR)` | La spec pide uuid; ese mapeo valida igual en MySQL y en H2 (mismo truco que `Usuario.rol`) |
| Fechas | `Instant` en entidades + `hibernate.jdbc.time_zone=UTC`; se muestran con una constante `ZoneId` de Argentina, nunca con la zona de la JVM | "Se guardan en UTC y se muestran siempre en hora de Argentina" |
| Sesión del dueño | Remember-me de Spring Security (cookie de 180 días, `alwaysRemember`), clave en `REMEMBER_ME_KEY` | Criterio "abrirla ya logueado" |
| PWA | Manifest + íconos + metas de iOS. **Sin service worker** | La spec solo pide instalable y pantalla completa; sin offline no hay riesgo de assets viejos |
| Link compartido | Token de 32 bytes `SecureRandom` en base64url (43 caracteres); token inválido o revocado responde **404** en página y fotos | "Deja de funcionar al instante" |
| Usuario dueño | La tabla `usuarios` y `V1` quedan como están; el dueño es el ADMIN que crea `DataInitializer` con `ADMIN_EMAIL`/`ADMIN_PASSWORD` | Ya cumple "se crea una sola vez por configuración o seed" |
| Limpieza del kit | Se borra el registro público, el panel admin de ejemplo y la subida pública de archivos. Las utilidades genéricas (`DateUtils`, `StringUtils`, etc.) quedan | Lo primero contradice la spec; lo demás no molesta |

## Rutas

| Ruta | Acceso | Qué hace |
| --- | --- | --- |
| `GET /` | Dueño | Pantalla principal: botón **Sacar foto**, comidas de hoy, pendientes |
| `POST /registros` | Dueño | Recibe la foto (multipart) + `capturadoEn`; crea el registro pendiente; responde JSON con el id |
| `GET /seleccion-rapida` | Dueño | Fragmento con los platos ordenados por franja horaria |
| `POST /registros/{id}/plato` | Dueño | Asigna un plato existente |
| `POST /registros/{id}/plato-nuevo` | Dueño | Crea el plato y lo asigna |
| `GET·POST /registros/{id}/editar`, `POST /registros/{id}/borrar` | Dueño | Editar plato, fecha y hora, nota; borrar con confirmación |
| `GET /semana?fecha=` | Dueño | Vista semanal con acceso a editar |
| `GET /fotos/{id}`, `GET /fotos/{id}/mini` | Dueño | Foto completa y miniatura |
| `GET·POST /platos`, `/platos/{id}/editar`, `/platos/{id}/archivar` | Dueño | ABM de platos (sin borrar) |
| `GET /link`, `POST /link/generar`, `POST /link/revocar` | Dueño | Link compartido |
| `GET /ver/{token}?fecha=` | Público con token | Vista semanal de solo lectura |
| `GET /ver/{token}/fotos/{id}`, `.../mini` | Público con token | Fotos, validando el token en cada pedido |

## Fases

Cada fase termina con `./mvnw test` en verde.

### 0. Preparación
- Crear `CLAUDE.md` y `PLAN.md` como se describe arriba.
- Correr `./mvnw test` para confirmar la línea de base.

### 1. Limpieza y acceso
- Borrar: métodos `/registro` de `AuthController`, `pages/registro.html`, `RegistroRequest`, `UsuarioService.registrar`, `controller/admin/*`, `pages/admin/*`, `layout/admin-base.html`, `admin.css`, `UsuarioSpecification`, `FileUploadService` + su test, `WebConfig`, `CacheConfig`, `SlugUtil` + su test si queda sin uso.
- `SecurityConfig`: públicos solo `/login`, `/error`, `/actuator/health`, estáticos (`/css`, `/js`, `/icons`, `/manifest.webmanifest`) y `/ver/**`; el resto `authenticated()`. Sacar `/uploads/**` y `/registro`. Agregar `rememberMe`. Tras el login siempre a `/` (o a la página pedida).
- `DataInitializer`: sacar el usuario demo `USER`; queda el admin demo en dev y el admin por variables en prod.
- Nuevas propiedades: `app.fotos.dir` (`FOTOS_DIR`, default `fotos-privadas`), `app.remember-me.key`, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`. En prod `REMEMBER_ME_KEY` es obligatoria. Actualizar `.env.example`, `docker-compose.yml` (volumen `fotos_data` en lugar de `uploads_data`), `Dockerfile` (directorio) y `.gitignore`.
- Layouts nuevos: `layout/app.html` (dueño, navegación Hoy / Semana / Platos / Link, pensado para celular) y `layout/publico.html` (sin navegación ni login, con `<meta name="robots" content="noindex">`). Estilos en `static/css/comidas.css`, reutilizando variables y clases `.btn` / `.alert` de `global.css`.
- Reescribir `MvcIntegrationTest` para las rutas que quedan.

### 2. Modelo de datos
- `V2__crear_platos_registros_links.sql`: tablas `platos`, `registros` (FK a `platos` con `RESTRICT`, índice por `fecha_hora`) y `links_compartidos` (`token` único), con los campos de la spec.
- Entidades `Plato`, `Registro`, `LinkCompartido` con `UUID` e `Instant` (`creadoEn` con `@CreationTimestamp`; no extienden `BaseAuditable` porque usa `LocalDateTime`).
- Repositorios. `util/ZonaHoraria` con la constante de Argentina y un bean `Clock` para poder fijar la hora en los tests.
- Test que guarda un `Instant` y lee la columna cruda para confirmar que quedó en UTC.

### 3. Platos
- `PlatoService`: crear, editar, archivar / desarchivar, listar. No hay borrado.
- `PlatoController` + `pages/platos.html` y `pages/plato-form.html`.

### 4. Cargar una comida
- `FotoStorageService`: valida que sea JPEG (extensión, MIME y magic bytes, tomado de `FileUploadService`), guarda `<uuid>.jpg` y `<uuid>_mini.jpg`, abre y elimina ambos, con la protección de path traversal del kit.
- `RegistroService.crear(foto, capturadoEn)`: registro pendiente con la hora de captura que manda el celular (si viene vacía o en el futuro, usa la del servidor). Así un reintento conserva la hora original.
- `SeleccionRapidaService`: cuenta usos por plato en los últimos 30 días dentro de ±2 h de la hora actual de Argentina (con cruce de medianoche); primero los usados por cantidad, después el resto alfabético; sin archivados.
- `static/js/carga.js`:
  1. `<input type="file" accept="image/*" capture="environment">` disparado por el botón grande.
  2. Compresión con canvas: lado mayor 1600 px, JPEG calidad 0,8, respetando la orientación.
  3. Subida automática con `fetch` (token CSRF de los `<meta>` del layout) mientras se muestra la selección rápida.
  4. Si se toca un plato antes de que termine la subida, la elección queda en cola y se aplica al terminar.
  5. **Plato nuevo** (nombre + descripción), **Después** (queda pendiente), buscador que filtra los botones.
  6. Si falla la subida: aviso y botón **Reintentar** con la misma foto en memoria.
- Los endpoints que usa `fetch` responden JSON (`MensajeDTO` para errores) en lugar del redirect de `GlobalExceptionHandler`.
- `pages/hoy.html`: comidas de hoy y pendientes resaltados, que se completan con la misma selección rápida.

### 5. Vista semanal y modal
- `SemanaService`: dada una fecha, calcula lunes a domingo en hora de Argentina, trae los registros del rango y arma siete días (vacíos incluidos) con DTOs ya formateados (hora 24 h, plato, descripción, nota). Los DTOs se arman en el service porque `open-in-view` está apagado.
- `fragments/semana.html` compartido por el dueño y la nutricionista, parametrizado con la URL base de fotos y un flag de solo lectura.
- Navegación: semana anterior, siguiente, actual y `<input type="date">` que salta a la semana de esa fecha.
- CSS: grilla de siete columnas; en pantallas angostas los días se apilan.
- `static/js/semana.js`: modal con foto grande (se pide recién al abrir), plato, descripción, fecha y hora, nota; flechas anterior / siguiente; cierra con X, Escape o clic afuera.
- `GET /semana` del dueño; el modal suma un enlace **Editar**.

### 6. Editar y borrar registros
- `pages/registro-form.html`: plato (incluye el archivado si ya lo tenía), `datetime-local` interpretado en hora de Argentina, nota.
- Borrado con confirmación; elimina el registro y los dos archivos.

### 7. Link compartido y vista de la nutricionista
- `LinkCompartidoService`: `generar()` desactiva el activo y crea uno nuevo en la misma transacción; `revocar()`; `validar(token)`.
- `pages/link.html`: link actual, botón copiar, revocar, generar nuevo.
- `VerController` bajo `/ver/{token}`: solo `GET`; valida el token en la página y en cada foto; agrega `X-Robots-Tag: noindex, nofollow`, `Referrer-Policy: no-referrer` y `Cache-Control: private` en las fotos.

### 8. PWA
- `static/manifest.webmanifest` (`display: standalone`, `start_url: /`, `lang: es`) e íconos 192, 512 y maskable en `static/icons/`, generados con un script de una sola vez.
- Metas en `layout/app.html`: manifest, `theme-color`, `apple-touch-icon`, `apple-mobile-web-app-capable`.

### 9. Documentación y deploy
- Reescribir `README.md` para esta app (hoy es el del kit): qué es, cómo correrla, variables, deploy con Docker Compose + Caddy para HTTPS (necesario para instalar la PWA y para las cookies seguras), backup de base y de fotos.
- Repasar los 14 criterios de aceptación de la spec uno por uno y marcar `PLAN.md`.

## Archivos principales

- Se modifican: `config/SecurityConfig.java`, `config/DataInitializer.java`, `controller/AuthController.java`, `controller/HomeController.java`, `exception/GlobalExceptionHandler.java`, `application*.properties`, `.env.example`, `docker-compose.yml`, `Dockerfile`, `pages/login.html`, `fragments/header.html`, `MvcIntegrationTest.java`, `README.md`.
- Se crean: `db/migration/V2__...sql`; `entity/{Plato,Registro,LinkCompartido}`; sus repositorios; `service/{PlatoService,RegistroService,SeleccionRapidaService,SemanaService,FotoStorageService,LinkCompartidoService}`; `controller/{PlatoController,RegistroController,FotoController,SemanaController,LinkController,VerController}`; `dto/` para las vistas; `layout/{app,publico}.html`; `fragments/{semana,seleccion-rapida}.html`; `pages/{hoy,semana,platos,plato-form,registro-form,link,ver}.html`; `static/css/comidas.css`; `static/js/{carga,semana}.js`; `static/manifest.webmanifest`; `static/icons/*`; `CLAUDE.md`; `PLAN.md`.
- Se reutiliza: validación de magic bytes y path traversal de `FileUploadService`; patrón CSRF para `fetch` de `layout/base.html`; `fragments/alertas.html`; `ResourceNotFoundException` / `IllegalArgumentException` + `GlobalExceptionHandler`; `DateUtils.HH_MM`; `MensajeDTO`; `CustomUserDetailsService`.

## Verificación

**Tests automáticos (`./mvnw test`)**
- `SeleccionRapidaServiceTest`: orden por franja, cruce de medianoche, corte de 30 días, archivados fuera.
- `SemanaServiceTest`: un registro del domingo 23:30 de Argentina (lunes en UTC) cae en el domingo; días vacíos presentes; orden por hora.
- `FotoStorageServiceTest`: rechaza lo que no es JPEG, genera la miniatura, borra ambos archivos, bloquea path traversal.
- `LinkCompartidoServiceTest`: un solo link activo; el revocado no valida.
- `MvcIntegrationTest`:
  - `/` sin sesión redirige a `/login`; `/registro` ya no existe.
  - Flujo completo con `MockMultipartFile`: subir, asignar plato, editar, borrar.
  - `/ver/{token}` responde 200 sin login y con `noindex`.
  - Token inválido o revocado: 404 en la página y en las fotos.
  - `/fotos/{id}` sin sesión no entrega la foto.
  - Cualquier `POST` bajo `/ver/**` es rechazado.

**Manual**
- `docker compose up db -d` + `./mvnw spring-boot:run`, y recorrer en Chrome con emulación de celular: carga, selección rápida, plato nuevo, **Después**, reintento (cortando la red en DevTools), edición, borrado, semana y modal.
- Abrir el link en una ventana de incógnito: se ve la semana, no hay controles de edición; revocar y comprobar que página y fotos dan 404.
- En el celular real, por HTTPS (deploy o un túnel temporal): cámara trasera, instalar en la pantalla de inicio, cerrar y reabrir ya logueado.
