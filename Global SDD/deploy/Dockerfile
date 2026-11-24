# Imagen unica: SPA + backend. Contexto de build = "Global SDD/":
#   docker build -f deploy/Dockerfile -t odonto:dev .

# ---- 1. SPA (Vite) ----------------------------------------------------------
FROM node:24.22.3-alpine AS frontend-build
WORKDIR /build/frontend
# Dependencias antes que fuentes: la capa de npm ci se cachea.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
# tokens.css se importa como ../design-system desde frontend/.
COPY design-system /build/design-system
COPY frontend/ ./
RUN npm run build

# ---- 2. Backend (Gradle) -----------------------------------------------------
FROM eclipse-temurin:21.0.13_9-jdk AS backend-build
WORKDIR /build/backend
COPY backend/gradlew ./
COPY backend/gradle gradle
COPY backend/settings.gradle.kts backend/build.gradle.kts ./
# Cache de dependencias antes de copiar las fuentes.
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY backend/src/main src/main
# La SPA queda dentro del jar (se sirve desde el mismo origen).
COPY --from=frontend-build /build/frontend/dist/ src/main/resources/static/
# Los tests corren en CI (Testcontainers necesita Docker), no en la imagen.
RUN ./gradlew --no-daemon bootJar -x test

# ---- 3. Runtime (JRE, sin herramientas de build) ------------------------------
FROM eclipse-temurin:21.0.13_9-jre AS runtime
ENV TZ=UTC
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=backend-build --chown=app:app /build/backend/build/libs/app.jar /app/app.jar
USER app
EXPOSE 8080
# bash + /dev/tcp: no depende de curl ni wget. UP solo aparece en el cuerpo del 200.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\n\r\n" >&3 && grep -q UP <&3'
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
