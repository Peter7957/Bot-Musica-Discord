# Etapa 1: Construcción (Build) con Java 25
FROM maven:3.9.15-amazoncorretto-25-al2023 AS build
WORKDIR /app

# Optimización: Copiar primero el pom para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar código fuente y compilar
COPY src ./src
RUN mvn package -DskipTests

# Etapa 2: Ejecución (Runtime) con Java 25 JRE
FROM eclipse-temurin:25.0.3_9-jre-noble
WORKDIR /app

# Instalar dependencias necesarias para JDA/jdave/librerías nativas
RUN apt-get update && apt-get install -y \
    libopus0 \
    libstdc++6 \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

# Copiamos el JAR final
COPY --from=build /app/target/*.jar app.jar

# Flags de JVM optimizados para contenedores (Docker/Hetzner)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS --enable-native-access=ALL-UNNAMED -Djava.net.preferIPv4Stack=true -jar app.jar"]
