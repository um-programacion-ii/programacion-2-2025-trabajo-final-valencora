# Servicio Proxy

Servicio intermediario que actúa como puente entre el backend y los servicios externos de la cátedra (Kafka y Redis).

## Descripción

El servicio proxy:
- Consume mensajes de Kafka sobre cambios en eventos
- Consulta el estado de asientos desde Redis de la cátedra
- Notifica al backend sobre cambios recibidos desde Kafka

## Requisitos

- Java 17+
- Maven 3.6+
- Acceso a la red ZeroTier (192.168.194.0/24)
- Kafka de la cátedra disponible en `192.168.194.250:9092`
- Redis de la cátedra disponible en `192.168.194.250:6379`
- Backend corriendo y accesible

## Configuración

### Variables de entorno

```bash
# Kafka
export KAFKA_BOOTSTRAP_SERVERS=192.168.194.250:9092
export KAFKA_CONSUMER_GROUP_ID=proxy-group-unique-id
export KAFKA_TOPIC_EVENTOS=eventos-changes

# Redis
export REDIS_HOST=192.168.194.250
export REDIS_PORT=6379

# Backend
export BACKEND_BASE_URL=http://localhost:8080
export BACKEND_JWT_SECRET=<mismo-secret-que-el-backend>

# Puerto del proxy
export SERVER_PORT=8081
```

### Archivo `application.yml`

Las configuraciones por defecto están en `src/main/resources/application.yml`. Puedes sobrescribirlas con variables de entorno.

## Arranque

```bash
cd proxy
./mvnw spring-boot:run
```

O compilar y ejecutar:

```bash
./mvnw clean package
java -jar target/proxy-0.0.1-SNAPSHOT.jar
```

## Endpoints

### GET `/api/asientos/evento/{eventoId}`
Obtiene el mapa de asientos de un evento desde Redis de la cátedra.

**Respuesta:**
```json
{
  "eventoId": 123,
  "asientos": [
    {
      "fila": "A",
      "numero": 1,
      "estado": "LIBRE"
    }
  ]
}
```

## Funcionalidades

### Consumer de Kafka (Issue #8)
- Se suscribe al tópico `eventos-changes` (configurable)
- Procesa mensajes de cambios de eventos (CREATE, UPDATE, DELETE, CANCEL)
- Notifica al backend de forma asíncrona

### Consulta de Asientos desde Redis (Issue #11)
- Consulta estado de asientos por evento desde Redis de la cátedra
- Endpoint REST para que el backend consulte asientos

### Comunicación con Backend (Issue #12)
- Genera tokens JWT con rol ADMIN para autenticarse con el backend
- Envía notificaciones al endpoint `/api/admin/eventos/notificacion`
- Maneja errores y reintentos

## Estructura del Proyecto

```
proxy/
├── src/main/java/com/um/eventosproxy/
│   ├── config/          # Configuración (ProxyProperties, ProxyConfiguration)
│   ├── dto/             # DTOs para notificaciones y asientos
│   ├── kafka/           # Consumer de Kafka
│   ├── service/         # Servicios (JWT, Redis, Notificaciones)
│   └── web/rest/        # Controladores REST
└── src/main/resources/
    └── application.yml  # Configuración de la aplicación
```

## Logs

Los logs se configuran en `application.yml` y `application-dev.yml`. En modo desarrollo, se muestran logs detallados de Kafka y Redis.

## Troubleshooting

### El proxy no se conecta a Kafka
- Verifica que Kafka esté disponible en la IP configurada
- Verifica la conectividad de red ZeroTier
- Revisa los logs para errores de conexión

### El proxy no se conecta a Redis
- Verifica que Redis esté disponible en la IP configurada
- Verifica la conectividad de red ZeroTier
- Revisa los logs para errores de conexión

### El backend rechaza las notificaciones
- Verifica que el `BACKEND_JWT_SECRET` coincida con el del backend
- Verifica que el backend esté corriendo y accesible
- Revisa los logs del backend para errores de autenticación

