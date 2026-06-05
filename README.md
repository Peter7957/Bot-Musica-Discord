# DJ Bot - Bot de Musica para Discord

Bot de musica para Discord con API REST, desarrollado en Java con Spring Boot. Reproduce musica desde YouTube y guarda un historial automatico en base de datos.

---

## Requisitos

- Java 25 o superior
- Maven 3.9+
- Cuenta de Discord y un servidor donde seas administrador
- Token de bot de Discord

---

## Configuracion rapida

### 1. Crear el bot en Discord

1. Ve a [Discord Developer Portal](https://discord.com/developers/applications)
2. Clic en **New Application** → ponle nombre **DJ**
3. Ve a la pestana **Bot** → clic en **Reset Token** → copia el token
4. En **Privileged Gateway Intents** activa:
   - Message Content Intent
   - Server Members Intent
5. Ve a **OAuth2 → URL Generator**:
   - Scopes: `bot`, `applications.commands`
   - Bot Permissions: `Connect`, `Speak`, `Send Messages`, `Use Slash Commands`
6. Copia la URL generada, abrela en tu navegador y agrega el bot a tu servidor

### 2. Variables de entorno

Define estas variables antes de correr el proyecto:

```bash
export DISCORD_TOKEN=tu_token_aqui
export DISCORD_GUILD_ID=id_de_tu_servidor
export DISCORD_GUILD_ID_TEST=id_de_tu_servidor
```

> **Nota:** Para obtener el ID del servidor, activa Modo Desarrollador en Discord (Ajustes → Avanzado), luego clic derecho en el nombre del servidor → **Copiar ID del servidor**.

### 3. Correr el proyecto

```bash
# Compilar y ejecutar
mvn spring-boot:run

# O compilar el JAR
mvn clean package
java -jar target/DJ-0.0.1-SNAPSHOT.jar
```

El bot se conectara a Discord y los comandos estaran disponibles en tu servidor.

---

## Comandos de Discord

| Comando | Descripcion |
|---------|-------------|
| `/ping` | Muestra la latencia del bot |
| `/play <url>` | Reproduce una cancion o playlist de YouTube. Se une al canal de voz |
| `/escuchando` | Muestra la cancion actual |
| `/skip` | Salta a la siguiente cancion |
| `/anterior` | Vuelve a la cancion anterior |
| `/pause` | Pausa la reproduccion |
| `/reanudar` | Reanuda la reproduccion |
| `/stop` | Detiene todo, limpia la cola y sale del canal |
| `/mix` | Carga y reproduce el historial guardado |
| `/borrar` | Elimina la cancion actual del historial |

---

## API REST

La API corre en `http://localhost:8080`

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `/api/music/play` | Reproduce una pista. Body: `{"guildId": "...", "trackUrl": "..."}` |
| POST | `/api/music/skip` | Salta la cancion actual. Body: `{"guildId": "..."}` |
| POST | `/api/music/pause` | Pausa. Body: `{"guildId": "..."}` |
| POST | `/api/music/resume` | Reanuda. Body: `{"guildId": "..."}` |

### Consola H2 (base de datos)

Accede en: `http://localhost:8080/h2-console`

- **JDBC URL:** `jdbc:h2:file:./data/music_db`
- **User:** `sa`
- **Password:** (dejar vacio)

> **Aviso:** Los datos se reinician cada vez que apagas el bot (`ddl-auto=create-drop`). Esto es intencional para pruebas locales.

---

## Docker (opcional)

```bash
# Construir imagen
docker build -t dj-bot .

# Correr container
docker run -e DISCORD_TOKEN=tu_token \
           -e DISCORD_GUILD_ID=tu_guild \
           -e DISCORD_GUILD_ID_TEST=tu_guild \
           -p 8080:8080 \
           dj-bot
```

---

## Estructura del proyecto

```
DJ/
├── src/
│   ├── main/
│   │   ├── java/com/Pixu/DJ/
│   │   │   ├── bot/config/         # Configuracion de Discord (JDA)
│   │   │   ├── controller/         # Endpoints REST
│   │   │   ├── exception/          # Excepciones personalizadas
│   │   │   ├── listeners/          # Escuchadores de comandos slash
│   │   │   ├── models/             # Entidades JPA (TrackEntity)
│   │   │   ├── music/              # Motor de audio (Lavaplayer)
│   │   │   ├── repository/         # Repositorios JPA
│   │   │   └── service/            # Logica de negocio
│   │   └── resources/
│   │       ├── application.properties
│   │       └── test_music.http     # Tests manuales de la API
│   └── test/                       # Tests (vacios por ahora)
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Tecnologias

- **Spring Boot 4.0.3** - Framework principal
- **JDA 6.3.0** - API de Discord para Java
- **Lavaplayer 2.2.6** - Motor de reproduccion de audio
- **H2 Database** - Base de datos embebida para historial
- **Maven** - Gestor de dependencias

---

## Notas importantes

- El bot solo funciona en el servidor configurado (`DISCORD_GUILD_ID`). Los comandos slash no son globales.
- La base de datos se reinicia en cada ejecucion. Si quieres persistir datos, cambia `spring.jpa.hibernate.ddl-auto` a `update` en `application.properties`.
- Asegurate de que el bot tenga permisos de **Conectar** y **Hablar** en los canales de voz.

---

## Autor

Pixu
