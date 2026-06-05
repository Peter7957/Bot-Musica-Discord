# Notas del Proyecto DJ Bot - Sesion de Desarrollo

## Cambios Realizados

### 1. Dockerfile actualizado a Java 25
- **Antes:** `eclipse-temurin:21-jre` / `maven:3.9.6-eclipse-temurin-21`
- **Despues:** `eclipse-temurin:25-jre` / `maven:3.9.6-eclipse-temurin-25`
- **Razon:** El POM usa `java.version=25`, asi que el contenedor debe coincidir.

### 2. CORS eliminado de la API REST
- **Archivo:** `MusicRestController.java`
- **Cambio:** Se elimino `@CrossOrigin(origins = "http://localhost:4200")` y su import.
- **Razon:** La parte de Discord no necesita la API REST de momento. Se puede volver a agregar cuando el frontend este listo.

### 3. SlashCommandListener - deferReply() agregado
- **Comandos afectados:** `/play` y `/mix`
- **Que hace:** Le dice a Discord "recibido, estoy procesando" y extiende el timeout de 3 segundos a 15 minutos.
- **Por que:** Estos comandos hacen I/O con YouTube/Lavaplayer y pueden tardar mas de 3 segundos.
- **Como funciona:**
  1. `event.deferReply().queue()` al inicio del comando (ANTES de cualquier validacion pesada)
  2. Luego se usan callbacks con `event.getHook().sendMessage(...)` o `event.getHook().editOriginal(...)`
- **Errores corregidos:**
  - Error de compilacion en linea 1 (`Slac` suelto al final del package)
  - Uso incorrecto de `event.reply()` despues de `deferReply()` en `/play`
  - Falta de `return` en `/borrar` cuando no hay cancion (enviaba doble respuesta)

## Estado de Spring Boot 4.0.6

- **Version real:** Si existe en Maven Central y start.spring.io
- **spring-boot-h2console:** Es un artifact valido en el BOM de Spring Boot 4.x. En versiones 4.x la consola H2 fue extraida como starter independiente.
- **Dependencia validada:** `org.springframework.boot:spring-boot-h2console:4.0.6` existe en Maven Central.
- **Nota:** Las versiones 4.x de Spring Boot son relativamente nuevas (2026). Algunas dependencias de terceros podrian no estar actualizadas todavia.

## Funcionalidad de Busqueda por Nombre (Lavaplayer)

- **Disponible:** Si, Lavaplayer soporta busqueda por nombre.
- **Prefijos soportados:**
  - `ytsearch: nombre de cancion` → Busca en YouTube
  - `scsearch: nombre de cancion` → Busca en SoundCloud (si esta registrado)
- **Implementacion actual:** Tu `MusicManager` ya registra `YoutubeAudioSourceManager` de `dev.lavalink.youtube`.
- **Para agregar busqueda automatica:** Detectar si el input es URL o texto, y prependear `ytsearch:` si no es URL:
  ```java
  String input = event.getOption("url").getAsString();
  String trackUrl = (input.startsWith("http")) ? input : "ytsearch: " + input;
  ```

## Tareas Pendientes para Hosting

### Bloqueadores para Produccion
1. **Persistencia de datos:**
   - `spring.jpa.hibernate.ddl-auto=create-drop` en `application.properties` BORRA la base de datos en cada reinicio.
   - **Solucion:** Cambiar a `update` y montar un volumen Docker para `./data`, o migrar a PostgreSQL/MySQL.

2. **Health Check:**
   - Agregar un endpoint `/health` o incluir `spring-boot-starter-actuator` para que el orquestador del hosting pueda verificar que el bot sigue vivo.

3. **Variables de entorno:**
   - Asegurarse de configurar en el hosting:
     - `DISCORD_TOKEN`
     - `DISCORD_GUILD_ID`
     - `DISCORD_GUILD_ID_TEST`

### Mejoras Recomendadas
1. **Manejo de errores global en Discord:**
   - `/play` y `/mix` solo atrapan `NoVoiceChannelException` y `NoGuildChannelException`. Agregar un `catch (Exception e)` general para evitar "la aplicacion no respondio" en casos inesperados.

2. **Logging:**
   - Revisar que los logs se escriban correctamente en el contenedor (stdout/stderr) para poder debuggear en el hosting.

3. **Volumen Docker:**
   - Si se mantiene H2, agregar un volumen persistente en `docker run`:
     ```bash
     -v dj-data:/app/data
     ```

## Comandos de Discord Disponibles

| Comando | Descripcion | Necesita deferReply? |
|---------|-------------|---------------------|
| `/ping` | Calcula latencia | No |
| `/play <url>` | Reproduce desde URL | **Si** |
| `/escuchando` | Muestra cancion actual | No |
| `/skip` | Salta cancion | No |
| `/anterior` | Cancion anterior | No |
| `/pause` | Pausa musica | No |
| `/reanudar` | Reanuda musica | No |
| `/stop` | Detiene y limpia cola | No |
| `/queue` | Muestra cola | No |
| `/volume` | Ajusta volumen | No |
| `/mix` | Reproduce mix de H2 | **Si** |
| `/borrar` | Borra cancion actual | No |

## Tecnologias Usadas

- Spring Boot 4.0.6
- Java 25
- JDA 6.3.0 (Discord API)
- Lavaplayer 2.2.6 (motor de audio)
- dev.lavalink.youtube (source manager de YouTube)
- H2 Database (embebida)
- Maven 3.9+
- Docker + eclipse-temurin:25

## Nota Importante

**No ejecutar `create-drop` en produccion.** Los datos del historial de canciones se perderan en cada reinicio del contenedor.

## Cambios para Despliegue en Hetzner (2026-05-05)

### 1. Base de datos migrada a PostgreSQL
- **Archivo:** `pom.xml`
- **Cambio:** Se reemplazó H2 + spring-boot-h2console por PostgreSQL (`org.postgresql:postgresql`)
- **Razon:** H2 no es adecuada para produccion. PostgreSQL es robusta y persistente.

### 2. Configuracion de PostgreSQL
- **Archivo:** `application.properties`
- **Cambios:**
  - `spring.datasource.url` apunta a PostgreSQL via variable de entorno
  - `spring.jpa.hibernate.ddl-auto=update` (antes era `create-drop`)
  - Dialecto de PostgreSQL configurado
- **Razon:** `create-drop` borraria la BD en cada reinicio. `update` mantiene los datos.

### 3. Docker Compose creado
- **Archivo:** `docker-compose.yml`
- **Servicios:**
  - `app`: El bot de Discord (Spring Boot)
  - `db`: PostgreSQL 16 Alpine
- **Volumen:** `postgres_data` para persistencia de la BD
- **Red:** `dj-network` para comunicacion entre contenedores

### 4. Variables de entorno centralizadas
- **Archivo:** `.env.example`
- **Variables:**
  - `DISCORD_TOKEN`
  - `DISCORD_GUILD_ID`
  - `DISCORD_GUILD_ID_TEST`
  - `DB_USER`
  - `DB_PASSWORD`

### 5. Healthcheck para PostgreSQL
- **Archivo:** `docker-compose.yml`
- **Cambio:** Se agrego `healthcheck` al servicio `db` usando `pg_isready`.
- **Cambio:** `depends_on` del servicio `app` ahora espera a que `db` este `healthy`.
- **Razon:** Evita que Spring Boot intente conectarse antes de que PostgreSQL este listo.

## Instrucciones para Desplegar en Hetzner

### 1. Preparar el servidor
```bash
# En tu VPS de Hetzner
sudo apt update && sudo apt install -y docker.io docker-compose-plugin git
sudo usermod -aG docker $USER
# Re-loguearte o ejecutar: newgrp docker
```

### 2. Subir el proyecto
```bash
git clone <tu-repo>
cd DJ
cp .env.example .env
# Editar .env con tus credenciales reales
nano .env
```

### 3. Levantar los servicios
```bash
docker compose up -d --build
```

### 4. Verificar logs
```bash
docker compose logs -f app
docker compose logs -f db
```

### 5. Comandos utiles
```bash
# Detener
docker compose down

# Detener y borrar datos (⚠️ cuidado)
docker compose down -v

# Reconstruir despues de cambios
docker compose up -d --build
```

### Notas importantes
- **Persistencia:** Los datos de PostgreSQL se guardan en el volumen `postgres_data`. No se pierden al reiniciar el contenedor.
- **Restart policy:** `unless-stopped` asegura que el bot se reinicie si falla.
- **Seguridad:** Cambia `DB_PASSWORD` por una contraseña segura antes de desplegar.
- **Firewall:** Asegurate de que el puerto 8080 no este expuesto a internet si no usas la API REST (por defecto solo la app interna lo usa).

---
*Documento actualizado el 2026-05-05*
