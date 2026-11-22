# ms-barriodigital-requests

Microservicio de **trámites** de BarrioDigital: crea solicitudes de los vecinos y gestiona su cambio de estado.
Es el **productor de eventos** del sistema: publica en **Kafka** (`requests.events`) y en **RabbitMQ** (notificaciones).

| Dato | Valor |
|------|-------|
| Puerto | `8081` |
| Stack | Spring Boot 4.1 · Java 17 · Maven · MySQL · Spring Kafka · Spring AMQP |
| Seguridad | OAuth2 Resource Server: JWT de Azure AD (lo llama el BFF con el token del usuario) |
| Base de datos | `barriodigital` |

## Endpoints

Base: `/api/v1/requests` (requiere token de Azure)

| Método | Ruta | Descripción | Respuestas |
|--------|------|-------------|------------|
| POST | `/` | Crear trámite (queda `INGRESADO`) | 201 · 400 |
| GET | `/` (`?status=INGRESADO`) | Listar, opcionalmente por estado | 200 |
| GET | `/{id}` | Detalle | 200 · 404 |
| PUT | `/{id}/status` | Cambiar estado (`{"status":"ADMITIDO"}`) | 200 · 400 · 404 · **409 sin cupo** · **503 catalog caído** |

Estados: `INGRESADO → ADMITIDO → EN_GESTION / EN_TERRENO → RESUELTO` (o `RECHAZADO`).
Regla: no se puede pasar a `EN_TERRENO` sin estar `ADMITIDO` (400).

## Kafka: productor de `requests.events`

Cada vez que se crea o cambia un trámite publica un evento:

```json
{ "id": 12, "tipo": "SOLICITUD_CREADA", "comunaId": "Av. Siempre Viva 742",
  "titulo": "PODA_ARBOLES", "descripcion": "...", "estado": "INGRESADO", "timestamp": "2026-11-21T10:00:00" }
```

| Aspecto | Valor | Por qué |
|---------|-------|---------|
| Topic | `requests.events` (propiedad `requests.kafka.topic`) | Topología del caso |
| Tipos de evento | `SOLICITUD_CREADA`, `SOLICITUD_ACTUALIZADA` | — |
| **Key** | **id de la solicitud** | Todos los eventos de un trámite van a la misma partición → se leen **en orden** (CREADA → ADMITIDO → RESUELTO) |
| `acks` | `all` | El mensaje se confirma cuando lo guardaron todas las réplicas sincronizadas |
| Creación del topic | `config/KafkaTopicConfig`: 3 particiones, `delete`, 7 días, réplicas = `KAFKA_REPLICAS` (1 local, 3 AWS) | El productor es dueño del topic que publica; la creación automática del broker está desactivada |

Consumidores: **ms-barriodigital-audit** (`auditoria-processor` y Kafka Streams `dashboard-processor`) y **ms-barriodigital-report** (`report-processor`).

## RabbitMQ: eventos para notificaciones

Publica en los exchanges `solicitudes.direct` y `solicitudes.topic` con routing keys `solicitud.created` y `solicitud.status`.
Los consume **ms-barriodigital-notifications**.

## Reserva de cupo en catalog (REST síncrono)

Al pasar de `INGRESADO` a `ADMITIDO`, `client/CatalogClient` llama a
`POST {CATALOG_SERVICE_URL}/api/v1/catalog/procedures/{tipoTramite}/reserve` **antes de guardar**:

| catalog responde | requests responde | ¿Admite? |
|------------------|-------------------|----------|
| 200 | sigue con el cambio | ✅ |
| 409 (sin cupo / inactivo) | 409 | ❌ |
| 404 (tipo desconocido) | 409 | ❌ |
| no responde (timeout 2-3 s) | 503 | ❌ |

Es síncrono a propósito: catalog debe poder **rechazar** la admisión antes de que ocurra (Kafka es asíncrono).

## Cómo levantar (local)

```powershell
# 1. Infraestructura (MySQL 3307, RabbitMQ, Kafka), desde barriodigital-infra/local
docker compose up -d

# 2. Este microservicio (lee el .env de barrioDigitalApp: DB_PORT=3307, Azure, RabbitMQ)
.\mvnw clean spring-boot:run "-Dmaven.test.skip=true" "-Dspring-boot.run.arguments=--spring.config.import=optional:file:../.env[.properties]"
```

| Variable | Por defecto | Uso |
|----------|-------------|-----|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `barriodigital` | MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | MySQL |
| `AZURE_TENANT_ID` / `AZURE_API_CLIENT_ID` | IDs de la app de Azure | Validación del JWT |
| `RABBITMQ_HOST` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `localhost` / `barriodigital` / … | RabbitMQ |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka |
| `KAFKA_REPLICAS` | `1` | Réplicas de `requests.events` |
| `CATALOG_SERVICE_URL` | `http://localhost:8085` | Reserva de cupos |

## Cómo probar

- **Tests unitarios:** `.\mvnw test "-Dtest=SolicitudServiceImplTest"` (incluye reservar cupo al admitir y rechazar sin cupo).
- **Flujo completo:** colección Postman de `barriodigital-infra/postman` (pasa por el BFF).
- **Ver los eventos en Kafka:**
  ```powershell
  docker exec -it kafka kafka-console-consumer --bootstrap-server kafka:9093 --topic requests.events --from-beginning --property print.key=true --property key.separator=" | "
  ```

## Despliegue

En AWS corre en la EC2 **apps** (`barriodigital-infra/aws/apps`), con 3 réplicas de Kafka y catalog en la misma red Docker.
