# syntax=docker/dockerfile:1

# ---- Etapa 1: compilar el JAR con el wrapper de Maven ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Primero solo lo necesario para descargar dependencias: esta capa se reutiliza
# mientras pom.xml no cambie, aunque cambie el código.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Los tests necesitan Docker (Testcontainers), así que se ejecutan fuera de la imagen.
COPY src src
RUN ./mvnw -B -q -DskipTests package \
    && cp target/franchises-api-*.jar app.jar

# ---- Etapa 2: imagen final, solo con el JRE y el JAR ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# La aplicación no se ejecuta como root
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app.jar app.jar

# Ajusta la memoria de la JVM al límite del contenedor
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
