# Ideal-Trip

Aplicación web de agencia de viajes desarrollada con Spring Boot. Permite explorar destinos, ver paquetes, registrarse y gestionar reservas. Incluye panel administrativo, API REST con JWT, login social con Google (OAuth2 OIDC) y documentación interactiva con Swagger.

## Stack tecnológico

- **Backend:** Java 21, Spring Boot 3.4.5, Spring Security 6, Spring Data JPA
- **Base de datos:** MySQL 8
- **Vistas:** Thymeleaf + Bootstrap 5
- **API REST:** JWT (jjwt 0.12.6), documentada con springdoc-openapi 2.8.4
- **Login social:** OAuth2 con Google (OpenID Connect)
- **Utilidades:** Lombok, Bean Validation
- **Build:** Maven (wrapper incluido)

## Funcionalidades

- Catálogo público de destinos y paquetes turísticos con imágenes y filtros.
- Registro y login de usuarios (formulario tradicional + Google).
- Reservas por parte del cliente con cancelación.
- Simulación de pago que confirma la reserva automáticamente.
- Panel administrativo con CRUD de destinos y paquetes + gestión de reservas (completar, cancelar).
- API REST básica protegida con JWT.
- Documentación de la API en Swagger UI.

## Cómo arrancar el proyecto

### Requisitos

- Java 21
- MySQL 8 corriendo en `localhost:3306` (la base se crea sola gracias a `createDatabaseIfNotExist=true`)
- Maven (o usar el wrapper incluido)

### Variables de entorno necesarias

La aplicación lee secrets desde variables de entorno. Configurá estas antes de arrancar:

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_PASSWORD` | Password de tu usuario `root` de MySQL | `tu-password` |
| `JWT_SECRET` | Clave para firmar tokens JWT (mínimo 32 bytes) | (generar uno robusto) |
| `GOOGLE_CLIENT_ID` | Client ID OAuth2 de Google Cloud | `...apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Client Secret OAuth2 de Google Cloud | `GOCSPX-...` |

Para obtener las credenciales de Google: Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client ID. Configurar redirect URI `http://localhost:8080/login/oauth2/code/google`.

### Arrancar con Maven

```bash
./mvnw spring-boot:run
```

(En Windows: `mvnw.cmd spring-boot:run`)

La app queda disponible en `http://localhost:8080`.

### Arrancar desde IntelliJ

1. Abrir la carpeta del proyecto.
2. Esperar a que Maven resuelva dependencias.
3. En **Run → Edit Configurations**, añadir las cuatro variables de entorno listadas arriba.
4. Run sobre `IdealTripApplication`.

## Credenciales de prueba

El `DataSeeder` crea automáticamente:

- **Admin:** `admin@ideal-trip.com` / `admin123`
- **Cliente:** `cliente@ideal-trip.com` / `cliente123`

## Rutas principales

- `/` — Home con destinos y paquetes destacados
- `/destinos`, `/paquetes` — Catálogo público con filtros
- `/login`, `/registro` — Autenticación tradicional y con Google
- `/mis-reservas` — Reservas del cliente logueado
- `/admin` — Panel administrativo (rol ADMIN)
- `/swagger-ui/index.html` — Documentación interactiva de la API
- `/api/auth/login`, `/api/destinos` — API REST con JWT

## Licencia

Proyecto académico. Sin licencia formal.