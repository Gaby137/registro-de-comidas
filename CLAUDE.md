# Registro de comidas

App web personal (PWA) para registrar con fotos lo que come el dueño y mostrárselo a su nutricionista por un link con token.

- **Fuente de verdad:** `registro_de_comidas_especificacion.md`. Si el código y la especificación no coinciden, manda la especificación.
- **Plan de implementación y estado de cada fase:** `PLAN.md`. Marcá la fase al terminarla.

## Stack

Spring Boot 4.1 · Java 25 · Thymeleaf + Layout Dialect · JS vanilla · Spring Security (form login, sesión, CSRF, remember-me) · JPA + Flyway · MySQL 8.4 · tests con JUnit + MockMvc + H2 en modo MySQL.

Paquete base: `com.comidas`. No hay API REST ni JWT, no se agregan dependencias sin necesidad y las páginas no cargan nada de terceros (ni CDN ni fuentes externas).

## Comandos

```bash
docker compose up db -d       # MySQL en localhost:3307
./mvnw spring-boot:run        # app en http://localhost:8080 (perfil dev)
./mvnw test                   # H2 en memoria, no necesita Docker
```

En PowerShell usar `.\mvnw.cmd`. En dev el login es `admin@app.com` / `admin123`.

Para probar sin tocar los datos de desarrollo, levantar una instancia con H2 en memoria: `spring-boot:run -Dspring-boot.run.useTestClasspath=true` con `SPRING_DATASOURCE_URL=jdbc:h2:mem:verif;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`, `SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver`, un perfil que no sea `dev`, `APP_REMEMBER_ME_KEY` de 32+ caracteres y otro `SERVER_PORT`.

## Reglas del proyecto

- **Dos roles, sin registro público.** El dueño entra con email y contraseña (es el ADMIN que crea `DataInitializer`). La nutricionista entra por `/ver/<token>`, sin cuenta.
- **`/ver/**` es de solo lectura.** Solo `GET`, sin controles de edición, con `noindex`. Token inválido o revocado responde 404, también para las fotos.
- **Las fotos son privadas.** Se guardan en `app.fotos.dir`, fuera de cualquier ruta estática, y solo se sirven desde un controller que valide sesión o token. Nunca exponer el directorio como recurso estático.
- **Fechas en UTC, vista en hora de Argentina.** Las entidades usan `Instant`; para mostrar o interpretar se usa `ZonaHoraria.AR`, nunca la zona por defecto de la JVM. Semana de lunes a domingo, formato de 24 horas.
- **IDs `UUID`** en `platos`, `registros` y `links_compartidos` (columna `VARCHAR(36)`).
- **Los platos no se borran, se archivan.** Un plato archivado no aparece en la selección rápida pero sus registros lo siguen mostrando.
- **Migraciones Flyway:** nunca editar una ya aplicada; crear `V<n>__descripcion.sql`. Hibernate solo valida (`ddl-auto=validate`).
- **Errores:** los services lanzan `ResourceNotFoundException` (404) o `IllegalArgumentException` (regla de negocio); `GlobalExceptionHandler` los traduce. Los endpoints que consume `fetch` responden JSON con `MensajeDTO`.
- **`open-in-view` está apagado:** los DTOs de las vistas se arman dentro del service.
- **Formularios y `fetch` llevan CSRF:** `th:action` en los forms; en `fetch`, usar `Comidas.enviar` (`static/js/app.js`), que agrega el header y renueva el token si la sesión venció.
- **Idioma:** código, nombres, mensajes e interfaz en español.
- **Interfaz del dueño:** pensada primero para el celular, con botones grandes. Cargar una comida son dos toques: foto y plato.
