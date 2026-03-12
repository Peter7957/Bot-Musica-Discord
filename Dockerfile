# Etapa 1: Construcción (Build) con Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Optimización: Copiar primero el pom para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar código fuente y compilar
COPY src ./src
RUN mvn package -DskipTests

# Etapa 2: Ejecución (Runtime) con Java 21 JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiamos el JAR final
COPY --from=build /app/target/*.jar app.jar

# Incluimos tus flags de optimización y red
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-XX:+UseG1GC", "-jar", "app.jar"]
