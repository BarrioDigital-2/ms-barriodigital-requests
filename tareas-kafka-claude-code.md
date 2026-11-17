# BarrioDigital — Tareas Kafka para Claude Code

## Cómo trabajar
- Haz **una tarea a la vez**. Al terminar cada una, detente y muéstrame un resumen de los archivos creados o modificados para que los revise.
- **No hagas commit ni push.** Git lo manejo yo.
- No toques `SecurityConfig.java` ni los cambios temporales de desarrollo (los revierto al final).
- Comentarios y logs en español. Entorno: Windows + PowerShell.
- Si algo del caso es ambiguo o choca con el código existente, pregúntame antes de decidir.

---

## Contexto del proyecto

**BarrioDigital** (DSY1107, DUOC UC): plataforma de trámites comunales con microservicios Spring Boot. RabbitMQ se usa para comandos y notificaciones, y Kafka para el streaming de auditoría y reportería.

**Stack:** Spring Boot **4.1.2**, Java 17, Spring Kafka 4 (Jackson 3), Lombok, MySQL.

**Repos / carpetas relevantes**
| Carpeta | Puerto | Rol |
|---|---|---|
| `ms-barriodigital-requests` | 8081 | Producer Kafka + RabbitMQ (CRUD de trámites) |
| `ms-BarrioDigital-Notifications` | 8082 | Tiene el `docker-compose.yml` actual (mysql, rabbit1, rabbit2, zookeeper, kafka) |
| `ms-kafka-consumer` (artifactId: ms-barriodigital-audit) | 8083 | Consumer de auditoría, persiste en MySQL, publica a `audit.timeline`, DLT |

**Buenas prácticas aplicadas:**
- Versionamiento de API: todas las rutas usan `/api/v1/...`
- Ramas: `main` → `develop` → `feature/*` (GitFlow simplificado)

**Estado actual de Kafka**
- Topic principal: `requests.events` (antes `solicitudes-eventos`), 3 particiones, key = `comunaId`, cleanup.policy=delete, retención 7 días.
- Producer: `SolicitudKafkaPublisher` con topic configurable por property `app.kafka.topic.requests-events`. DTO `SolicitudKafkaEvent` con campos: `eventId` (UUID), `tipo`, `solicitudId`, `comunaId`, `tipoTramite`, `estadoAnterior`, `estado`, `usuario`, `origen`, `timestamp` (ISO-8601). Serializa con `JacksonJsonSerializer` y `acks=all`. Publica al crear y en cada cambio de estado. Valida `comunaId` nulo → `"SIN_COMUNA"`.
- Consumer (ms-barriodigital-audit): DTO `SolicitudEvent` con los mismos campos. Consumer group `auditoria-processor`. Persiste en MySQL (tabla `auditoria_eventos`, idempotente por `eventId`). Publica a `audit.timeline` (compact,delete, 30 días). DLT con 3 reintentos hacia `requests.events.audit.DLT` (delete, 14 días).
- Endpoints audit: `GET /api/v1/audit/requests/{solicitudId}/timeline` y `GET /api/v1/audit/events?usuario=&from=&to=&tipo=`.
- Broker: `confluentinc/cp-kafka:7.9.0` + `cp-zookeeper:7.9.0`. **No bajar a 7.5.0** (Kafka client 4.x incompatible con brokers 3.5). Dual listener: `INTERNAL://kafka:9093` (Docker) y `EXTERNAL://localhost:9092` (host).
- Dependencia: `spring-boot-starter-kafka` (no `spring-kafka` directo).

**Levantar en local**
```powershell
cd ms-BarrioDigital-Notifications; docker compose up -d
cd ms-barriodigital-requests; .\mvnw spring-boot:run "-Dmaven.test.skip=true"
cd ms-kafka-consumer; .\mvnw spring-boot:run "-Dmaven.test.skip=true"
# Prueba: POST http://localhost:8081/api/v1/requests
# {"tipoTramite":"PERMISO_OBRAS","vecinoEmail":"test@vecino.cl","direccion":"Av. Siempre Viva 742","descripcion":"Test Kafka"}
```

---

## Lo que exige el caso (sección 9 + relacionadas)

| Tópico | Part. | Réplicas | Política | Retención | Propósito |
|---|---|---|---|---|---|
| `requests.events` | 3 | 3 | delete | 3–7 días | Fuente de verdad de eventos del trámite |
| `audit.timeline` | 3 | 3 | compact,delete | 14–30 días | Historial quién / qué / cuándo / desde dónde |
| `*.DLT` (uno por consumidor) | 3 | 3 | delete | 7–14 días | Mensajes fallidos tras N reintentos |

- Estados del trámite: `INGRESADO → ADMITIDO → EN_GESTIÓN → EN_TERRENO → RESUELTO / RECHAZADO`.
- Infra: `ec2-kafka` con **Zookeeper (3 nodos) + Kafka (3 brokers) + Kafka UI**, en un `compose.yml` propio.
- Además, un **microservicio administrador de Kafka** (según la pauta de la evaluación).

---

## Tareas

### Tarea 1 — Alinear el evento y el topic (ms-barriodigital-requests) ✅ COMPLETADA
1. ✅ Topic renombrado a `requests.events` con property configurable.
2. ✅ `SolicitudKafkaEvent` enriquecido con `eventId`, `solicitudId`, `estadoAnterior`, `usuario`, `origen`, `timestamp` ISO-8601.
3. ✅ Se publica evento al crear y en cada cambio de estado.
4. ✅ Validación de `comunaId` nulo → `"SIN_COMUNA"`.
5. ✅ Topic configurado: 3 particiones, réplicas por property, cleanup.policy=delete, retención 7 días.
6. ✅ Test `SolicitudServiceImplTest` ya tenía el mock de `kafkaPublisher`.

### Tarea 2 — ms-kafka-consumer → ms-barriodigital-audit ✅ COMPLETADA
1. ✅ Renombrado `artifactId` y `spring.application.name` (carpeta y paquete Java se mantienen).
2. ✅ `KafkaStreamsConfig` eliminado, properties de Streams eliminadas.
3. ✅ DTO actualizado con campos nuevos.
4. ✅ Persistencia en MySQL (tabla `auditoria_eventos`, idempotente por `eventId`).
5. ✅ Publicación a `audit.timeline` (compact,delete, 30 días, 3 particiones).
6. ✅ DLT: `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`, 3 reintentos, backoff fijo.
7. ✅ Endpoints: `GET /api/v1/audit/requests/{solicitudId}/timeline` y `GET /api/v1/audit/events`.
8. ✅ Puerto 8083 mantenido.

### Tarea 3 — Crear ms-barriodigital-report (Kafka Streams) ⬚ PENDIENTE
1. Proyecto nuevo: Spring Boot 4.1.2, Java 17, `spring-boot-starter-kafka`, `kafka-streams`, `spring-boot-starter-webmvc`, Lombok. Puerto **8084**.
2. Properties de Streams: `application-id=report-processor`, `state-dir=./kafka-streams-state`, `commit.interval.ms=1000`, `deserialization.exception.handler=LogAndContinueExceptionHandler`.
3. Topología leyendo `requests.events`:
   - **Trámites por hora:** contar `SOLICITUD_CREADA` en ventanas de 1 hora.
   - **Estados activos:** KTable último estado por `solicitudId` → `groupBy(estado).count()`. Excluir RESUELTO y RECHAZADO.
   - **Tiempo de resolución:** guardar fecha de INGRESADO, al llegar RESUELTO calcular duración, acumular promedio.
   - **Tipos más demandados:** conteo por `tipoTramite` en ventanas de 1 día, últimos 7 días.
4. Endpoints:
   - `GET /api/v1/report/kpis?range=last24h`
   - `GET /api/v1/report/top-procedures?range=last7d`
   - Si Streams no está `RUNNING`, responder 503.

### Tarea 4 — Infra Kafka según el caso ⬚ PENDIENTE
1. Crear `infra/kafka/compose.yml` con 3 Zookeeper + 3 brokers + Kafka UI (`provectuslabs/kafka-ui`).
2. No romper el compose actual de `ms-BarrioDigital-Notifications`.
3. README corto con instrucciones.

### Tarea 5 — (pendiente de pauta) Microservicio administrador de Kafka ⬚ PENDIENTE
Solo propuesta de endpoints con `AdminClient`. No implementar aún.

---

## Cambios temporales que HAY QUE REVERTIR antes del despliegue

1. **SecurityConfig.java (ms-requests):** `permitAll()` → restaurar OAuth2/JWT.
2. **SecurityConfig.java (ms-bff):** verificar que matchers usen `/api/v1/...` al restaurar.
3. **docker-compose.yml:** quitar puerto 3306 de MySQL expuesto al host.
4. **application.properties (ms-requests):** quitar `logging.level.org.apache.kafka=DEBUG` si existe.
5. **KAFKA_BOOTSTRAP_SERVERS:** en Docker/producción debe apuntar a `kafka:9093` (listener INTERNAL).

---

## Problemas conocidos y soluciones

| Problema | Causa | Solución |
|----------|-------|----------|
| `InconsistentClusterIdException` al levantar Kafka | Volumen `kafka_data` tiene datos de otro cluster | `docker volume rm barriodigital-rabbitmq_kafka_data` y recrear |
| `Failed to construct kafka producer` con timeoutMillis=0 | Kafka client 4.0+ incompatible con broker 3.5 | Usar cp-kafka:7.9.0 y cp-zookeeper:7.9.0 |
| `ClassNotFoundException: SolicitudKafkaEvent` en consumer | Header `__TypeId__` apunta al paquete del producer | `spring.json.use.type.headers=false` + `spring.json.value.default.type` |
| `No qualifying bean of type KafkaTemplate` | Dependencia `spring-kafka` sin autoconfiguración | Usar `spring-boot-starter-kafka` |
| PowerShell no acepta `-Dmaven.test.skip=true` | PowerShell interpreta el punto | Poner entre comillas: `"-Dmaven.test.skip=true"` |
| Puerto 8083 ya en uso | Instancia anterior corriendo | `netstat -ano \| findstr :8083` → `taskkill /PID <pid> /F` |
| `JAVA_HOME` no definido | JDK no configurado en PATH | Instalar Temurin JDK 17 marcando "Set JAVA_HOME" |

---

## Fuera de alcance por ahora
- Rutas del BFF y pantallas Angular para `/reports` y `/audit`.
- Revertir cambios temporales (se hace antes del despliegue).

## Artefactos creados
- Diagrama de implementación Kafka: https://claude.ai/artifact/P7SMXzURbiwQmbv2uegDg2
- Comparativa Kafka vs RabbitMQ: https://claude.ai/artifact/B8srtp19um7Lijpp4Rx2NH
