# NOTAS_PROYECTO.md — Ideal-Trip

> Documento maestro del proyecto. Su propósito es que cualquier nuevo chat de Claude (o el propio desarrollador después de un tiempo sin tocar el código) pueda retomar el trabajo sin perder contexto. **Mantener actualizado tras cada hito.**

---

## 1. Resumen del proyecto

**Nombre:** Ideal-Trip
**Tipo:** Aplicación web de agencia de viajes
**Estado:** App funcionalmente completa. Lotes 1, 2, 3 y 4 cerrados. **Lote 5 (JWT REST API) en marcha:** login JWT y endpoints públicos REST funcionando. Pulido y tests pendientes.
**Estudiante:** Felipe (u20241222745@usco.edu.co)
**Repositorio local:** `C:\Users\Andrés Felipe\OneDrive\Documents\Proyecto Programación WEB\ideal_trip`

Permite a usuarios explorar destinos turísticos, ver paquetes asociados, registrarse y realizar reservas. Tiene un panel administrativo (a futuro) para gestionar destinos, paquetes y reservas.

---

## 2. Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 3.4.5 |
| Build | Maven | (wrapper incluido) |
| Persistencia | Spring Data JPA + Hibernate | Auto |
| Base de datos | MySQL | 8.x |
| Vistas | Thymeleaf | Auto |
| Seguridad | Spring Security + BCrypt | Auto |
| Integración Thymeleaf ↔ Security | thymeleaf-extras-springsecurity6 | 3.1.2.RELEASE |
| JWT (API REST) | jjwt-api + jjwt-impl + jjwt-jackson | 0.12.6 |
| Validación | Spring Boot Starter Validation | Auto |
| Utilidades | Lombok | Versión del BOM |
| Estilos | CSS propio + Bootstrap 5 (CDN, cargado globalmente) | — |
| Logging | SLF4J + Logback (default de Spring Boot) | — |
| Testing | spring-boot-starter-test + spring-security-test + H2 | Auto |

---

## 3. Decisiones de diseño tomadas

### Arquitectura
- **Estilo:** por capas (no vertical slice).
- **Paquetes:** `model`, `model.enums`, `repository`, `service`, `controller`, `controller.api`, `dto`, `dto.api`, `config`, `security`, `exception`.
- **Estilo de controladores:** **híbrido**. MVC con Thymeleaf bajo `/` (web) y REST JSON bajo `/api/**` (API stateless con JWT).
- **Separación física:** los controladores REST viven en `controller/api/` y sus DTOs de respuesta/petición en `dto/api/`. Esto deja claro qué clases pertenecen al mundo "servlet con sesión" y cuáles al mundo "stateless JSON".

### Convenciones de código
- **Inyección:** por constructor, con `@RequiredArgsConstructor` de Lombok y campos `final`.
- **Lombok en entidades:** anotaciones granulares (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@ToString(exclude=...)`, `@EqualsAndHashCode(of="id")`). **NO usar `@Data`** porque rompe `equals/hashCode/toString` con relaciones JPA.
- **Lombok en DTOs / configuración:** `@Data` o `record` permitidos.
- **Servicios:** `@Transactional(readOnly = true)` a nivel de clase; `@Transactional` simple en métodos que escriben.
- **Logging:** `@Slf4j` + placeholders `{}` (no concatenación con `+`).
- **Nombres de tablas:** plural en español, minúsculas (`usuarios`, `destinos`, `paquetes`, `reservas`).
- **Nombres de columnas:** snake_case (`fecha_registro`, `imagen_url`).

### Modelo de dominio
- **Entidades base (4):** `Usuario`, `Destino`, `Paquete`, `Reserva`.
- **Roles:** enum simple `Rol { CLIENTE, ADMIN }` dentro de `Usuario`. **No** se usa entidad `Rol` separada ni ManyToMany.
- **Estados de reserva:** enum `EstadoReserva { PENDIENTE, CONFIRMADA, CANCELADA, COMPLETADA }`.
- **Soft delete:** campo `boolean activo` en `Usuario`, `Destino` y `Paquete`. NO se borra físicamente para preservar integridad histórica de reservas.
- **Dinero:** `BigDecimal` con `precision=12, scale=2`. Nunca `double`/`float`.
- **Fechas:** `LocalDateTime` (incluyendo `Reserva.fechaViaje`) y `LocalDate` según el caso. Nunca `java.util.Date`.
- **Relaciones LAZY** por defecto en `@ManyToOne`; bidireccionales con `mappedBy` y excluidas de `@ToString`.
- **Cascade ALL** del lado padre (OneToMany); orphanRemoval activo.
- **Enums** persisten con `EnumType.STRING` (no ORDINAL).
- **Campo de notas en `Reserva`:** se llama `descripcion` (max 500), NO `observaciones`.

### Excepciones
- **Excepción común:** `RecursoNoEncontradoException` en paquete `exception/`.
- **Constructor sugar:** `new RecursoNoEncontradoException("Paquete", id)` produce mensaje formateado.
- **`GlobalExceptionHandler`** con `@ControllerAdvice` ya implementado (mundo HTML / MVC):
  - `RecursoNoEncontradoException` → 404 (`error/404.html`)
  - `IllegalArgumentException` → 400 (`error/400.html`)
  - `IllegalStateException` → 409 (`error/409.html`)
  - Cualquier otra `Exception` → 500 (`error/500.html`)
- **`ApiExceptionHandler`** con `@RestControllerAdvice(basePackages = "com.web.spring.ideal_trip.controller.api")` y `@Order(Ordered.HIGHEST_PRECEDENCE)` (mundo REST / JSON):
  - `BadCredentialsException` → 401 JSON
  - `RecursoNoEncontradoException` → 404 JSON
  - `MethodArgumentNotValidException` → 400 JSON con mapa `campo → mensaje`
  - `Exception` genérico → 500 JSON
- **Por qué dos handlers:** el `@ControllerAdvice` del mundo Thymeleaf devolvería HTML aunque la petición fuera a `/api/**`. Limitar el `@RestControllerAdvice` al paquete `controller.api` con `basePackages` evita que se "coma" excepciones del lado MVC.
- Las plantillas HTML usan `${mensaje} ?: 'fallback'` (operador Elvis) para servir tanto a las excepciones controladas como a las URLs sin handler (que Spring Boot resuelve directo contra `templates/error/{code}.html`).

### Seguridad (ya configurada)
- `BCryptPasswordEncoder` registrado **dentro de `SecurityConfig`** como `@Bean`. El antiguo `PasswordEncoderConfig.java` fue eliminado para tener todo en un solo lugar.
- `SecurityConfig` en `config/` define **dos `SecurityFilterChain`** con `@Order` (multi-chain pattern):
  - **`apiFilterChain` (`@Order(1)`)** → `securityMatcher("/api/**")`:
    - CSRF desactivado.
    - `SessionCreationPolicy.STATELESS` (no se crea `HttpSession`).
    - Públicas: `/api/auth/**`, `/api/destinos/**`, `/api/paquetes/**`.
    - Todo lo demás bajo `/api/**` exige autenticación vía JWT.
    - `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`.
  - **`webFilterChain` (`@Order(2)`)** → resto de URLs:
    - Públicas: `/`, `/destinos/**`, `/paquetes/**`, `/login`, `/registro`, `/css/**`, `/js/**`, `/img/**`, `/error/**`, `/favicon.ico`.
    - `/admin/**` exige rol ADMIN.
    - Todo lo demás exige autenticación.
    - `formLogin` con `loginPage=/login`, `usernameParameter=email`, `defaultSuccessUrl=/`.
    - `logout` por POST a `/logout`, redirige a `/login?logout`.
    - CSRF activo (default, recomendado para formularios Thymeleaf — el token se inyecta automáticamente con `th:action`).
- **¿Por qué dos cadenas y no una?** `@Order` + `securityMatcher` permiten que cada mundo (web stateful con sesión / API stateless con JWT) tenga sus propias reglas de CSRF, sesión, autenticación y autorización sin interferirse. La primera cadena que "agarra" la request es la que la procesa.
- `CustomUserDetailsService` en `security/` carga usuarios desde `UsuarioRepository.findByEmail` y mapea `Rol` a `ROLE_CLIENTE`/`ROLE_ADMIN` con `.roles()`. Si `activo=false`, se devuelve como `disabled` y Spring bloquea el login. Lo usan ambas cadenas.
- **Auto-wiring:** Spring Security 6 auto-construye el `DaoAuthenticationProvider` al ver un `UserDetailsService` y un `PasswordEncoder` como beans. No hay que declararlo manualmente.

### JWT (Lote 5)
- **Librería:** `io.jsonwebtoken:jjwt-api:0.12.6` + `jjwt-impl` y `jjwt-jackson` con `<scope>runtime</scope>`.
- **`JwtService` en `security/`:**
  - Lee `app.jwt.secret` y `app.jwt.expiration-ms` de `application.properties` con `@Value`.
  - Construye la clave HMAC con `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))` (HS256).
  - `generarToken(email, rol)` → claim `rol` + subject email + issuedAt + expiration.
  - `extraerEmail(token)`, `extraerRol(token)`, `esValido(token)` con `Jwts.parser().verifyWith(signingKey).build()`.
- **`JwtAuthenticationFilter` en `security/`:**
  - Extiende `OncePerRequestFilter` (se ejecuta una vez por petición).
  - Lee header `Authorization: Bearer <token>`; si no está, `chain.doFilter` y sigue.
  - Si está, valida con `JwtService` y, si es válido, monta un `UsernamePasswordAuthenticationToken` con `ROLE_<rol>` y lo deja en el `SecurityContext`.
  - **No** se registra automáticamente como filtro de servlet: se declara un `FilterRegistrationBean` con `setEnabled(false)` para que solo lo enchufe `SecurityConfig`. Sin esto, Spring Boot lo registraría dos veces (una por la cadena de Security, otra como filtro global del servlet container) y el filtro correría en peticiones HTML también.
- **DTOs API en `dto/api/`:**
  - `LoginRequestDto` (`@Data`, con `@NotBlank` + `@Email`).
  - `LoginResponseDto` con `token`, `email`, `nombre`, `rol` y `expiraEn` (ms).
  - `DestinoResponseDto` para no exponer la entidad JPA ni sus relaciones.
- **`AuthApiController`** (`POST /api/auth/login`): valida credenciales con `PasswordEncoder.matches()` directamente sobre `UsuarioService.buscarPorEmail(...)`. Si no coincide o `!activo`, lanza `BadCredentialsException` (la captura `ApiExceptionHandler`). Si todo está bien, devuelve `LoginResponseDto` con el JWT.
- **`DestinoApiController`** (`GET /api/destinos`, `GET /api/destinos/{id}`): expone destinos activos como JSON. Sirve para probar el flujo end-to-end y verificar que `ApiExceptionHandler` devuelve 404 JSON.
- **Configuración:**
  ```properties
  app.jwt.secret=esta-es-una-clave-muy-larga-de-ejemplo-cambiar-en-produccion-por-favor-12345
  app.jwt.expiration-ms=86400000
  ```
- ⚠️ **Pendiente de seguridad:** mover `app.jwt.secret` a variable de entorno (`${JWT_SECRET}`) antes de cualquier despliegue real. La clave en `application.properties` es solo para desarrollo local.

### Vistas
- **Fragmentos compartidos** en `templates/fragments/layout.html` con tres fragmentos: `head(titulo)`, `navbar`, `footer`.
- **Bootstrap 5** cargado desde CDN dentro del fragmento `head` (global para todas las páginas).
- **`sec:authorize`** usado en el navbar para mostrar/ocultar "Iniciar sesión" / "Cerrar sesión" según autenticación.
- **Sintaxis Thymeleaf preferida:** literal substitution `|texto ${var}|` antes que concatenación `${var + ' texto'}`.
- **URLs:** siempre con `@{/ruta}` o `@{/ruta/{id}(id=${var})}`; nunca hardcoded `href`.
- **Hero con imagen dinámica:** `th:style="${entidad.imagenUrl != null} ? |background-image: url(${entidad.imagenUrl})| : ''"`.

### Formularios y validación
- **DTOs** en `dto/` para los formularios (no exponer entidades JPA directamente al navegador).
- **`@Data` permitido en DTOs** (no en entidades).
- **Anotaciones de validación** en el DTO: `@NotBlank`, `@Email`, `@Size`, etc., con `message` personalizado.
- **`@Valid` + `BindingResult`** en el controlador: `BindingResult` SIEMPRE va justo después del objeto que valida, sino Spring lanza excepción.
- **Validación cruzada o de unicidad:** dentro del controlador con `bindingResult.rejectValue(campo, código, mensaje)`.
- **Patrón POST-Redirect-GET:** tras un POST exitoso, redirigir (`return "redirect:/..."`) en vez de servir HTML directo. Evita re-envíos en F5.
- **Mensajes flash:** `RedirectAttributes.addFlashAttribute(...)` para enviar mensajes que sobreviven una redirección y desaparecen tras leerlos.
- **Plantillas con `th:object` + `th:field="*{campo}"`**: el `*{...}` es relativo al objeto y genera id, name, value de un golpe.
- **Mostrar errores:** `th:if="${#fields.hasErrors('campo')}"` + `th:errors="*{campo}"`.

---

## 4. Modelo de datos

```
Usuario  ──< Reserva >──  Paquete  >──  Destino
   1         N      N       1       N        1
   │
   └── tiene un Rol (enum)

Reserva tiene un EstadoReserva (enum)
```

### Tablas generadas por Hibernate (ddl-auto=update)

- `usuarios` — id, nombre, apellido, email (UK), password (BCrypt), telefono, rol, activo, fecha_registro
- `destinos` — id, nombre, pais, continente, descripcion (TEXT), imagen_url, precio_base, activo
- `paquetes` — id, nombre, tipo, descripcion (TEXT), incluye (TEXT), precio, duracion_dias, cupos_disponibles, activo, destino_id (FK)
- `reservas` — id, usuario_id (FK), paquete_id (FK), fecha_reserva, fecha_viaje, cantidad_personas, precio_total, estado, descripcion (max 500)

---

## 5. Estructura del proyecto

```
ideal_trip/
├── pom.xml                      ← Spring Boot 3.4.5, Java 21, security, thymeleaf-extras-security6
├── mvnw, mvnw.cmd
├── HELP.md                      ← (genérico de Spring Initializr, se puede borrar)
├── NOTAS_PROYECTO.md            ← ESTE ARCHIVO
└── src/
    ├── main/
    │   ├── java/com/web/spring/ideal_trip/
    │   │   ├── IdealTripApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java               (dos SecurityFilterChain: API + Web, PasswordEncoder bean)
    │   │   │   └── DataSeeder.java                   (CommandLineRunner idempotente)
    │   │   ├── controller/
    │   │   │   ├── HomeController.java               (GET /)
    │   │   │   ├── DestinoController.java            (GET /destinos, /destinos/{id})
    │   │   │   ├── PaqueteController.java            (GET /paquetes, /paquetes/{id})
    │   │   │   ├── AuthController.java               (GET /login, /registro; POST /registro)
    │   │   │   ├── ReservaController.java            (GET /reservas/nueva, /mis-reservas;
    │   │   │   │                                      POST /reservas, /reservas/{id}/cancelar)
    │   │   │   ├── AdminController.java              (CRUD destinos+paquetes, dashboard
    │   │   │   │                                      en /admin/**, solo ROLE_ADMIN)
    │   │   │   └── api/
    │   │   │       ├── AuthApiController.java        (POST /api/auth/login → JWT)
    │   │   │       └── DestinoApiController.java     (GET /api/destinos, /api/destinos/{id})
    │   │   ├── dto/
    │   │   │   ├── RegistroDto.java                  (form de registro)
    │   │   │   ├── ReservaDto.java                   (form de reserva)
    │   │   │   ├── DestinoDto.java                   (form admin de destino)
    │   │   │   ├── PaqueteDto.java                   (form admin de paquete)
    │   │   │   └── api/
    │   │   │       ├── LoginRequestDto.java          (email + password)
    │   │   │       ├── LoginResponseDto.java         (token + email + nombre + rol + expiraEn)
    │   │   │       └── DestinoResponseDto.java       (proyección JSON de Destino)
    │   │   ├── exception/
    │   │   │   ├── RecursoNoEncontradoException.java
    │   │   │   ├── GlobalExceptionHandler.java       (@ControllerAdvice — HTML)
    │   │   │   └── ApiExceptionHandler.java          (@RestControllerAdvice scoped a controller.api — JSON)
    │   │   ├── model/
    │   │   │   ├── Destino.java
    │   │   │   ├── Paquete.java
    │   │   │   ├── Reserva.java
    │   │   │   ├── Usuario.java
    │   │   │   └── enums/
    │   │   │       ├── EstadoReserva.java
    │   │   │       └── Rol.java
    │   │   ├── repository/
    │   │   │   ├── DestinoRepository.java
    │   │   │   ├── PaqueteRepository.java
    │   │   │   ├── ReservaRepository.java
    │   │   │   └── UsuarioRepository.java
    │   │   ├── security/
    │   │   │   ├── CustomUserDetailsService.java     (carga usuarios desde BD)
    │   │   │   ├── JwtService.java                   (genera/valida tokens HS256, JJWT 0.12.6)
    │   │   │   └── JwtAuthenticationFilter.java     (OncePerRequestFilter — popula SecurityContext)
    │   │   └── service/
    │   │       ├── DestinoService.java
    │   │       ├── PaqueteService.java
    │   │       ├── UsuarioService.java               (BCrypt integrado)
    │   │       └── ReservaService.java               (descuento/liberación de cupos)
    │   └── resources/
    │       ├── application.properties
    │       ├── static/
    │       │   └── css/styles.css
    │       └── templates/
    │           ├── fragments/
    │           │   └── layout.html                   (head, navbar, footer compartidos)
    │           ├── error/
    │           │   ├── 400.html
    │           │   ├── 404.html
    │           │   ├── 409.html
    │           │   └── 500.html
    │           ├── index.html                        (home — Thymeleaf)
    │           ├── destinos.html                     (listado — Thymeleaf)
    │           ├── destino-detalle.html              (detalle de destino)
    │           ├── paquetes.html                     (listado — Thymeleaf)
    │           ├── paquete-detalle.html              (detalle de paquete)
    │           ├── login.html                        (formulario de login)
    │           ├── registro.html                     (formulario de registro)
    │           ├── reserva-form.html                 (formulario nueva reserva)
    │           ├── mis-reservas.html                 (listado del cliente logueado)
    │           └── admin/
    │               ├── dashboard.html                (métricas)
    │               ├── destinos.html                 (tabla CRUD destinos)
    │               ├── destino-form.html             (crear/editar destino)
    │               ├── paquetes.html                 (tabla CRUD paquetes)
    │               └── paquete-form.html             (crear/editar paquete)
    └── test/
        └── java/com/web/spring/ideal_trip/
            └── IdealTripApplicationTests.java
```

---

## 6. Configuración actual

### `application.properties` (versión real, por secciones)

```properties
# APLICACIÓN
spring.application.name=ideal_trip

# SERVIDOR
server.port=8080

# BASE DE DATOS - MYSQL
spring.datasource.url=jdbc:mysql://localhost:3306/ideal_trip?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota&characterEncoding=UTF-8&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=<hardcoded>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / HIBERNATE
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# THYMELEAF
spring.thymeleaf.cache=false

# MULTIPART
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB

# LOGGING (comentar cuando no se esté debuggeando para limpiar consola)
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE

# JWT (Lote 5 — API REST)
app.jwt.secret=esta-es-una-clave-muy-larga-de-ejemplo-cambiar-en-produccion-por-favor-12345
app.jwt.expiration-ms=86400000
```

### ⚠️ Pendientes de seguridad / higiene
- [ ] Sacar la contraseña de MySQL del archivo (`${DB_PASSWORD}` + variable de entorno o perfil `application-local.properties` en `.gitignore`).
- [ ] Sacar `app.jwt.secret` a variable de entorno (`${JWT_SECRET}`). La clave actual es solo para desarrollo local.
- [ ] Añadir `spring.jpa.open-in-view=false` para evitar el anti-patrón "OSIV". Actualmente OSIV está activo (default Spring Boot), lo cual permite que las plantillas accedan a relaciones LAZY (`paquete.destino.nombre`) sin pensar. Al desactivar habrá que hacer `JOIN FETCH` o cargar todo desde el servicio dentro de la transacción.
- [ ] Comentar los logs SQL DEBUG/TRACE cuando no se esté debuggeando.

---

## 7. Progreso del proyecto

### ✅ Lote 0 — Fundamentos y dominio
- [x] Diagnóstico inicial y `pom.xml` corregido (Spring Boot 3.4.5, starters, Thymeleaf, validation, H2 para tests).
- [x] `application.properties` con MySQL local.
- [x] Estructura de paquetes.
- [x] Enums `Rol` y `EstadoReserva`.
- [x] Entidades `Usuario`, `Destino`, `Paquete`, `Reserva` con validaciones y relaciones.
- [x] Repositorios JPA con métodos derivados.
- [x] Excepción `RecursoNoEncontradoException`.
- [x] Servicios completos: `DestinoService`, `PaqueteService`, `UsuarioService`, `ReservaService`.
- [x] Aplanamiento de la estructura de carpetas.

### ✅ Seguridad
- [x] `PasswordEncoderConfig` con BCrypt.
- [x] `CustomUserDetailsService` apoyado en `UsuarioRepository`.
- [x] `SecurityConfig` con rutas públicas/privadas, formLogin, logout y CSRF.
- [x] Dependencia `thymeleaf-extras-springsecurity6` añadida al `pom.xml` (habilita `sec:authorize`).

### ✅ Lote 1 — Navegación pública de lectura
- [x] `HomeController` (`GET /`) muestra destinos y paquetes destacados.
- [x] `DestinoController` (`GET /destinos`, `GET /destinos/{id}`) con filtros por nombre y país.
- [x] `PaqueteController` (`GET /paquetes`, `GET /paquetes/{id}`) con filtros por destino, tipo y rango de precio.
- [x] Fragmentos compartidos `fragments/layout.html` (head, navbar, footer).
- [x] `index.html` migrada a Thymeleaf.
- [x] `destinos.html` migrada a Thymeleaf con filtros funcionales.
- [x] `destino-detalle.html` (nueva).
- [x] `paquetes.html` (nueva) con filtros funcionales.
- [x] `paquete-detalle.html` (nueva) con botón "Reservar" que enruta a `/reservas/nueva` (protegido por Security).

### ✅ Manejo de errores
- [x] `GlobalExceptionHandler` con `@ControllerAdvice` mapeando 4 tipos de excepción.
- [x] Plantillas `templates/error/400.html`, `404.html`, `409.html`, `500.html` reutilizando los fragmentos.
- [x] CSS para `.error-page` y `.error-page__codigo` en `styles.css`.

### ✅ Datos seed
- [x] `DataSeeder` (`CommandLineRunner` idempotente) en `config/`.
- [x] 5 destinos con imágenes desde `picsum.photos` (París, São Paulo, Santa Marta, Madrid, Ciudad de México).
- [x] 7 paquetes vinculados a los destinos.
- [x] 2 usuarios: `admin@ideal-trip.com / admin123` (ADMIN) y `cliente@ideal-trip.com / cliente123` (CLIENTE), ambos con password BCrypt.

### ✅ Lote 2 — Autenticación
- [x] `RegistroDto` con validaciones (`@NotBlank`, `@Email`, `@Size`).
- [x] `AuthController` con `GET /login`, `GET /registro` y `POST /registro`.
- [x] `POST /login` lo procesa Spring Security (no hay método propio).
- [x] Plantillas `login.html` y `registro.html` con `th:object` + `th:field` + `th:errors`.
- [x] Validación cruzada de password y unicidad de email con `rejectValue` en el controlador.
- [x] Mensajes flash (`RedirectAttributes.addFlashAttribute`) tras registro exitoso.
- [x] Banners para `?error` (credenciales incorrectas) y `?logout` (sesión cerrada).
- [x] CSS para `.auth-card`, `.auth-form`, `.error-msg` y `.alert` (success/info/danger).

### ✅ Lote 3 — Reservas del cliente
- [x] `ReservaDto` con `@NotNull`, `@Future`, `@Min`, `@DateTimeFormat(ISO.DATE_TIME)`.
- [x] `ReservaController` con cuatro endpoints: `GET /reservas/nueva?paqueteId=...`, `POST /reservas`, `GET /mis-reservas`, `POST /reservas/{id}/cancelar`.
- [x] Identificación del usuario logueado mediante `Principal principal` + `usuarioService.buscarPorEmail(principal.getName())`.
- [x] **Autorización a nivel de fila** en el cancelar: solo cancelas tus propias reservas. La autenticación dice "quién eres", la autorización dice "qué puedes hacer".
- [x] Validación adicional de cupos en el controlador antes de delegar al servicio.
- [x] Plantilla `reserva-form.html` con `<input type="datetime-local">` y resumen del paquete.
- [x] Plantilla `mis-reservas.html` con badges por estado (`.badge--pendiente/confirmada/cancelada/completada`), `#temporals.format` para fechas, y botón "Cancelar" solo visible para estados PENDIENTE/CONFIRMADA.
- [x] Patrón POST-Redirect-GET en todas las acciones (crear y cancelar).
- [x] CSS para `.reserva-resumen`, `.reserva-card` y badges por estado.

### Decisiones nuevas tras Lote 3
- **Acceso al usuario logueado en controladores:** `Principal principal` parámetro + `usuarioService.buscarPorEmail(principal.getName())`. Las alternativas (`Authentication` y `@AuthenticationPrincipal`) son válidas pero más verbosas para nuestro caso.
- **Enums en Thymeleaf:** comparar con `entidad.enumField.name() == 'VALOR'` o usar `T(package.Enum).VALOR`. La comparación directa de objetos no funciona como en Java.
- **Fechas en Thymeleaf:** `#temporals.format(fecha, 'pattern')` para `java.time` (no usar `#dates`, que es para el viejo `java.util.Date`).
- **Confirmación de acciones destructivas:** `onsubmit="return confirm('...');"` inline es la solución mínima sin JS externo.

### ✅ Lote 4 — Área admin
- [x] `AdminController` único con prefijo `@RequestMapping("/admin")`, protegido por `hasRole("ADMIN")` en `SecurityConfig`.
- [x] `DestinoDto` y `PaqueteDto` con validaciones para formularios admin.
- [x] **Dashboard** (`GET /admin`): métricas básicas (total destinos, total paquetes, total reservas, total usuarios).
- [x] **CRUD destinos:** listar, crear (GET+POST), editar (GET+POST), desactivar (POST), activar (POST).
- [x] **CRUD paquetes:** listar, crear (GET+POST), editar (GET+POST), desactivar (POST).
- [x] Plantillas en `templates/admin/`: `dashboard.html`, `destinos.html`, `destino-form.html`, `paquetes.html`, `paquete-form.html`.
- [x] **Formulario único crear/editar** con variable `${modo}` que controla `th:action`, título y botón.
- [x] **Enlaces admin en navbar** mediante `sec:authorize="hasRole('ADMIN')"`.
- [x] Sub-nav admin (`fragments/layout.html :: adminNav`) incluida en cada plantilla admin.
- [x] CSS para `.metric-card`, `.admin-table`, `.admin-nav`, `.btn--sm`, `.btn--danger`.
- **Opcional pendiente:** añadir `activar(id)` a `PaqueteService` y al `AdminController` para tener simetría con destinos (el código está como comentario en `paquetes.html`).

### Decisiones nuevas tras Lote 4
- **CRUD con DTO para formularios admin:** mismo patrón que en registro/reserva; nunca exponer la entidad JPA al navegador.
- **Formulario único para crear+editar** ahorra duplicar plantillas casi idénticas; `${modo}` decide el comportamiento.
- **Pre-llenado de DTOs en edición:** el controlador construye el DTO copiando los campos de la entidad. En proyectos serios se usa MapStruct o ModelMapper.
- **`count()` vs `listarTodos().size()`:** para métricas usar siempre `count()` del repositorio (un solo `SELECT COUNT(*)` en SQL). Aquí mezclé ambos por simplicidad.

### ✅ Lote 5 — API REST con JWT (en marcha)
- [x] Añadidas dependencias JJWT 0.12.6 al `pom.xml` (`jjwt-api` compile, `jjwt-impl` y `jjwt-jackson` runtime).
- [x] `application.properties` con `app.jwt.secret` y `app.jwt.expiration-ms`.
- [x] `JwtService` (genera/valida tokens HS256 con HMAC-SHA).
- [x] `JwtAuthenticationFilter extends OncePerRequestFilter` que lee `Authorization: Bearer …`, valida y popula el `SecurityContext`.
- [x] `FilterRegistrationBean<JwtAuthenticationFilter>` con `setEnabled(false)` para evitar doble registro (Spring Boot + Security).
- [x] `SecurityConfig` reescrito con **dos `SecurityFilterChain`** (`apiFilterChain` con `@Order(1)` + `securityMatcher("/api/**")` stateless + JWT, `webFilterChain` con `@Order(2)` form login + sesiones).
- [x] `PasswordEncoder` movido a `SecurityConfig` como `@Bean` y eliminado `PasswordEncoderConfig.java`.
- [x] DTOs API en `dto/api/`: `LoginRequestDto`, `LoginResponseDto`, `DestinoResponseDto`.
- [x] `AuthApiController` (`POST /api/auth/login`) devuelve `LoginResponseDto` con el JWT, valida credenciales con `PasswordEncoder.matches()` y comprueba `activo`.
- [x] `DestinoApiController` (`GET /api/destinos`, `GET /api/destinos/{id}`) — endpoints públicos JSON para verificar el flujo.
- [x] `ApiExceptionHandler` con `@RestControllerAdvice(basePackages = "…controller.api")` + `@Order(HIGHEST_PRECEDENCE)` que devuelve JSON para `BadCredentialsException` (401), `RecursoNoEncontradoException` (404), `MethodArgumentNotValidException` (400) y `Exception` (500).
- [x] Verificado: login JWT funciona en Postman (body `raw` + JSON, `Content-Type: application/json`).
- [ ] Verificar GET `/api/destinos` y `/api/destinos/{id}` (200 JSON).
- [ ] Verificar respuestas de error en JSON (404 con id inexistente, 401 con credenciales malas, 400 con DTO inválido).

### Decisiones nuevas tras Lote 5
- **Multi-chain Security:** dos `SecurityFilterChain` con `@Order` y `securityMatcher` permiten que web (sesiones + CSRF + formLogin) y API (stateless + JWT) convivan sin pisarse. La primera cadena que matchea la URL es la que la procesa.
- **`@RestControllerAdvice` con `basePackages`:** restringir el handler de excepciones JSON al paquete `controller.api` evita que se trague las excepciones del lado MVC (que deben renderizarse como HTML por `GlobalExceptionHandler`).
- **`FilterRegistrationBean.setEnabled(false)`:** sin esto, Spring Boot autorregistraría el filtro JWT como filtro de servlet global y se ejecutaría en TODAS las peticiones (incluso `/login` HTML). Lo queremos solo dentro de la cadena `/api/**`.
- **Postman & DTOs:** en Postman hay que elegir `Body → raw → JSON` y mandar el header `Content-Type: application/json`. Si se manda como `text` o sin header, Jackson recibe un `String` y revienta con `HttpMessageNotReadableException` porque el DTO no tiene constructor de un solo String.
- **JWT secret:** la clave HMAC para HS256 debe pesar al menos 256 bits (~32 caracteres ASCII). Hay que sacarla a variable de entorno antes de producción.
- **`PasswordEncoder` bean único:** al juntar la API y el web, el bean lo definimos una sola vez dentro de `SecurityConfig` (donde está la configuración de Security). Esto evita el error `required a bean of type 'PasswordEncoder' that could not be found` cuando se elimina `PasswordEncoderConfig.java` sin acordarse de mover el bean.

### 🔄 En curso / siguiente paso
La app está funcionalmente completa. Lo que queda son tareas de pulido y calidad:

### 🕐 Pendiente (pulido / mejoras opcionales)
1. **Reservas admin** (opcional, no era parte del alcance del Lote 4): listado global y transiciones de estado (PENDIENTE → CONFIRMADA → COMPLETADA).
2. **Activar paquetes** desde admin: añadir `activar(id)` a `PaqueteService` + endpoint en `AdminController` + descomentar el bloque en `paquetes.html`.
3. **Pequeño detalle UX:** si todavía no lo cambiaste, en `SecurityConfig` poner `.logoutSuccessUrl("/login?logout")` para ver el banner azul de logout.
4. **Carpeta `static/img/`** con `placeholder.jpg` para el fallback de destinos sin imagen.
5. **Higiene:** `spring.jpa.open-in-view=false`, password de MySQL a variable de entorno, comentar logs SQL DEBUG.
6. **Estilos:** clase `.ficha-paquete` y `.ficha-paquete__list` (referenciadas en `paquete-detalle.html`, propuestas pero no añadidas a `styles.css` todavía).
7. **Tests** con `@WebMvcTest`, `@DataJpaTest` y H2.
8. **Página `error/403.html`** para cuando un CLIENTE intente entrar a `/admin/**`.
9. **Tipos de paquete como enum** en vez de strings sueltos.
10. **Decidir cuándo borrar/condicionar `DataSeeder`** (opcionalmente con `@Profile("dev")`).
11. **Decidir si se mantienen o se borran** los HTML extras de `.claude/worktrees/` (signup, admin, about, Noticias, Estructura).

---

## 8. Cómo arrancar el proyecto

### Requisitos
- Java 21 instalado.
- MySQL 8.x corriendo en `localhost:3306` con credenciales `root` / `<password configurada>`.
- IntelliJ IDEA (o cualquier IDE que soporte Maven).

### Desde IntelliJ
1. Abrir la carpeta `ideal_trip/` (la que contiene el `pom.xml` directamente).
2. Esperar a que Maven resuelva dependencias.
3. Botón Run sobre `IdealTripApplication`.
4. App accesible en `http://localhost:8080`.

### Desde terminal
```powershell
cd "C:\Users\Andrés Felipe\OneDrive\Documents\Proyecto Programación WEB\ideal_trip"
.\mvnw.cmd spring-boot:run
```

### Rutas disponibles
**Públicas (sin login):**
- `GET /` — home con destinos y paquetes destacados.
- `GET /destinos` — listado (acepta `?q=...&pais=...`).
- `GET /destinos/{id}` — detalle del destino con sus paquetes.
- `GET /paquetes` — listado (acepta `?destinoId=...&tipo=...&precioMin=...&precioMax=...`).
- `GET /paquetes/{id}` — detalle del paquete.
- `GET /login` — formulario de inicio de sesión.
- `POST /login` — autenticación (procesado por Spring Security).
- `GET /registro` — formulario de creación de cuenta.
- `POST /registro` — procesa el registro (crea Usuario con BCrypt + rol CLIENTE).

**Autenticadas (cualquier rol):**
- `GET /reservas/nueva?paqueteId=...` — formulario para reservar un paquete.
- `POST /reservas` — crea la reserva (descuenta cupos automáticamente).
- `GET /mis-reservas` — listado de reservas del usuario logueado.
- `POST /reservas/{id}/cancelar` — cancela una reserva propia (libera cupos).
- `POST /logout` — cierra sesión.

**ADMIN (rol ADMIN):**
- `GET /admin` — dashboard con métricas básicas.
- `GET /admin/destinos` — tabla CRUD de destinos.
- `GET /admin/destinos/nuevo` y `POST /admin/destinos/nuevo` — crear destino.
- `GET /admin/destinos/{id}/editar` y `POST /admin/destinos/{id}/editar` — editar destino.
- `POST /admin/destinos/{id}/desactivar` y `POST /admin/destinos/{id}/activar` — soft delete y reactivación.
- `GET /admin/paquetes` — tabla CRUD de paquetes.
- `GET /admin/paquetes/nuevo` y `POST /admin/paquetes/nuevo` — crear paquete (select de destinos activos).
- `GET /admin/paquetes/{id}/editar` y `POST /admin/paquetes/{id}/editar` — editar paquete.
- `POST /admin/paquetes/{id}/desactivar` — soft delete del paquete.

**API REST (JSON, Lote 5):**
- `POST /api/auth/login` — body JSON `{ "email": "...", "password": "..." }`. Devuelve `LoginResponseDto` con el JWT y los datos básicos del usuario. Header obligatorio: `Content-Type: application/json`.
- `GET /api/destinos` — listado público de destinos activos como JSON (`List<DestinoResponseDto>`).
- `GET /api/destinos/{id}` — detalle de un destino. 404 JSON si no existe.
- *(pendientes: `POST /api/auth/registro`, `GET /api/paquetes/**`, `GET /api/reservas/mias`, `/api/admin/**`)*

**Uso del JWT (cuando haya endpoints protegidos):**
```
Authorization: Bearer <token>
```

### Credenciales de prueba (creadas por DataSeeder)
- ADMIN: `admin@ideal-trip.com` / `admin123`
- CLIENTE: `cliente@ideal-trip.com` / `cliente123`

### SQL para meter datos de prueba a mano (mientras no haya seed)
```sql
USE ideal_trip;

INSERT INTO destinos (nombre, pais, continente, descripcion, imagen_url, precio_base, activo)
VALUES
('París', 'Francia', 'Europa', 'La ciudad de la luz', null, 1200000.00, true),
('Sao Paulo', 'Brasil', 'América', 'Megaciudad vibrante', null, 1500000.00, true),
('Santa Marta', 'Colombia', 'América', 'Playas del Caribe', null, 800000.00, true);

INSERT INTO paquetes (nombre, tipo, descripcion, incluye, precio, duracion_dias, cupos_disponibles, activo, destino_id)
VALUES
('París Romántico', 'Luna de Miel', 'Luna de miel en París', 'Vuelo + hotel · Traslados · Para 2 personas', 1500000.00, 7, 10, true, 1),
('Aventura en Sao Paulo', 'Aventura', 'Tour cultural', 'Vuelo + hotel · Traslados · Para 2 personas', 1800000.00, 5, 8, true, 2);
```

---

## 9. Convenciones útiles para queries derivadas de Spring Data

| Método | SQL equivalente |
|---|---|
| `findByEmail(String)` | `WHERE email = ?` |
| `existsByEmail(String)` | `SELECT COUNT(*) > 0 WHERE email = ?` |
| `findByActivoTrue()` | `WHERE activo = true` |
| `findByPaisIgnoreCase(String)` | `WHERE LOWER(pais) = LOWER(?)` |
| `findByNombreContainingIgnoreCase(String)` | `WHERE LOWER(nombre) LIKE LOWER('%?%')` |
| `findByPrecioBetween(BigDecimal, BigDecimal)` | `WHERE precio BETWEEN ? AND ?` |
| `findByDestinoId(Long)` | `WHERE destino_id = ?` (navega relación) |
| `countByDestinoId(Long)` | `SELECT COUNT(*) WHERE destino_id = ?` |
| `findByUsuarioIdAndEstado(Long, EstadoReserva)` | `WHERE usuario_id = ? AND estado = ?` |

---

## 10. Cómo continuar en un nuevo chat de Claude

Al abrir un chat nuevo sobre esta carpeta, pegar este prompt:

```
Estoy continuando el proyecto Spring Boot Ideal-Trip. Por favor lee
NOTAS_PROYECTO.md en la raíz del proyecto para entender el estado
actual, las decisiones de diseño tomadas y los pendientes.

Después confírmame que estás al día y continuamos con
[siguiente tarea concreta].
```

El Claude nuevo leerá este archivo y el código real, y retomará en sintonía. **Recordar actualizar este archivo después de cada hito relevante**, sobre todo:
- Nuevas entidades, servicios o controladores.
- Cambios de arquitectura o convenciones.
- Decisiones de diseño nuevas.
- Items marcados como completados.

---

## 11. Recordatorios importantes

- **Instrucción del proyecto:** "Analiza el proyecto y recomienda modificaciones pero no las hagas directamente." Claude NO debe escribir código en archivos del proyecto sin confirmación; debe mostrar el código en chat para que el estudiante lo aplique.
- **Excepciones a la instrucción:** archivos de documentación (como este) explícitamente solicitados por el usuario.
- **No usar `@Data` en entidades JPA** (rompe `equals/hashCode/toString` con relaciones).
- **No llamar `repository.save()` después de modificar una entidad gestionada dentro de `@Transactional`** — el dirty checking de Hibernate lo hace solo.
- **Soft delete siempre** sobre Usuario, Destino y Paquete. Borrado físico solo en casos muy puntuales y nunca desde la UI normal.
- **Dinero con `BigDecimal`**, fechas con `java.time`, IDs con `Long`.
- **Inyección por constructor**, nunca con `@Autowired` en campos.
- **Sintaxis Thymeleaf:** preferir `|texto ${var}|` (literal substitution) sobre concatenación con `+`.
- **URLs en plantillas:** siempre con `@{/ruta}`, nunca hardcoded.
- **Plantillas con relaciones LAZY:** OSIV está activo, así que `${entidad.relacion.campo}` funciona. Cuando OSIV se desactive habrá que cargar las relaciones explícitamente.
- **Logout en formularios:** POST a `/logout` con CSRF token (lo inyecta Thymeleaf automáticamente con `th:action`).
- **API REST vs MVC:** todo lo que viva en `/api/**` es JSON, stateless, sin CSRF y se autentica con JWT en el header `Authorization: Bearer <token>`. Todo lo demás es HTML con sesión + CSRF + formLogin. Son dos mundos servidos por la misma app pero por cadenas de filtros distintas.
- **JWT secret en `application.properties`:** solo para desarrollo. Antes de cualquier despliegue real, pasar a variable de entorno (`${JWT_SECRET}`).
- **Filtros JWT y doble registro:** si se añade un filtro custom como `@Component`, Spring Boot lo registra dos veces (en la cadena de Security + como filtro global del servlet). Usar siempre `FilterRegistrationBean<...>` con `setEnabled(false)` para los filtros que solo deben correr dentro de Security.
- **Postman y DTOs JSON:** seleccionar `Body → raw → JSON` (no `text`) y enviar header `Content-Type: application/json`. Sin esto, Jackson recibe un `String` y revienta con `HttpMessageNotReadableException`.

---

---

## 12. Cambios posteriores (mayo 2026)

Incrementos sobre el Lote 4/5 ya documentados. Sin renombrar lotes para no romper la numeración histórica.

### Limpieza del `pom.xml`
Eliminado el bloque entero de Kotlin (propiedades, dependencias `kotlin-stdlib-jdk8`/`kotlin-test`, plugin `kotlin-maven-plugin`). IntelliJ lo había inyectado por accidente con una versión inexistente (`2.3.10`) que rompía la resolución de Maven. El `maven-compiler-plugin` quedó solo con el `annotationProcessorPath` de Lombok.

### Swagger / OpenAPI
Dependencia `springdoc-openapi-starter-webmvc-ui` **2.8.4** (la 2.6.0 da `NoSuchMethodError` con Spring 6.2 porque `ControllerAdviceBean(Object)` cambió de firma). Bean `OpenApiConfig` con metadata + `bearerAuth` SecurityScheme. `SecurityConfig.webFilterChain` permite `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`. Controllers y DTOs `dto/api/` anotados con `@Tag`, `@Operation`, `@ApiResponse`, `@Schema`. UI en `http://localhost:8080/swagger-ui/index.html`.

### Login con Google (OAuth2 OIDC)
Dependencia `spring-boot-starter-oauth2-client`. **Google usa OpenID Connect**, así que Spring invoca `OidcUserService`, no `OAuth2UserService` — esa distinción se nos pasó al inicio y costó un debug. La solución es `CustomOidcUserService` que hereda de `OidcUserService` y devuelve `DefaultOidcUser` con `nameAttributeKey="email"` (para que `principal.getName()` siga devolviendo el email, consistente con formLogin). Crea automáticamente el `Usuario` en BD si el email es nuevo: rol `CLIENTE`, `activo=true`, password aleatoria BCrypt (UUID) que el usuario nunca usa. Marcado `@Transactional`.

`SecurityConfig.webFilterChain` añade `.oauth2Login()` coexistiendo con `formLogin` (mismo `loginPage="/login"`). Rutas `/oauth2/**` y `/login/oauth2/**` en `permitAll`. `login.html` con botón "Iniciar sesión con Google" apuntando a `/oauth2/authorization/google`. Banner `?oauthError` para fallos. Variables de entorno: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`. Scopes: `openid,profile,email`. Redirect URI: `{baseUrl}/login/oauth2/code/{registrationId}`.

**`PasswordEncoderConfig.java` separado** del `SecurityConfig` para romper el ciclo `SecurityConfig → CustomOidcUserService → PasswordEncoder (que vivía dentro de SecurityConfig)`. Volvimos al diseño original que las notas viejas ya mencionaban.

### Imágenes en paquetes
Campo `imagenUrl` (String, nullable, max 255) en entidad `Paquete` y `PaqueteDto`. Misma estrategia que `Destino`: URL externa, no upload. `PaqueteService.actualizar()` copia el campo. `paquete-form.html` con input `type="url"` opcional. `paquete-detalle.html` con `th:with` que usa la URL del paquete y cae a la del destino como fallback. `paquetes.html` público rediseñado con cards modernos (imagen redondeada con padding interior, lista con iconos emoji, descripción truncada con `-webkit-line-clamp`, CTA pill azul). `admin/paquetes.html` con columna de thumb.

### Gestión admin de reservas
`AdminController` inyecta ahora `ReservaService`. Endpoints: `GET /admin/reservas` (con filtro `?estado=...`), `POST /admin/reservas/{id}/completar`, `POST /admin/reservas/{id}/cancelar`. La acción "Confirmar" se eliminó (ver pago). Plantilla `admin/reservas.html` con tabla, filtro auto-submit en `<select>`, acciones contextuales por estado. Dashboard ampliado con métricas por estado (`reservasPendientes`, `reservasConfirmadas`, `reservasCompletadas`).

### Fragmento `adminNav`
Estaba referenciado por todas las plantillas admin pero **no existía** en `layout.html`. Causaba `TemplateInputException` en el dashboard. Se añadió el fragmento con links a Dashboard, Reservas, Destinos, Paquetes.

### Simulación de pago (flujo simple)
`POST /reservas/{id}/pagar` sin form intermedio: solo botón en `mis-reservas.html` con `onsubmit="return confirm(...)"`. El método valida autorización a nivel de fila (solo el dueño paga) + estado PENDIENTE, luego llama a `reservaService.confirmar(id)`. **El admin perdió la acción "Confirmar"** — solo el cliente puede confirmar mediante pago. El admin solo puede cancelar PENDIENTES si el cliente no paga. (Existió brevemente una versión con `PagoDto` + `pago-form.html` checkout ficticio con campos de tarjeta, descartada por simplicidad.)

### Otros fixes
`GlobalExceptionHandler` ahora maneja `NoResourceFoundException` específicamente devolviendo `ResponseEntity.notFound()` con `log.debug` (antes contaminaba el log con stack traces gigantes cada vez que el navegador pedía `/favicon.ico`). Se añadió un `favicon.ico` real en `static/`.

### Deuda técnica acumulada
- **Secrets hardcoded**: `spring.datasource.password` y `app.jwt.secret` siguen en `application.properties` en texto plano. Solo `GOOGLE_CLIENT_ID/SECRET` están externalizados a variables de entorno. Crítico antes de cualquier commit a repo público.
- **API REST incompleta**: solo `/api/auth/login` y `/api/destinos/**`. Decisión consciente — Felipe priorizó Swagger + OAuth sobre completar la API.
- **Sin tests**, **`spring.jpa.open-in-view`** sigue activo, **sin expiración automática** de reservas PENDIENTES no pagadas.

### Archivos nuevos respecto al Lote 4
```
config/
  ├── OpenApiConfig.java
  └── PasswordEncoderConfig.java          (se sacó de SecurityConfig)
security/
  └── CustomOidcUserService.java          (Google es OIDC)
templates/
  └── admin/reservas.html
static/
  └── favicon.ico
```

`PagoDto.java` y `pago-form.html` existieron brevemente y se eliminaron.

---

*Última actualización: 2026-05-22 — Hito alcanzado: **Swagger UI operativo, login social con Google (OIDC) coexistiendo con formLogin, imágenes en paquetes, gestión admin de reservas (sin confirmar), simulación de pago vía botón**. La confirmación de reservas pasó a ser responsabilidad del cliente (pago). Pendiente: tests, sacar secrets a env vars, expiración automática de PENDIENTES, completar API REST si se quisiera (paquetes/reservas/admin).*
