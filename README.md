# Registro de comidas

App web personal (PWA) para registrar con fotos todo lo que como y mostrárselo a mi nutricionista por un link.

- **Dueño:** entra con email y contraseña desde el celular. Cargar una comida son dos toques: sacar la foto y elegir el plato.
- **Nutricionista:** abre un link con token secreto, sin cuenta, y ve una semana completa de fotos. Solo lectura.

La especificación funcional está en [`registro_de_comidas_especificacion.md`](registro_de_comidas_especificacion.md).

## Índice

1. [Stack](#1-stack)
2. [Correr en desarrollo](#2-correr-en-desarrollo)
3. [Cómo se usa](#3-cómo-se-usa)
4. [Rutas](#4-rutas)
5. [Decisiones de diseño](#5-decisiones-de-diseño)
6. [Tests](#6-tests)
7. [Deploy a producción](#7-deploy-a-producción)
8. [Variables de entorno](#8-variables-de-entorno)
9. [Estructura del proyecto](#9-estructura-del-proyecto)
10. [Problemas comunes](#10-problemas-comunes)

---

## 1. Stack

- **Java 25** + **Spring Boot 4.1**
- **Thymeleaf** + Layout Dialect, JavaScript sin frameworks ni librerías externas
- **Spring Security**: form login, sesión, CSRF y cookie remember-me de 180 días
- **Spring Data JPA** + **Flyway** + **MySQL 8.4**
- **Docker** multi-stage + docker-compose
- Tests con **JUnit 5 + MockMvc + H2** (modo MySQL)

La página no carga nada de terceros (ni fuentes ni scripts): todo sale del mismo servidor.

## 2. Correr en desarrollo

Requisitos: JDK 25 y Docker Desktop. Maven no hace falta (el proyecto trae `./mvnw`; en PowerShell, `.\mvnw.cmd`).

```bash
cp .env.example .env            # completar DB_PASS y DB_ROOT_PASS como mínimo
docker compose up db -d         # MySQL en localhost:3307
./mvnw spring-boot:run          # app en http://localhost:8080 (perfil dev)
```

Al arrancar en dev:

- Flyway crea o actualiza las tablas (`src/main/resources/db/migration`).
- Si la base está vacía, crea un dueño de prueba: `admin@app.com` / `admin123`.
- Las fotos se guardan en `fotos-privadas/` (está en `.gitignore`).

Para probar desde el celular hace falta HTTPS (la cámara funciona por HTTP en la red local, pero instalar la PWA no). Lo más simple es un túnel temporal, por ejemplo `cloudflared tunnel --url http://localhost:8080`.

Para empezar de cero (borra todos los datos): `docker compose down -v && docker compose up db -d`, y borrar `fotos-privadas/`.

## 3. Cómo se usa

**Cargar una comida**

1. Abrir la app desde el ícono de la pantalla de inicio.
2. Tocar **Sacar foto**. Se abre la cámara trasera.
3. Al confirmar la foto, se comprime en el celular (1600 px, JPEG ~80 %) y se sube sola.
4. Mientras sube aparece la **selección rápida**: los platos como botones grandes, primero los más usados a esa hora. Tocar uno guarda la comida.

Otras opciones del mismo panel:

- **Plato nuevo:** pide nombre y descripción corta, lo guarda en la lista y lo asigna.
- **Después:** guarda la foto sin plato. Queda como *pendiente* y se completa más tarde desde la pantalla principal.
- Si la subida falla, aparece **Reintentar**: no hay que volver a sacar la foto y se conserva la hora original.

**Las otras pantallas**

| Pantalla | Para qué |
| --- | --- |
| Hoy | Sacar foto, comidas de hoy, pendientes de otros días |
| Semana | La misma vista que ve la nutricionista, con acceso a editar cada comida |
| Platos | Crear, editar y archivar platos |
| Link | Ver y copiar el link compartido, revocarlo o generar uno nuevo |

**Instalar en el celular**

- Android (Chrome): menú ⋮ → *Agregar a pantalla principal* / *Instalar app*.
- iPhone (Safari): botón compartir → *Agregar a inicio*.

La sesión queda recordada 180 días, así que la app abre ya logueada.

## 4. Rutas

| Ruta | Acceso | Qué hace |
| --- | --- | --- |
| `GET /` | Dueño | Pantalla principal |
| `POST /registros` | Dueño | Recibe la foto y crea el registro pendiente. Responde JSON con el `id` |
| `GET /seleccion-rapida` | Dueño | Fragmento HTML con los platos ordenados para la hora actual |
| `POST /registros/{id}/plato` | Dueño | Asigna un plato existente |
| `POST /registros/{id}/plato-nuevo` | Dueño | Crea un plato y lo asigna |
| `GET·POST /registros/{id}/editar` | Dueño | Cambiar plato, fecha y hora, nota |
| `POST /registros/{id}/borrar` | Dueño | Borra el registro y su foto |
| `GET /semana?fecha=` | Dueño | Vista semanal |
| `GET /fotos/{id}`, `/fotos/{id}/mini` | Dueño | Foto completa y miniatura |
| `GET·POST /platos`, `/platos/{id}/editar`, `/platos/{id}/archivar` | Dueño | Administración de platos |
| `GET /link`, `POST /link/generar`, `POST /link/revocar` | Dueño | Link compartido |
| `GET /ver/{token}?fecha=` | Público con token | Vista semanal de solo lectura |
| `GET /ver/{token}/fotos/{id}`, `.../mini` | Público con token | Fotos, validando el token en cada pedido |
| `GET /actuator/health` | Público | Estado de la app |

Todo lo que no es `/login`, estáticos, health y `GET /ver/**` exige la sesión del dueño.

## 5. Decisiones de diseño

- **Fotos privadas.** Se guardan en `FOTOS_DIR`, fuera de cualquier ruta estática. Solo salen por un controller que antes validó la sesión del dueño (`/fotos/...`) o un token activo (`/ver/<token>/fotos/...`). Cada foto son dos archivos: `<uuid>.jpg` y `<uuid>_mini.jpg`; la miniatura (400 px) la genera el servidor.
- **Link compartido.** El token son 32 bytes aleatorios en base64url. Solo hay un link activo: generar uno nuevo o revocar deja al anterior respondiendo 404, para la página y para las fotos. La vista lleva `noindex`, `Referrer-Policy: no-referrer` y no se guarda en caché.
- **Fechas.** Se guardan en UTC y se muestran siempre en hora de Argentina (`ZonaHoraria.AR`), sin depender de la zona del servidor ni del navegador. Semana de lunes a domingo, formato de 24 horas.
- **Hora de la comida.** Es el momento en que se confirmó la foto en el celular. Si esa hora falta, es futura o tiene más de 7 días, se usa la del servidor.
- **Selección rápida.** Primero los platos más usados en la franja de 2 horas antes y después de la hora actual, en los últimos 30 días (cruzando la medianoche); el resto, en orden alfabético. Los archivados no aparecen.
- **Platos.** No se borran: se archivan. Un plato archivado sigue mostrándose en sus registros viejos.
- **Un solo dueño.** No hay registro público. La cuenta se crea al arrancar con `ADMIN_EMAIL` y `ADMIN_PASSWORD`.
- **PWA sin service worker.** Es instalable y abre a pantalla completa, pero no funciona sin conexión.

## 6. Tests

```bash
./mvnw test
```

Usan H2 en memoria (perfil `test`), no necesitan Docker.

| Test | Qué cubre |
| --- | --- |
| `MvcIntegrationTest` | Acceso, flujo completo de carga, plato nuevo, pendientes, edición y borrado, platos, link compartido y su revocación |
| `SeleccionRapidaServiceTest` | Orden por franja horaria, cruce de medianoche, ventana de 30 días, archivados |
| `SemanaServiceTest` | Semana de lunes a domingo en hora de Argentina, días vacíos, fechas guardadas en UTC |
| `FotoStorageServiceTest` | Solo JPEG válidos, miniatura, borrado, path traversal |
| `LinkCompartidoServiceTest` | Un solo link activo, revocación |
| `ApplicationTests` | Que el contexto levante con las migraciones |

## 7. Deploy a producción

### 7.1 Con Docker Compose (VPS)

```bash
cp .env.example .env            # completar TODO (ver sección 8)
openssl rand -base64 48         # usar la salida como REMEMBER_ME_KEY
docker compose up -d --build
docker compose logs -f app      # verificar que arrancó
```

En el primer arranque Flyway crea las tablas y se crea la cuenta del dueño con `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

### 7.2 HTTPS (obligatorio)

Sin HTTPS la PWA no se puede instalar y la cookie de sesión no viaja. La app escucha HTTP en el 8080: poné adelante un proxy reverso con HTTPS. Ejemplo mínimo con **Caddy** (certificado automático):

```
comidas.tudominio.com {
    reverse_proxy localhost:8080
}
```

La app respeta los headers `X-Forwarded-*`, así que el link compartido se arma con el dominio real.

### 7.3 Checklist

- [ ] `REMEMBER_ME_KEY` generada con `openssl rand -base64 48`
- [ ] `DB_PASS` y `DB_ROOT_PASS` fuertes
- [ ] `ADMIN_PASSWORD` fuerte
- [ ] HTTPS funcionando y `COOKIE_SECURE=true`
- [ ] `./mvnw test` en verde
- [ ] Health check: `curl https://comidas.tudominio.com/actuator/health` → `{"status":"UP"}`
- [ ] Backups de la base **y de las fotos** (van en volúmenes distintos):

```bash
# Base de datos
docker compose exec -T db sh -c 'exec mysqldump --single-transaction -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > backup.sql
# Fotos
docker compose exec -T app tar -czf - -C /app fotos > fotos.tar.gz
```

## 8. Variables de entorno

| Variable | Obligatoria en prod | Default | Descripción |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | sí: `prod` (compose ya lo pone) | `dev` | Perfil activo |
| `DATABASE_URL` | sí (compose la arma) | dev: `jdbc:mysql://localhost:<DB_PORT>/<DB_NAME>` | URL JDBC |
| `DB_NAME` | — | `comidas_db` | Nombre de la base |
| `DB_USER` / `DB_PASS` | sí | `appuser` / `apppass` | Usuario de la app en MySQL |
| `DB_ROOT_PASS` | sí | `rootpass` | Root de MySQL (compose) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | primer arranque | vacío | Cuenta del dueño, si todavía no existe |
| `REMEMBER_ME_KEY` | **sí** (mín. 32 caracteres) | dev: clave fija | Firma la cookie que mantiene la sesión |
| `COOKIE_SECURE` | — | `true` | Cookie de sesión solo por HTTPS |
| `FOTOS_DIR` | — | `fotos-privadas` (compose: `/app/fotos`) | Directorio privado de las fotos |
| `APP_NOMBRE` | — | `Comidas` | Nombre visible en la app |
| `APP_PORT` / `DB_PORT` | — | `8080` / `3307` | Puertos de la app y de MySQL |
| `TZ` | — | `America/Argentina/Buenos_Aires` | Zona horaria de los logs |

## 9. Estructura del proyecto

```
src/main/java/com/comidas/
├── config/        SecurityConfig, DataInitializer (cuenta del dueño), TiempoConfig (Clock)
├── controller/    Home (Hoy), Carga (fetch/JSON), Registro, Foto, Semana, Plato, Link, Ver (nutricionista), Pwa (manifest)
├── dto/           Vistas ya formateadas (RegistroVista, DiaVista, SemanaVista, PlatoOpcion) y formularios
├── entity/        Plato, Registro, LinkCompartido, Usuario
├── exception/     GlobalExceptionHandler, ResourceNotFoundException
├── repository/    Spring Data JPA
├── service/       Registro, Plato, SeleccionRapida, Semana, FotoStorage, LinkCompartido
└── util/          ZonaHoraria (hora de Argentina) y utilidades generales

src/main/resources/
├── db/migration/  V1 usuarios, V2 platos + registros + links_compartidos
├── pwa/           manifest.webmanifest (lo sirve PwaController con su tipo MIME)
├── static/
│   ├── css/       global.css (base), comidas.css (componentes), auth.css (login)
│   ├── js/        app.js (fetch con CSRF), carga.js (foto + selección rápida), semana.js (modal)
│   └── icons/     Íconos de la PWA
└── templates/
    ├── layout/    app (dueño), publico (nutricionista), base (login y error)
    ├── fragments/ semana (vista semanal compartida), seleccion-rapida, alertas
    └── pages/     hoy, semana, registro-form, platos, plato-form, link, ver, login
```

`_kit-removido/` guarda los archivos del starter kit que esta app no usa (registro público, panel admin, subida pública de archivos). No se compila y se puede borrar.

## 10. Problemas comunes

| Síntoma | Causa / solución |
| --- | --- |
| `Communications link failure` al arrancar | MySQL no está levantado (`docker compose up db -d`) o `DB_PORT` no coincide |
| `Access denied for user 'appuser'` | Cambiaste `DB_USER`/`DB_PASS` después de crear el volumen. Recrear la base: `docker compose down -v && docker compose up db -d` |
| `app.remember-me.key (REMEMBER_ME_KEY) debe tener al menos 32 caracteres` | Falta la variable en producción o es corta: `openssl rand -base64 48` |
| `Schema-validation: missing table/column` | Falta una migración Flyway para un cambio en una entidad |
| `Migration checksum mismatch` | Se editó una migración ya aplicada. Revertir y crear una nueva |
| El login no mantiene la sesión en prod | Estás sin HTTPS con `COOKIE_SECURE=true` |
| `403` en un POST | Falta el token CSRF: usar `th:action` en los formularios y `Comidas.enviar` en `fetch` |
| No aparece "Instalar app" en el celular | La página no está servida por HTTPS |
| El link compartido da 404 | Fue revocado o se generó uno nuevo: copiar el actual desde la pantalla **Link** |
