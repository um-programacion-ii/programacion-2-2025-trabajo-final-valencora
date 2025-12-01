## Registro manual contra el servicio de la cátedra

Este documento describe el procedimiento recomendado para registrar el backend y almacenar el token requerido para consumir los endpoints de la cátedra.

### Red ZeroTier

El servicio de la cátedra está disponible en la red ZeroTier con el rango de IP `192.168.194.0/24`.

**Servicios disponibles:**
- **Servicio de la cátedra**: `http://192.168.194.250:8080`
- **Redis**: `192.168.194.250:6379` (usado por el servicio proxy)
- **Kafka**: `192.168.194.250:9092` (usado por el servicio proxy)

**Para obtener tu IP en ZeroTier:**
```bash
# En Linux/Mac
ip addr show zt* | grep "inet " | awk '{print $2}' | cut -d/ -f1

# O verifica en la interfaz de ZeroTier
```

### 1. Solicitar credenciales

1. Envía a la cátedra los datos de tu backend (nombre del proyecto, URL pública, mail de contacto).
2. Recibirás un **token de autenticación** y la URL base del servicio.

### 2. Registrar el backend (ejemplo)

**Nota:** El servicio de la cátedra está disponible en la red ZeroTier en `http://192.168.194.250:8080`

```bash
curl -X POST "http://192.168.194.250:8080/api/v1/agregar_usuario" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "valen",
    "password": "valen123",
    "firstName": "valentin",
    "lastName": "coratollo",
    "email": "v.coratolo@alumno.um.edu.ar",
    "nombreAlumno": "valentin coratolo",
    "descripcionProyecto": "Proyecto de valentin coratolo"
  }'
```

**Importante:** Reemplaza `192.168.194.18` con tu IP en la red ZeroTier (rango 192.168.194.0/24).

La respuesta incluye el token que deberás configurar en el backend.

### 3. Guardar el token

**Opción recomendada (variable de entorno):**

```bash
export CATEDRA_AUTH_TOKEN="token-entregado-por-la-catedra"
./mvnw
```

**Opción alternativa (archivo `application-*.yml`):**

```yaml
application:
  catedra:
    auth-token: ${CATEDRA_AUTH_TOKEN:token-temporal}
```

### 4. Actualizar el token en caliente

El backend expone el endpoint administrativo (requiere rol `ROLE_ADMIN`):

- `PUT /api/admin/catedra-token`

Body:

```json
{ "token": "token-nuevo" }
```

- `DELETE /api/admin/catedra-token` elimina el token cargado en memoria.
- `GET /api/admin/catedra-token` indica si hay un token vigente.

> Nota: El token se almacena en memoria. Para que persista entre reinicios, mantenlo en la variable de entorno `CATEDRA_AUTH_TOKEN`.

### 5. Verificar conectividad

1. Inicia el backend (`./mvnw`).
2. Comprueba el estado:

```bash
curl -H "Authorization: Bearer <token-admin>" http://localhost:8080/api/admin/catedra-token
```

Debe responder `tokenPresent: true`.

### 6. Renovación del token

Cuando la cátedra invalide el token:

1. Repite el paso de registro (sección 2) para obtener uno nuevo.
2. Actualiza `CATEDRA_AUTH_TOKEN` o usa el endpoint `PUT /api/admin/catedra-token`.
3. Verifica nuevamente el estado.

### 7. Errores comunes

| Error                                           | Causa                                                                 | Solución                                                                |
|-------------------------------------------------|-----------------------------------------------------------------------|-------------------------------------------------------------------------|
| `No hay token configurado...`                   | No se cargó token al iniciar                                          | Configura `CATEDRA_AUTH_TOKEN` o usa el endpoint admin                  |
| `Token inválido o expirado para el servicio...` | La cátedra rechazó el token (HTTP 401)                                | Solicita un token nuevo y actualízalo con el endpoint admin             |
| `Debe configurar application.catedra.base-url`  | Falta la URL base en `application-*.yml`                              | Completa `application.catedra.base-url` en el perfil correspondiente    |



