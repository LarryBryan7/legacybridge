# LegacyBridge — Enterprise Legacy Integration Gateway

## 1. Qué es LegacyBridge

LegacyBridge es una API REST en Spring Boot pensada para funcionar, en fases posteriores, como una pasarela ("gateway") entre aplicaciones modernas (REST/JSON) y sistemas core legacy que exponen servicios SOAP/XML propietarios.

Es un proyecto genérico de portafolio: no reproduce código, endpoints, WSDL, XML ni reglas de negocio de ningún sistema empresarial real.

## 2. Qué problema intenta resolver

Muchas organizaciones (especialmente en el sector financiero) siguen operando sobre sistemas core legacy que solo hablan SOAP/XML. Construir un canal digital nuevo (app, web, integraciones con terceros) directamente contra ese legacy es lento y frágil. LegacyBridge propone una capa intermedia que:

- Expone una API REST/JSON moderna, documentada y versionada.
- Traduce y adapta hacia/desde el protocolo legacy (a partir de la Fase 2).
- Aplica resiliencia, seguridad y auditoría de forma centralizada, en lugar de repetirlas en cada cliente.

## 3. Arquitectura

Organización por *feature*, no por capa técnica global, para que cada dominio (por ahora, `operation`) sea autocontenido:

```
com.legacybridge
├── LegacyBridgeApplication.java
├── operation
│   ├── controller   -> expone la API REST
│   ├── dto          -> contratos de entrada/salida (records)
│   ├── entity       -> modelo JPA (Operation, OperationStatus)
│   ├── repository   -> acceso a datos (Spring Data JPA)
│   └── service      -> lógica de negocio
├── common
│   └── exception    -> manejo global de errores (@RestControllerAdvice)
└── config
    └── OpenApiConfig
```

Flujo de una request típica (`POST /api/v1/operations`):

```
Cliente
  → OperationController        (valida el DTO con @Valid)
  → OperationService            (aplica reglas, crea la entidad)
  → OperationRepository (JPA)   (persiste en SQL Server)
  → OperationResponse           (DTO de salida, nunca la entidad)
```

La entidad `Operation` nunca se expone directamente: los `Controller` solo hablan en términos de DTOs.

### Diagrama de evolución (visión completa del producto)

```
Aplicación moderna
      ↓ REST / JSON
  LegacyBridge   ← estamos aquí (Fase 1: núcleo de dominio)
      ↓ SOAP / XML   (Fase 2+: Legacy Adapter)
 Sistema Legacy
```

## 4. Tecnologías utilizadas (Fase 1)

| Categoría | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3.4 (Spring MVC, Spring Core/DI, Spring Data JPA) |
| Build | Maven |
| Base de datos | Microsoft SQL Server |
| Validación | Bean Validation (Jakarta Validation) |
| Documentación | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, MockMvc |
| Utilidades | Lombok |

No incluye todavía: Kafka, WebFlux, Resilience4j, OAuth2, Docker, Jenkins, Quarkus ni microservicios — quedan para fases posteriores (ver Roadmap).

## 5. Cómo ejecutar el proyecto

Requisitos: Java 17+, Maven 3.9+, SQL Server 2019+ corriendo localmente (o accesible por red).

```bash
mvn spring-boot:run
```

La aplicación arranca por defecto en `http://localhost:8080`.

## 6. Configuración de SQL Server

La configuración vive en `src/main/resources/application.yml` y se resuelve por variables de entorno, con valores por defecto solo para desarrollo local — **no hay credenciales reales en el repositorio**.

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de SQL Server |
| `DB_PORT` | `1433` | Puerto |
| `DB_NAME` | `legacybridge` | Nombre de la base de datos |
| `DB_USERNAME` | `sa` | Usuario |
| `DB_PASSWORD` | `Legacybridge#1` | Password (cumple la política de complejidad de SQL Server) |
| `SERVER_PORT` | `8080` | Puerto de la aplicación |

La URL JDBC incluye `encrypt=true;trustServerCertificate=true` — necesario para conectarse en desarrollo local sin un certificado TLS válido (el driver `mssql-jdbc` exige cifrado por defecto desde la v9). **En producción no se debe usar `trustServerCertificate=true`**; ahí corresponde un certificado real.

### Levantar SQL Server localmente

SQL Server no tiene binario nativo para macOS, así que la forma estándar de correrlo en local es con Docker:

```bash
docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=Legacybridge#1" \
  -p 1433:1433 --name legacybridge-sqlserver \
  -d mcr.microsoft.com/mssql/server:2022-latest
```

> En Mac con chip Apple Silicon (arm64), la imagen oficial de `mssql/server` corre vía emulación (más lenta). Si prefieres una alternativa nativa arm64 compatible con el protocolo TDS de SQL Server para desarrollo, usa `mcr.microsoft.com/azure-sql-edge` con las mismas variables de entorno y el mismo puerto.

Crear la base de datos (con `sqlcmd`, incluido en la imagen o instalable por separado):

```bash
docker exec -it legacybridge-sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P "Legacybridge#1" -C \
  -Q "CREATE DATABASE legacybridge"
```

En esta fase, Hibernate crea/actualiza el esquema automáticamente (`ddl-auto: update`). Migrar a Flyway/Liquibase está en el roadmap.

### Notas de la migración PostgreSQL → SQL Server

- **Sensibilidad a mayúsculas/minúsculas en `external_id`:** la collation por defecto de SQL Server (`SQL_Latin1_General_CP1_CI_AS`) es *case-insensitive*, a diferencia de PostgreSQL. Hoy eso significa que `OP-1001` y `op-1001` se consideran el **mismo** valor para la restricción de unicidad. Si el sistema legacy real distingue mayúsculas en sus identificadores, esta columna debería declararse con una collation *case-sensitive* (`_CS_AS`) — no se cambió en esta fase porque depende de una regla de negocio que este proyecto genérico no fija.
- **`createdAt`/`updatedAt` (`Instant`):** SQL Server no tiene un tipo con offset de huso horario como `timestamptz` de PostgreSQL; Hibernate los mapea a `datetime2`, que guarda la fecha/hora "naive" — el UTC se mantiene por convención de la aplicación (siempre se escribe/lee como `Instant`), no por el motor. No requiere cambios de código, pero es importante tenerlo claro para no asumir que la base de datos garantiza el huso horario por sí sola.
- El resto del modelo (`IDENTITY`, `decimal(19,2)`, `varchar` de longitud fija, el índice único sobre `external_id`) es portable entre ambos motores sin cambios.

## 7. Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/operations` | Lista todas las operaciones |
| `GET` | `/api/v1/operations/{id}` | Obtiene una operación por id |
| `POST` | `/api/v1/operations` | Crea una operación (queda en estado `PENDING`) |

## 8. Cómo acceder a Swagger

Con la aplicación corriendo:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 9. Ejemplos de request/response

**Crear una operación**

```http
POST /api/v1/operations
Content-Type: application/json

{
  "externalId": "OP-1001",
  "amount": 2500.00,
  "currency": "PEN"
}
```

```json
{
  "id": 1,
  "externalId": "OP-1001",
  "amount": 2500.00,
  "currency": "PEN",
  "status": "PENDING",
  "createdAt": "2026-09-07T10:00:00Z",
  "updatedAt": "2026-09-07T10:00:00Z"
}
```

**Error de validación (400)**

```json
{
  "timestamp": "2026-09-07T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene datos invalidos",
  "path": "/api/v1/operations",
  "details": ["externalId: externalId es obligatorio"]
}
```

**Operación no encontrada (404)**

```json
{
  "timestamp": "2026-09-07T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe una Operation con id 99",
  "path": "/api/v1/operations/99",
  "details": []
}
```

## Ejecutar los tests

```bash
mvn test
```

Incluye pruebas unitarias del `OperationService` con Mockito (creación, búsqueda exitosa, búsqueda con id inexistente) y pruebas de slice (`@WebMvcTest`) sobre la validación del `OperationController`.

## 10. Roadmap de futuras fases

- **Fase 2 — Legacy Adapter:** interfaz `LegacyOperationGateway` + implementación SOAP/XML contra un **Mock Legacy SOAP System** propio (sin datos reales de ningún proveedor).
- **Fase 3 — Resiliencia:** Resilience4j (circuit breaker, retry, timeout) en las llamadas al legacy.
- **Fase 4 — Seguridad:** Spring Security + JWT/OAuth2 para proteger la API.
- **Fase 5 — Observabilidad y eventos:** métricas/trazas, y Kafka para publicar eventos de cambio de estado de una `Operation`.
- **Fase 6 — Escalado:** WebFlux para las llamadas concurrentes al legacy, contenedorización con Docker y pipeline CI/CD (Jenkins o GitHub Actions).

Estas tecnologías se incorporan solo cuando el problema real lo justifique, no por completar un checklist.
