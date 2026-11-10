# Starter Kit — Base para proyectos freelance (MVC + API REST)

Base reutilizable con **Spring Boot 4** para arrancar proyectos de clientes sin empezar de cero.
Sirve para tres tipos de proyecto:

| Tipo de proyecto | Ejemplo | Qué usás del kit |
|---|---|---|
| **Web MVC** | Sitio con panel admin, formularios, catálogo | Thymeleaf + sesiones (`/`, `/admin/**`) |
| **API REST** | Backend para un frontend React/Angular o una app mobile | JWT stateless (`/api/**`) |
| **Ambos** | Sitio público + API para la app mobile | Todo (viene así por defecto) |

---

## Índice

1. [Stack](#1-stack)
2. [Requisitos](#2-requisitos)
3. [Crear un proyecto nuevo desde el kit](#3-crear-un-proyecto-nuevo-desde-el-kit)
4. [Correr en desarrollo](#4-correr-en-desarrollo)
5. [Elegir el modo: MVC, API o ambos](#5-elegir-el-modo-mvc-api-o-ambos)
6. [Receta: agregar un recurso nuevo (paso a paso)](#6-receta-agregar-un-recurso-nuevo-paso-a-paso)
7. [Seguridad](#7-seguridad)
8. [Manejo de errores](#8-manejo-de-errores)
9. [Base de datos y migraciones](#9-base-de-datos-y-migraciones)
10. [Subida de archivos](#10-subida-de-archivos)
11. [Tests](#11-tests)
12. [Deploy a producción](#12-deploy-a-producción)
13. [Variables de entorno](#13-variables-de-entorno)
14. [Referencia de la API incluida](#14-referencia-de-la-api-incluida)
15. [Estructura del proyecto](#15-estructura-del-proyecto)
16. [Problemas comunes](#16-problemas-comunes)

---

## 1. Stack

- **Java 25** + **Spring Boot 4.1**
- **Thymeleaf** + Layout Dialect + HTMX (vistas MVC)
- **API REST con JWT** (Auth0 `java-jwt`, HS256)
- **Spring Security** dual: sesiones + CSRF para MVC, tokens stateless para API
- **Spring Data JPA** + **Specifications** (filtros dinámicos y paginación)
- **Flyway** (migraciones versionadas) + **MySQL 8.4** (LTS)
- **Actuator** (solo `/actuator/health`)
- **Swagger UI / OpenAPI** (springdoc) en `/swagger-ui.html` — apagado en prod
- **Docker** multi-stage + docker-compose
- **Lombok** + **Bean Validation**
- Tests con **JUnit 5 + MockMvc + H2** (modo MySQL)

---

## 2. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| JDK | 25 | Compilar y correr |
| Docker Desktop | cualquiera reciente | MySQL en desarrollo y deploy |
| Maven | **no hace falta** | El proyecto trae Maven Wrapper (`./mvnw`) |
| IntelliJ IDEA | recomendado | Activar *Annotation Processing* para Lombok |

> En Windows usá `mvnw.cmd` en vez de `./mvnw` si estás en CMD/PowerShell.
> En Git Bash funciona `./mvnw`.

---

## 3. Crear un proyecto nuevo desde el kit

### 3.1 Copiar el kit

```bash
# Opción A: si el kit está en un repo
git clone <repo-del-kit> cliente-xyz
cd cliente-xyz
rm -rf .git && git init          # historial limpio para el cliente

# Opción B: copiar la carpeta (sin target/, uploads/*, .idea/ ni .env)
```

### 3.2 Renombrar el proyecto

1. **`pom.xml`**: cambiar `groupId`, `artifactId`, `name` y `description`.
   ```xml
   <groupId>com.clientexyz</groupId>
   <artifactId>clientexyz</artifactId>
   <name>Cliente XYZ</name>
   ```

2. **Paquete base** `com.tudominio.app` → `com.clientexyz` (o el que corresponda).
   En IntelliJ: click derecho sobre el paquete `com.tudominio.app` → **Refactor → Rename**,
   marcando *"Search in comments and strings"*. Después verificá que no quede nada:
   ```bash
   grep -rn "tudominio" src/      # no debería devolver nada
   ```
   Hay strings que el refactor tiene que actualizar: `basePackages` en `ApiExceptionHandler`
   y `GlobalExceptionHandler`, y `logging.level.com.tudominio` en los `.properties`.

3. **Nombre técnico**: `spring.application.name` en `application.properties` (aparece en los
   logs y es el `iss` de los tokens JWT).

4. **Nombre visible de la app**: `APP_NOMBRE` en `.env` (o `app.nombre` en `application.properties`).
   Se usa en header, footer, títulos, la descripción de la página y Swagger.

5. **Base de datos**: `DB_NAME` en `.env`. La usan docker compose y la app en dev, así que no hay que
   tocar ningún `.properties`. Los contenedores y volúmenes de Docker toman el nombre de la carpeta
   del proyecto (`cliente-xyz-db-1`), así que varios proyectos del kit conviven sin pisarse.

6. **CSS**: colores en las variables `:root` de `static/css/global.css` y `admin.css`.

7. **Contenido de ejemplo**: los textos de `pages/home.html` y del dashboard son genéricos, reemplazalos.

### 3.3 Configurar variables de entorno

```bash
cp .env.example .env
# Editar .env: como mínimo DB_NAME, DB_PASS, DB_ROOT_PASS, JWT_SECRET, ADMIN_EMAIL, ADMIN_PASSWORD
openssl rand -base64 48         # usar la salida como JWT_SECRET
```

El mismo `.env` lo leen docker compose y la app en el perfil dev: las credenciales de MySQL
siempre coinciden.

### 3.4 Elegir el modo

Si el proyecto es solo MVC o solo API, seguí la [sección 5](#5-elegir-el-modo-mvc-api-o-ambos)
para borrar lo que no usás. Si usás ambos, no hay que tocar nada.

### 3.5 Verificar

```bash
./mvnw test                     # tienen que pasar todos
```

---

## 4. Correr en desarrollo

```bash
# 1. Levantar solo MySQL (puerto 3307 en localhost)
docker compose up db -d

# 2. Correr la app con el perfil dev (es el perfil por defecto)
./mvnw spring-boot:run
```

Abrir <http://localhost:8080> (o el `APP_PORT` de tu `.env`).

**Documentación de la API (Swagger UI):** <http://localhost:8080/swagger-ui.html>
— lista todos los endpoints de `/api/**` y permite probarlos desde el navegador
(ver [Probar la API rápido](#probar-la-api-rápido)).

Al arrancar en **dev**:
- Flyway crea/actualiza las tablas (`src/main/resources/db/migration`).
- Si la base está vacía, se crean usuarios de prueba:

| Rol | Email | Contraseña |
|---|---|---|
| ADMIN | `admin@app.com` | `admin123` |
| USER | `usuario@app.com` | `user1234` |

- Los templates se recargan sin reiniciar (`spring.thymeleaf.cache=false`).
- La app lee el `.env` del proyecto: usuario, contraseña y puerto de MySQL (`DB_*`), `APP_PORT`,
  `JWT_SECRET`, etc. Sin `.env` usa los mismos defaults que `docker-compose.yml`. Las variables de
  entorno del sistema (o de la *Run Configuration* de IntelliJ) tienen prioridad sobre el `.env`.
- Sin `JWT_SECRET` usa un secreto de desarrollo fijo. Si lo definís en `.env`, tiene que tener
  32 caracteres o más (también en dev).
- CORS permite `localhost:5173`, `:3000` y `:4200` (Vite, CRA, Angular).

> **Dos proyectos del kit a la vez:** poné distinto `DB_PORT` (3307, 3308...) y `APP_PORT`
> (8080, 8081...) en el `.env` de cada uno.

### Probar la API rápido

**Con Swagger UI** (<http://localhost:8080/swagger-ui.html>):

1. Abrir `POST /api/auth/login` → *Try it out* → body
   `{"email":"admin@app.com","contrasena":"admin123"}` → *Execute*.
2. Copiar el `token` de la respuesta.
3. Botón **Authorize** (arriba a la derecha) → pegar el token (sin `Bearer`) → *Authorize*.
4. Ya podés ejecutar cualquier endpoint protegido. El token se recuerda al recargar la página.

El JSON de la especificación está en <http://localhost:8080/v3/api-docs>: se puede importar
en Postman o Insomnia (*Import → URL*) o usar para generar el cliente del frontend.

**Con curl:**

```bash
# Login → token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@app.com","contrasena":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# Usar el token
curl http://localhost:8080/api/usuarios/me -H "Authorization: Bearer $TOKEN"
curl "http://localhost:8080/api/admin/usuarios?rol=USER&page=0&size=10" -H "Authorization: Bearer $TOKEN"
```

---

## 5. Elegir el modo: MVC, API o ambos

Por defecto el kit trae **ambos**. Las dos capas comparten entidades, repositorios y servicios;
solo cambian los controllers y la cadena de seguridad.

```
                ┌─────────────── Service (lógica) ───────────────┐
 Navegador ──▶  controller/        (Thymeleaf, sesión, CSRF)      │──▶ Repository ──▶ MySQL
 Front/App ──▶  controller/api/    (JSON, JWT)                    │
                └────────────────────────────────────────────────┘
```

### 5.1 Solo API REST (backend para SPA o mobile)

1. **Borrar** estos archivos y carpetas:
   ```
   src/main/resources/templates/
   src/main/resources/static/
   src/main/java/.../controller/AuthController.java
   src/main/java/.../controller/HomeController.java
   src/main/java/.../controller/GlobalModelAdvice.java
   src/main/java/.../controller/admin/
   src/main/java/.../exception/GlobalExceptionHandler.java
   src/test/java/.../MvcIntegrationTest.java
   ```
2. **`pom.xml`**: borrar las dependencias `spring-boot-starter-thymeleaf`,
   `thymeleaf-layout-dialect` y `thymeleaf-extras-springsecurity6`.
3. **`application*.properties`**: borrar las líneas `spring.thymeleaf.*` y
   `server.servlet.session.cookie.*`.
4. **`SecurityConfig.java`**: reemplazar el método `mvcSecurityFilterChain` y borrar
   `authenticationSuccessHandler` por esto:
   ```java
   // CHAIN 2: todo lo que no es /api/** — solo health y archivos subidos
   @Bean
   @Order(2)
   public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
       http
               .csrf(csrf -> csrf.disable())
               .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
               .authorizeHttpRequests(auth -> auth
                       .requestMatchers("/actuator/health", "/error", "/uploads/**").permitAll()
                       .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                       .anyRequest().denyAll()
               );
       return http.build();
   }
   ```
   y borrar los imports que queden sin usar.
5. `./mvnw test`.

> Si la API la consume un frontend en **otro dominio**, configurá `CORS_ALLOWED_ORIGINS`
> (ver [7.4](#74-cors)).

### 5.2 Solo MVC (sitio web con panel admin)

1. **Borrar**:
   ```
   src/main/java/.../controller/api/
   src/main/java/.../security/          (JwtUtils, JwtAuthenticationFilter, ApiSecurityHandlers)
   src/main/java/.../dto/request/LoginRequest.java
   src/main/java/.../dto/request/EstadoRequest.java
   src/main/java/.../dto/response/
   src/main/java/.../exception/ApiExceptionHandler.java
   src/main/java/.../config/OpenApiConfig.java      (Swagger)
   src/test/java/.../ApiIntegrationTest.java
   ```
2. **`pom.xml`**: borrar las dependencias `com.auth0:java-jwt` y
   `springdoc-openapi-starter-webmvc-ui` (y la property `springdoc.version`).
3. **`application*.properties`** y **`.env.example`**: borrar las líneas `app.jwt.*`,
   `app.cors.*`, `springdoc.*`, `JWT_*`, `CORS_*` y `SWAGGER_*`. En `docker-compose.yml` borrar
   `JWT_SECRET`, `JWT_EXPIRATION_HOURS`, `CORS_ALLOWED_ORIGINS` y `SWAGGER_ENABLED` del servicio `app`.
4. **`SecurityConfig.java`**: borrar
   - los campos `jwtAuthenticationFilter`, `apiSecurityHandlers` y `corsAllowedOrigins`,
   - los métodos `apiSecurityFilterChain`, `corsConfigurationSource` y `jwtFilterRegistration`,
   - en `mvcSecurityFilterChain`, la línea de `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`,
   - los imports que queden sin usar.

   El bean `authenticationManager` puede quedar o borrarse (solo lo usaba la API).
5. `./mvnw test`.

---

## 6. Receta: agregar un recurso nuevo (paso a paso)

Ejemplo completo: un catálogo de **productos**. Seguí los pasos en orden; cada uno compila.
Los usuarios (`Usuario`) ya implementan todo este patrón, así que podés copiar de ahí.

### Paso 1 — Migración Flyway

`src/main/resources/db/migration/V2__crear_productos.sql`

```sql
CREATE TABLE productos (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(120)  NOT NULL,
    slug           VARCHAR(140)  NOT NULL,
    descripcion    TEXT,
    precio         DECIMAL(12,2) NOT NULL,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en      DATETIME(6),
    actualizado_en DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_productos_slug UNIQUE (slug)
);
```

> Reglas: número siguiente (`V2`, `V3`...), **nunca editar una migración ya aplicada**,
> nombres de columnas en `snake_case` (Hibernate convierte `creadoEn` → `creado_en`).

### Paso 2 — Entidad

`entity/Producto.java`

```java
@Entity
@Table(name = "productos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto extends BaseAuditable {          // creadoEn / actualizadoEn automáticos

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
```

Si la entidad no coincide con la migración, la app **no arranca** (`ddl-auto=validate`)
y los tests fallan: así te enterás antes de llegar a producción.

### Paso 3 — Repositorio

```java
public interface ProductoRepository extends JpaRepository<Producto, Long>,
                                            JpaSpecificationExecutor<Producto> {
    Optional<Producto> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
```

### Paso 4 — Specification (filtros)

```java
public final class ProductoSpecification {
    private ProductoSpecification() {}

    public static Specification<Producto> filtrar(String texto, BigDecimal precioMax, Boolean activo) {
        Specification<Producto> spec = Specification.unrestricted();
        if (texto != null && !texto.isBlank()) {
            String patron = "%" + texto.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("nombre")), patron));
        }
        if (precioMax != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("precio"), precioMax));
        }
        if (activo != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("activo"), activo));
        }
        return spec;
    }
}
```

> Si el texto viene del usuario, escapá `%` y `_` como en `UsuarioSpecification.contiene()`.

### Paso 5 — DTOs

```java
// dto/request/ProductoRequest.java — lo que entra (form MVC o JSON)
@Data
public class ProductoRequest {
    @NotBlank @Size(max = 120)
    private String nombre;
    private String descripcion;
    @NotNull @DecimalMin("0.00")
    private BigDecimal precio;
}

// dto/response/ProductoResponse.java — lo que sale por la API
public record ProductoResponse(Long id, String nombre, String slug, String descripcion,
                               BigDecimal precio, boolean activo) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getSlug(),
                p.getDescripcion(), p.getPrecio(), p.isActivo());
    }
}
```

> **Nunca devuelvas entidades JPA directo en la API**: exponés campos internos y el contrato
> cambia cada vez que cambia la tabla.

### Paso 6 — Service

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;

    public Page<Producto> buscar(String texto, BigDecimal precioMax, Boolean activo, Pageable pageable) {
        return productoRepository.findAll(ProductoSpecification.filtrar(texto, precioMax, activo), pageable);
    }

    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));   // → 404
    }

    @Transactional
    public Producto crear(ProductoRequest req) {
        String slug = SlugUtil.toSlug(req.getNombre());
        if (productoRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre");      // → 400
        }
        return productoRepository.save(Producto.builder()
                .nombre(req.getNombre()).slug(slug)
                .descripcion(req.getDescripcion()).precio(req.getPrecio())
                .build());
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest req) {
        Producto p = obtenerPorId(id);
        p.setNombre(req.getNombre());
        p.setDescripcion(req.getDescripcion());
        p.setPrecio(req.getPrecio());
        return p;                       // dirty checking: se guarda al cerrar la transacción
    }

    @Transactional
    public void eliminar(Long id) {
        productoRepository.delete(obtenerPorId(id));
    }
}
```

Convención de errores en los services (ver [sección 8](#8-manejo-de-errores)):
`ResourceNotFoundException` → 404, `IllegalArgumentException` → 400 con el mensaje.

### Paso 7a — Controller API (si usás API)

```java
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoApiController {

    private final ProductoService productoService;

    @GetMapping
    public PagedModel<ProductoResponse> listar(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) BigDecimal precioMax,
                                               @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return new PagedModel<>(productoService.buscar(q, precioMax, true, pageable).map(ProductoResponse::from));
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return ProductoResponse.from(productoService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        Producto p = productoService.crear(req);
        return ResponseEntity.created(URI.create("/api/productos/" + p.getId()))
                .body(ProductoResponse.from(p));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return ProductoResponse.from(productoService.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Paso 7b — Controller + vistas MVC (si usás MVC)

```java
@Controller
@RequestMapping("/admin/productos")
@RequiredArgsConstructor
public class AdminProductoController {

    private final ProductoService productoService;

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @PageableDefault(size = 20, sort = "nombre") Pageable pageable, Model model) {
        model.addAttribute("pagina", productoService.buscar(q, null, null, pageable));
        model.addAttribute("q", q);
        return "pages/admin/productos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("productoRequest", new ProductoRequest());
        return "pages/admin/producto-form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute ProductoRequest productoRequest, BindingResult result,
                        RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "pages/admin/producto-form";        // vuelve al form mostrando los errores
        }
        productoService.crear(productoRequest);
        ra.addFlashAttribute("success", "Producto creado.");
        return "redirect:/admin/productos";            // POST → Redirect → GET
    }
}
```

Templates: copiá `templates/pages/admin/usuarios.html` como base del listado y
`templates/pages/registro.html` como base del formulario (`th:object`, `th:field`, `th:errors`).
Agregá el link en el sidebar de `templates/layout/admin-base.html`.

Las páginas públicas usan `layout:decorate="~{layout/base}"`; las del panel,
`layout:decorate="~{layout/admin-base}"`.

### Paso 8 — Reglas de seguridad

- Todo lo que está bajo `/admin/**` y `/api/admin/**` ya exige rol ADMIN.
- Para páginas **públicas** nuevas (ej. catálogo `/productos/**`), agregalas al `permitAll()`
  de la cadena MVC en `SecurityConfig`:
  ```java
  .requestMatchers("/", "/error", "/productos/**").permitAll()
  ```
- Para endpoints **públicos** de la API (ej. `GET /api/productos`), agregalos en la cadena API:
  ```java
  .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
  ```
- Para reglas por método, usá `@PreAuthorize("hasRole('ADMIN')")` en el controller.

### Paso 9 — Tests

Copiá un test de `ApiIntegrationTest` o `MvcIntegrationTest` y adaptalo. Como mínimo:
el listado responde 200, crear con datos inválidos da 400, y un USER no puede crear (403).

---

## 7. Seguridad

### 7.1 Dos cadenas de filtros

| | **API** (`/api/**`) | **MVC** (todo lo demás) |
|---|---|---|
| Autenticación | `Authorization: Bearer <jwt>` | Form login (`/login`) + cookie de sesión |
| Estado | Stateless (sin sesión) | Sesión HTTP |
| CSRF | Desactivado (no hay cookies) | **Activado** |
| CORS | Configurable | No aplica |
| Sin autenticar | `401` JSON | Redirect a `/login` |
| Sin permisos | `403` JSON | Página `error.html` con 403 |
| Rutas públicas | `/api/auth/**` | `/`, `/login`, `/registro`, `/css/**`, `/uploads/**`, `/actuator/health`, Swagger (si está prendido) |
| Solo ADMIN | `/api/admin/**` | `/admin/**` |

### 7.2 JWT

- Se firma con HS256 usando `JWT_SECRET`. **La app no arranca si el secreto tiene menos de 32 caracteres.**
- Expira en `JWT_EXPIRATION_HOURS` (default 24h). No hay refresh token: al expirar, el cliente vuelve a hacer login.
- En cada request se recarga el usuario desde la base: si se lo **desactiva** o se le
  **cambia el rol**, el cambio aplica inmediatamente aunque el token siga vigente.
- Cambiar `JWT_SECRET` invalida todos los tokens emitidos.
- El token incluye el claim `rol` (`ADMIN`, `USER`): el mismo valor que devuelven `/api/auth/login`
  y `/api/usuarios/me`.

### 7.3 Usuario actual en un controller

```java
@GetMapping("/algo")
public X algo(@AuthenticationPrincipal UserDetails principal) {
    String email = principal.getUsername();
    ...
}
```

En Thymeleaf: `sec:authorize="hasRole('ADMIN')"`, `sec:authentication="name"` o `${#authentication.name}`.

### 7.4 CORS

Solo hace falta si un **navegador** llama a la API desde **otro dominio**
(ej. frontend en `app.cliente.com`, API en `api.cliente.com`).

```bash
CORS_ALLOWED_ORIGINS=https://app.cliente.com,https://admin.cliente.com
```

Apps mobile, Postman y llamadas servidor-a-servidor no usan CORS.

### 7.5 CSRF con HTMX / fetch

Los layouts incluyen el token CSRF en `<meta name="_csrf">` y lo agregan automáticamente a
cada request de **HTMX**. Para `fetch` manual:

```js
const token = document.querySelector("meta[name='_csrf']").content;
const header = document.querySelector("meta[name='_csrf_header']").content;
fetch('/algo', { method: 'POST', headers: { [header]: token } });
```

Los formularios con `th:action` ya incluyen el token solos.

### 7.6 Roles

Están en `enums/Rol.java` (`ADMIN`, `USER`). Para agregar uno (ej. `VENDEDOR`), sumalo al enum;
la columna es `VARCHAR`, no hace falta migración. Usalo con `hasRole('VENDEDOR')`.

### 7.7 Registro público

El kit permite que cualquiera se registre (rol `USER`). Si el sistema del cliente es cerrado
(los usuarios los crea el admin):

- **MVC**: borrar los métodos `/registro` de `AuthController`, `templates/pages/registro.html`,
  los links a `/registro` (header, home, login) y `"/registro"` del `permitAll()` en `SecurityConfig`.
- **API**: borrar el método `registro` de `AuthApiController`.

---

## 8. Manejo de errores

Convención: los **services lanzan excepciones**, los handlers las traducen.

| Excepción | API (`ApiExceptionHandler`) | MVC (`GlobalExceptionHandler`) |
|---|---|---|
| `ResourceNotFoundException` | `404` JSON | `error.html` con 404 |
| `IllegalArgumentException` (regla de negocio) | `400` JSON con el mensaje | Vuelve a la página anterior con el mensaje en `mensajeError` |
| `@Valid` falla (body o parámetros) | `400` JSON con `detalles` | Usar `BindingResult` en el controller |
| JSON malformado / parámetro inválido / `sort` por un campo inexistente | `400` | `400` |
| Otros errores del cliente que detecta Spring (parámetro obligatorio faltante, `Content-Type` no soportado, archivo demasiado grande...) | Su código (`400`, `415`, `413`...) | Su código, con `error.html` |
| `DataIntegrityViolationException` | `409` | `500` |
| Cualquier otra | `500` genérico (el detalle solo va al log) | `500` genérico |

Las rutas o métodos HTTP que no existen dentro de `/api/**` (`404`/`405`) los responde Spring Boot
con su formato estándar: `{"timestamp", "status", "error", "path"}`.

Formato JSON de error (`MensajeDTO`):

```json
{ "tipo": "ERROR", "mensaje": "Errores de validación", "detalles": ["El email no es válido"] }
```

Las alertas MVC se muestran con el fragmento `fragments/alertas.html` (flash attributes
`success`, `error`, `mensajeError`, `info`). El panel admin ya lo incluye en el layout.

---

## 9. Base de datos y migraciones

- El esquema lo maneja **Flyway**: `src/main/resources/db/migration/V<n>__descripcion.sql`.
- Hibernate **solo valida** (`ddl-auto=validate`) en todos los entornos: si una entidad no
  coincide con las tablas, la app no arranca.
- Los tests corren las mismas migraciones sobre H2 en modo MySQL, así que una migración
  faltante se detecta con `./mvnw test`.
- **Nunca** edites una migración ya aplicada en algún entorno; creá una nueva.
- Datos que deben existir en todos los entornos (provincias, categorías fijas): migración
  (`V3__datos_iniciales.sql`). Datos de prueba: `DataInitializer.crearDatosDemo()` (solo dev, y solo
  si la base está vacía).

Para empezar de cero en dev (borra todos los datos):

```bash
docker compose down -v && docker compose up db -d
```

---

## 10. Subida de archivos

`FileUploadService` valida tamaño (5MB), extensión, MIME type y que los *magic bytes*
coincidan con la extensión (un PDF renombrado a `.jpg` se rechaza). Guarda con nombre UUID
y devuelve la URL pública `/uploads/<uuid>.<ext>`.

```java
@PostMapping("/admin/productos/{id}/imagen")
public String subirImagen(@PathVariable Long id, @RequestParam MultipartFile archivo) throws IOException {
    String url = fileUploadService.guardarArchivo(archivo);   // IllegalArgumentException si no es válido
    productoService.actualizarImagen(id, url);
    return "redirect:/admin/productos";
}
```

```html
<form th:action="@{/admin/productos/{id}/imagen(id=${p.id})}" method="post" enctype="multipart/form-data">
    <input type="file" name="archivo" accept="image/*">
    <button type="submit">Subir</button>
</form>
```

- Formatos permitidos: `FORMATOS_PERMITIDOS` en `FileUploadService` (jpg, png, webp, gif, pdf).
- Para borrar: `fileUploadService.eliminarArchivo(url)`.
- **Todo `/uploads/**` es público.** No lo uses para documentos privados de usuarios
  (para eso: guardarlos fuera de `/uploads` y servirlos desde un controller que verifique permisos).
- En Docker los archivos se guardan en el volumen `uploads_data`.

---

## 11. Tests

```bash
./mvnw test
```

| Test | Qué cubre |
|---|---|
| `ApiIntegrationTest` | Registro, login JWT, 401/403 JSON, filtros y paginación, usuario desactivado, errores 400/415, CORS, Swagger |
| `MvcIntegrationTest` | Páginas públicas, form login y redirecciones, CSRF, panel admin, registro |
| `FileUploadServiceTest` | Validación de formatos, magic bytes, path traversal |
| `SlugUtilTest` | Slugs con acentos, símbolos y espacios |
| `ApplicationTests` | Que el contexto levante con las migraciones |

Usan H2 en memoria (perfil `test`), no necesitan Docker.

---

## 12. Deploy a producción

### 12.1 Con Docker Compose (VPS)

```bash
cp .env.example .env            # completar TODO (ver sección 13)
docker compose up -d --build
docker compose logs -f app      # verificar que arrancó
```

En el primer arranque:
1. Flyway crea las tablas.
2. Si no hay ningún ADMIN, se crea con `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

Si falta `JWT_SECRET` (o tiene menos de 32 caracteres), la app no arranca y lo dice en el log.

### 12.2 HTTPS (obligatorio)

La app escucha HTTP en el 8080; poné adelante un proxy reverso con HTTPS (Nginx + Certbot,
Caddy, Traefik o Cloudflare). La app ya respeta los headers `X-Forwarded-*`
(`server.forward-headers-strategy=native`).

Ejemplo mínimo con **Caddy** (HTTPS automático):

```
cliente.com {
    reverse_proxy localhost:8080
}
```

Con `COOKIE_SECURE=true` (default en prod), la cookie de sesión **solo viaja por HTTPS**:
sin HTTPS el login MVC no funciona. Para probar el stack prod en tu máquina sin HTTPS,
poné `COOKIE_SECURE=false` en `.env`.

### 12.3 Checklist antes de entregar

- [ ] `JWT_SECRET` generado con `openssl rand -base64 48` (único por cliente)
- [ ] `DB_PASS` y `DB_ROOT_PASS` fuertes
- [ ] `ADMIN_PASSWORD` fuerte (y cambiado después del primer login si lo compartiste)
- [ ] HTTPS funcionando y `COOKIE_SECURE=true`
- [ ] `CORS_ALLOWED_ORIGINS` solo con los dominios reales (o vacío)
- [ ] `SWAGGER_ENABLED=false` (o sin definir): `https://cliente.com/swagger-ui.html` debe dar 404
- [ ] Sin Docker Compose: `SPRING_PROFILES_ACTIVE=prod` (sin eso arranca en dev, con el secreto JWT de desarrollo)
- [ ] `TZ` con la zona horaria del cliente (default `America/Argentina/Buenos_Aires`)
- [ ] `./mvnw test` en verde
- [ ] Backups de la base (desde la carpeta del proyecto):
  ```bash
  # backup
  docker compose exec -T db sh -c 'exec mysqldump --single-transaction -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > backup.sql
  # restaurar
  docker compose exec -T db sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < backup.sql
  ```
- [ ] Health check: `curl https://cliente.com/actuator/health` → `{"status":"UP"}`

### 12.4 Sin Docker (jar)

```bash
./mvnw clean package
SPRING_PROFILES_ACTIVE=prod DATABASE_URL=jdbc:mysql://host:3306/db DB_USER=... DB_PASS=... \
JWT_SECRET=... java -jar target/app-0.0.1-SNAPSHOT.jar
```

---

## 13. Variables de entorno

| Variable | Obligatoria en prod | Default | Descripción |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | **sí**: `prod` (compose ya lo pone) | `dev` | Perfil activo |
| `DATABASE_URL` | sí (compose la arma) | dev: `jdbc:mysql://localhost:<DB_PORT>/<DB_NAME>` | URL JDBC |
| `DB_NAME` | — | `starter_db` | Nombre de la base (compose y dev) |
| `DB_USER` / `DB_PASS` | sí | `appuser` / `apppass` | Usuario de la app en MySQL |
| `DB_ROOT_PASS` | sí | `rootpass` | Root de MySQL (compose) |
| `JWT_SECRET` | **sí** (mín. 32 caracteres) | dev: secreto fijo | Firma de tokens |
| `JWT_EXPIRATION_HOURS` | — | `24` | Duración del token |
| `JWT_ISSUER` | — | `spring.application.name` | Claim `iss` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | primer deploy | vacío | Admin inicial si no existe ninguno |
| `CORS_ALLOWED_ORIGINS` | — | sin definir (dev: localhost) | Orígenes permitidos para la API |
| `APP_NOMBRE` | — | `Mi App` | Nombre visible |
| `COOKIE_SECURE` | — | `true` | Cookie de sesión solo por HTTPS |
| `SWAGGER_ENABLED` | — | dev: siempre / prod: `false` | Habilita `/swagger-ui.html` y `/v3/api-docs` en prod |
| `UPLOAD_DIR` | — | `uploads` | Carpeta de archivos subidos |
| `APP_PORT` / `DB_PORT` | — | `8080` / `3307` | Puertos de la app y de MySQL (compose y dev) |
| `TZ` | — | `America/Argentina/Buenos_Aires` | Zona horaria de la app y de MySQL (compose) |
| `COMPOSE_PROJECT_NAME` | — | nombre de la carpeta | Prefijo de contenedores y volúmenes |

---

## 14. Referencia de la API incluida

La referencia completa y siempre actualizada está en **Swagger UI**: <http://localhost:8080/swagger-ui.html>
(se genera sola a partir de los controllers de `/api/**`; en prod está apagado salvo `SWAGGER_ENABLED=true`).

Para mejorar lo que muestra, en los controllers podés usar (opcional):

```java
@Tag(name = "Productos", description = "ABM de productos")          // en la clase
@Operation(summary = "Lista productos con filtros y paginación")    // en el método
```
(`io.swagger.v3.oas.annotations.*`). Sin anotaciones igual se documentan todos los endpoints,
parámetros y DTOs; las validaciones (`@NotBlank`, `@Size`...) aparecen como restricciones.

Configuración: `config/OpenApiConfig.java` (título, esquema Bearer) y `springdoc.*` en
`application.properties`.

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/registro` | — | Crea un usuario (rol USER). `201` + usuario |
| `POST` | `/api/auth/login` | — | Devuelve el token JWT |
| `GET` | `/api/usuarios/me` | token | Datos del usuario autenticado |
| `GET` | `/api/admin/usuarios` | ADMIN | Listado paginado. Filtros: `q`, `rol`, `activo`. Paginación: `page`, `size`, `sort=campo,asc` |
| `GET` | `/api/admin/usuarios/{id}` | ADMIN | Detalle |
| `PATCH` | `/api/admin/usuarios/{id}/estado` | ADMIN | Body `{"activo": false}` — activa/desactiva |
| `GET` | `/actuator/health` | — | Estado de la app |

**Login:**

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "admin@app.com", "contrasena": "admin123" }
```

```json
{ "token": "eyJ...", "tipo": "Bearer", "expiraEnSegundos": 86400, "email": "admin@app.com", "rol": "ADMIN" }
```

**Respuesta paginada:**

```json
{
  "content": [ { "id": 2, "nombre": "Usuario", "email": "usuario@app.com", "rol": "USER", "activo": true, ... } ],
  "page": { "size": 20, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

---

## 15. Estructura del proyecto

```
src/main/java/com/tudominio/app/
├── config/            SecurityConfig (2 cadenas), DataInitializer (admin + demo), WebConfig (uploads), CacheConfig, OpenApiConfig (Swagger)
├── controller/        MVC: Home, Auth (login/registro), GlobalModelAdvice (appNombre, requestURI)
│   ├── admin/         Panel admin: Dashboard, Usuarios (plantilla de ABM MVC)
│   └── api/           REST: Auth (login/registro), Usuario (/me), AdminUsuario (plantilla de recurso API)
├── dto/               MensajeDTO (formato de mensajes/errores)
│   ├── request/       Lo que entra, con Bean Validation
│   └── response/      Lo que sale por la API (records)
├── entity/            Entidades JPA (extienden BaseAuditable)
├── enums/             Rol
├── exception/         ApiExceptionHandler (JSON) y GlobalExceptionHandler (vistas)
├── repository/        Spring Data JPA + JpaSpecificationExecutor
├── security/          JwtUtils, JwtAuthenticationFilter, ApiSecurityHandlers (401/403 JSON)
├── service/           Lógica de negocio (compartida por MVC y API)
├── specification/     Filtros dinámicos combinables
└── util/              Fechas, números (formato AR), strings, slugs, passwords

src/main/resources/
├── db/migration/      Migraciones Flyway (V1__crear_usuarios.sql, ...)
├── static/css/        global.css (sitio), auth.css (login/registro), admin.css (panel)
├── templates/
│   ├── error.html     Página de error única (404, 403, 500...)
│   ├── fragments/     header, footer, alertas
│   ├── layout/        base.html (sitio), admin-base.html (panel)
│   └── pages/         home, login, registro, admin/dashboard, admin/usuarios
├── application.properties        Común
├── application-dev.properties    Desarrollo
└── application-prod.properties   Producción (todo por variables de entorno)
```

---

## 16. Problemas comunes

| Síntoma | Causa / solución |
|---|---|
| `Communications link failure` al arrancar | MySQL no está levantado (`docker compose up db -d` y esperar unos segundos) o `DB_PORT` no coincide |
| `Access denied for user 'appuser'` | MySQL guarda usuario y contraseña al crear el volumen: cambiaste `DB_USER`/`DB_PASS` en `.env` después. Recreá la base (borra los datos): `docker compose down -v && docker compose up db -d` |
| `port is already allocated` al levantar MySQL | Otro proyecto usa ese puerto: frenalo (`docker compose stop` en su carpeta) o cambiá `DB_PORT` en `.env` |
| `Port 8080 was already in use` | Otra app usa el puerto: cambiá `APP_PORT` en `.env` |
| `Schema-validation: missing table [...]` | Falta la migración Flyway de esa entidad |
| `Schema-validation: wrong column type` | El tipo en el SQL no coincide con la entidad (ej. `INT` vs `Long` → usar `BIGINT`) |
| `Migration checksum mismatch` | Editaste una migración ya aplicada. Revertí el cambio y creá una nueva (en dev podés hacer `docker compose down -v`) |
| `app.jwt.secret (JWT_SECRET) debe tener al menos 32 caracteres` | Generá uno: `openssl rand -base64 48` (también pasa en dev si `JWT_SECRET` está vacío o es corto en `.env`) |
| Login MVC no mantiene la sesión en prod | Estás sin HTTPS con `COOKIE_SECURE=true` |
| `403` en un POST de formulario | Falta el token CSRF: usá `th:action` (no `action`) o el header en fetch/HTMX |
| La API responde `403` CORS en el navegador | Agregá el origen del frontend a `CORS_ALLOWED_ORIGINS` |
| Lombok: `cannot find symbol getXxx()` en IntelliJ | *Settings → Build → Compiler → Annotation Processors → Enable* |
| `./mvnw: Permission denied` | `chmod +x mvnw` |
