# Sistema de Registro de Asistencia a Eventos

Sistema distribuido para registrar la asistencia a eventos únicos (charlas, cursos, obras de teatro, etc.). El sistema consta de varios componentes que interactúan entre sí mediante APIs REST, Kafka y Redis.

## 📋 Descripción del Proyecto

Este sistema permite a los usuarios:
- Registrarse e iniciar sesión
- Ver eventos disponibles
- Seleccionar asientos para eventos
- Bloquear asientos temporalmente
- Realizar compras de entradas
- Gestionar sesiones compartidas entre múltiples dispositivos
- Sincronizar automáticamente cambios en eventos desde el servicio de la cátedra

## 🏗️ Arquitectura del Sistema

El sistema está compuesto por los siguientes componentes:

1. **Backend (backend)**: Servicio Spring Boot (JHipster) que actúa como orquestador principal
2. **Proxy (proxy)**: Servicio intermediario con acceso a Kafka y Redis de la cátedra
3. **Cliente Móvil (mobile)**: Aplicación desarrollada en Kotlin Multiplatform (KMP) con Compose Multiplatform
4. **Servicio de la Cátedra (cátedra)**: Servicio externo proporcionado por la cátedra

### Diagrama de Flujo

```
App Móvil → Backend → Proxy → Cátedra (Kafka/Redis/REST)
```

**Comunicación:**
- **App Móvil ↔ Backend**: Todas las operaciones (autenticación, eventos, sesión, asientos, ventas)
- **Backend ↔ Proxy**: Operaciones que requieren Kafka/Redis (mapa de asientos, confirmación de ventas)
- **Proxy ↔ Cátedra**: Consumo de Kafka, consulta de Redis, confirmación de ventas

### Arquitectura MVVM en Mobile

La aplicación móvil sigue el patrón **MVVM (Model-View-ViewModel)**:

```
mobile/app/src/main/java/com/um/eventosmobile/
├── model/          # Modelos de UI (UiState, Effects)
├── viewmodel/      # ViewModels con lógica de negocio
├── ui/             # Composables (Screens)
└── MainActivity.kt # Punto de entrada
```

**Separación de responsabilidades:**
- **Model**: Estructuras de datos (`LoginUiState`, `LoginEffect`, etc.)
- **ViewModel**: Lógica de negocio y gestión de estado reactivo
- **UI (Screen)**: Solo presentación, observa ViewModels

## 🚀 Inicio Rápido

### Requisitos Previos

- **Java 17+**
- **Maven 3.6+**
- **PostgreSQL 12+** (para el backend)
- **Node.js 18+** y **npm** (para el frontend del backend y tests Cypress)
- **Kotlin 1.9+** y **Gradle 7+** (para el cliente móvil)
- **Docker** y **Docker Compose** (opcional, para servicios de desarrollo)
- **Android SDK** (para desarrollo móvil)
- **Acceso a red ZeroTier** (192.168.194.0/24) para conectar con servicios de la cátedra

### Instalación

1. **Clonar el repositorio**
   ```bash
   git clone <repository-url>
   cd programacion-2-2025-trabajo-final-valencora
   ```

2. **Configurar Base de Datos**
   ```bash
   # Crear base de datos PostgreSQL
   createdb backend
   # O usar Docker
   docker compose -f backend/src/main/docker/postgresql.yml up -d
   ```

3. **Configurar Backend**
   ```bash
   cd backend
   ./mvnw clean install
   # Ver configuración detallada en backend/README.md
   ```

4. **Configurar Proxy**
   ```bash
   cd proxy
   ./mvnw clean install
   # Ver configuración detallada en proxy/README.md
   ```

5. **Configurar Cliente Móvil**
   ```bash
   cd mobile
   ./gradlew build
   ```

## ⚙️ Configuración

### Variables de Entorno

#### Backend

```bash
# Base de datos
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/backend
export SPRING_DATASOURCE_USERNAME=backend
export SPRING_DATASOURCE_PASSWORD=backend

# JWT
export JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET=<generar-secret>

# Cátedra
export CATEDRA_BASE_URL=http://192.168.194.250:8080
export CATEDRA_AUTH_TOKEN=<token-obtenido-de-la-catedra>

# Proxy
export PROXY_BASE_URL=http://localhost:8081
```

#### Proxy

```bash
# Kafka
export KAFKA_BOOTSTRAP_SERVERS=192.168.194.250:9092
export KAFKA_CONSUMER_GROUP_ID=proxy-group-unique-id
export KAFKA_TOPIC_EVENTOS=eventos-actualizacion

# Redis
export REDIS_HOST=192.168.194.250
export REDIS_PORT=6379

# Backend
export BACKEND_BASE_URL=http://localhost:8080
export BACKEND_JWT_SECRET=<mismo-secret-que-el-backend>
export BACKEND_SYNC_EVENTS_PATH=/api/admin/eventos/sincronizar
```

#### Cliente Móvil

Para dispositivo físico conectado por USB:
```bash
# Configurar port forwarding
adb reverse tcp:8080 tcp:8080
```

La app está configurada para usar `http://localhost:8080` como backend.

### Archivos de Configuración

- **Backend**: `backend/src/main/resources/config/application.yml`
- **Proxy**: `proxy/src/main/resources/application.yml`
- **Cliente Móvil**: Configuración en `mobile/app/src/main/java/com/um/eventosmobile/MainActivity.kt`

## 🔧 Desarrollo

### Ejecutar en Modo Desarrollo

#### Backend
```bash
cd backend
./mvnw
# La aplicación estará disponible en http://localhost:8080
```

#### Proxy
```bash
cd proxy
./mvnw spring-boot:run
# El proxy estará disponible en http://localhost:8081
```

#### Cliente Móvil
```bash
cd mobile
./gradlew installDebug  # Para Android
# O abrir en Android Studio
```

**Importante para desarrollo móvil:**
- Ejecutar `adb reverse tcp:8080 tcp:8080` antes de usar la app
- La app se comunica solo con el backend (puerto 8080)
- El backend orquesta las llamadas al proxy cuando es necesario

### Ejecutar Tests

#### Backend
```bash
cd backend
./mvnw test                    # Tests unitarios
./mvnw verify                  # Tests de integración
```

#### Tests End-to-End con Cypress
```bash
cd backend

# Ejecutar todos los tests e2e
npm run e2e

# Ejecutar solo el test del flujo de venta
npm run e2e -- --spec "src/test/javascript/cypress/e2e/eventos/flujo-venta-e2e.cy.ts"

# Ejecutar en modo interactivo
npm run cypress
```

#### Proxy
```bash
cd proxy
./mvnw test
```

#### Cliente Móvil
```bash
cd mobile
./gradlew test
```

## 📡 Endpoints del Backend

### Autenticación
- `POST /api/authenticate` - Iniciar sesión y obtener token JWT
- `POST /api/register` - Registrar nuevo usuario (activación automática, sin email)

### Eventos
- `GET /api/eventos` - Listar eventos activos (no cancelados, no expirados)
- `GET /api/eventos/{id}` - Obtener detalle de un evento

### Sesión de Selección
- `GET /api/sesion/estado` - Obtener estado actual de selección
- `PUT /api/sesion/estado` - Guardar estado completo de selección
- `PUT /api/sesion/evento/{eventoId}` - Actualizar evento seleccionado
- `PUT /api/sesion/asientos` - Actualizar asientos seleccionados
- `PUT /api/sesion/nombres` - Actualizar nombres de personas
- `DELETE /api/sesion/estado` - Limpiar estado de selección

### Asientos
- `GET /api/asientos/evento/{eventoId}` - Obtener mapa de asientos (el backend consulta al proxy)
- `POST /api/asientos/bloquear/{eventoId}` - Bloquear asientos seleccionados (el backend envía al proxy)

### Ventas
- `POST /api/ventas` - Procesar una venta (el backend confirma con el proxy/cátedra)
- `GET /api/ventas` - Listar ventas del usuario autenticado
- `GET /api/ventas/{id}` - Obtener detalle de una venta

### Administración
- `POST /api/admin/eventos/sincronizar` - Sincronizar eventos desde la cátedra (usado por el proxy)

## 🔄 Flujos Principales

### Flujo de Registro y Autenticación

1. **Registro**: Usuario se registra en `/api/register`
   - Se crea el usuario con `activated=true` (sin activación por email)
   - No se indexa en Elasticsearch durante el registro (para evitar timeouts)
2. **Login**: Usuario inicia sesión en `/api/authenticate`
   - Recibe token JWT
   - Token se guarda localmente en la app móvil

### Flujo de Venta

1. **Autenticación**: Usuario inicia sesión
2. **Listado de Eventos**: Cliente consulta eventos disponibles
3. **Detalle de Evento**: Usuario selecciona un evento y ve detalles
4. **Selección de Asientos**: Usuario selecciona de 1 a 4 asientos
5. **Bloqueo de Asientos**: Al avanzar, los asientos se bloquean temporalmente
   - Backend obtiene asientos de la sesión y los envía al proxy
   - Proxy bloquea en Redis de la cátedra
6. **Carga de Nombres**: Usuario ingresa nombres y apellidos para cada asiento
7. **Confirmación de Venta**: Usuario confirma la compra
   - Backend crea venta local (estado PENDIENTE)
   - Backend llama al proxy para confirmar con la cátedra
   - Proxy confirma con la cátedra vía REST
   - Backend actualiza venta (EXITOSA/FALLIDA) y limpia sesión
8. **Resultado**: Usuario recibe confirmación o notificación de error

### Flujo de Sincronización de Eventos (Kafka)

1. **Kafka publica mensaje**: Cátedra publica cambio en topic `eventos-actualizacion`
2. **Proxy consume mensaje**: `EventoKafkaConsumer` recibe la notificación
3. **Proxy notifica al Backend**: POST `/api/admin/eventos/sincronizar` (con JWT)
4. **Backend sincroniza**: Consulta eventos de la cátedra y actualiza BD local
   - Crea/actualiza eventos
   - Elimina eventos obsoletos
   - Marca eventos expirados

## 🎯 Funcionalidades Principales

### Gestión de Usuarios
- ✅ Registro de usuarios sin activación por email
- ✅ Autenticación JWT
- ✅ Búsqueda de usuarios (opcional, requiere Elasticsearch)

### Gestión de Eventos
- ✅ Sincronización automática de eventos desde la cátedra
- ✅ Notificaciones en tiempo real de cambios (Kafka)
- ✅ Filtrado de eventos cancelados y expirados
- ✅ Limpieza automática de sesiones asociadas a eventos cancelados/expirados

### Gestión de Sesiones
- ✅ Sesiones compartidas entre múltiples clientes
- ✅ Expiración automática después de 30 minutos de inactividad
- ✅ Persistencia del estado de selección
- ✅ Gestión de asientos seleccionados y nombres de personas

### Gestión de Asientos
- ✅ Consulta de mapa de asientos desde Redis (vía proxy)
- ✅ Bloqueo temporal de asientos
- ✅ Validación de disponibilidad antes de venta

### Gestión de Ventas
- ✅ Bloqueo temporal de asientos (5 minutos)
- ✅ Manejo de conflictos en ventas concurrentes
- ✅ Reintentos automáticos para ventas pendientes
- ✅ Validación de disponibilidad antes de venta
- ✅ Estados: PENDIENTE, EXITOSA, FALLIDA

### Arquitectura Hexagonal
- ✅ Separación clara entre capas (web, service, repository)
- ✅ Servicios de dominio independientes de frameworks
- ✅ DTOs para comunicación entre capas

### MVVM en Mobile
- ✅ Separación Model-View-ViewModel
- ✅ StateFlow/SharedFlow para estado reactivo
- ✅ Effects para eventos unidireccionales
- ✅ ViewModels testables e independientes de UI

## 🧪 Pruebas


**Ejecutar tests de integración Java:**

```bash
cd backend
./mvnw verify
```

## 📦 Despliegue

### Producción

#### Backend
```bash
cd backend
./mvnw -Pprod clean package
java -jar target/*.jar --spring.profiles.active=prod
```

#### Proxy
```bash
cd proxy
./mvnw clean package
java -jar target/proxy-*.jar --spring.profiles.active=prod
```

### Docker

#### Backend
```bash
cd backend
docker build -t eventos-backend .
docker run -p 8080:8080 eventos-backend
```

#### Proxy
```bash
cd proxy
docker build -t eventos-proxy .
docker run -p 8081:8081 eventos-proxy
```

## 📚 Documentación Adicional

- [Backend README](backend/README.md) - Documentación detallada del backend
- [Proxy README](proxy/README.md) - Documentación detallada del proxy
- [Registro contra la Cátedra](backend/docs/registro-catedra.md) - Guía para registrar el backend



## 🛠️ Tecnologías Utilizadas

- **Backend**: Spring Boot, JHipster, JPA/Hibernate, PostgreSQL, Elasticsearch (opcional)
- **Proxy**: Spring Boot, Kafka Consumer, Redis Client, RestTemplate
- **Cliente Móvil**: Kotlin Multiplatform, Compose Multiplatform, Ktor Client, StateFlow/SharedFlow
- **Autenticación**: JWT
- **Comunicación**: REST APIs, Kafka, Redis
- **Testing**: JUnit, Cypress (E2E)
- **Build**: Maven, Gradle

## 📝 Consideraciones Importantes

- Las sesiones expiran a los 30 minutos de inactividad (parametrizable)
- El bloqueo de asientos dura 5 minutos
- Se pueden seleccionar hasta 4 asientos por sesión
- Las ventas pendientes se reintentan automáticamente (máximo 5 intentos)
- El sistema maneja múltiples instancias del backend
- El registro de usuarios no requiere activación por email
- Elasticsearch es opcional: el sistema funciona sin él
- La app móvil se comunica solo con el backend (no directamente con el proxy)
- El backend actúa como orquestador y se comunica con el proxy cuando es necesario

## 🏛️ Arquitectura del Proyecto

### Backend
- **Arquitectura Hexagonal**: Separación clara entre web, service, repository
- **Servicios de dominio**: Lógica de negocio independiente
- **DTOs**: Comunicación entre capas
- **Configuración**: RestTemplate con builder para proxy, factory para otros casos

### Proxy
- **Servicio intermediario**: Acceso a Kafka y Redis de la cátedra
- **Consumidor de Kafka**: Notificaciones de cambios en eventos
- **Cliente Redis**: Consulta de estado de asientos
- **Comunicación con Backend**: JWT para autenticación

### Mobile
- **MVVM**: Model-View-ViewModel pattern
- **Estado Reactivo**: StateFlow/SharedFlow
- **Efectos Unidireccionales**: SharedFlow para eventos
- **Compose Multiplatform**: UI declarativa
- **Ktor Client**: Comunicación HTTP con backend

## 👥 Autores

- Valencora
