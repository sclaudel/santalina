####
# Dockerfile multi-stage — Lac Plongée
# Stage 1: Build frontend React
# Stage 2: Build Quarkus JVM
# Stage 3: Image runtime légère
####

# ---- Stage 1: Frontend React ----
FROM node:20-alpine AS frontend-build
WORKDIR /app/webui
COPY src/main/webui/package*.json ./
RUN npm ci --silent
COPY src/main/webui/ ./
RUN npm run build

# ---- Stage 2: Backend Quarkus ----
FROM eclipse-temurin:21-jdk AS backend-build
WORKDIR /app

# Gradle wrapper + configs
COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# Pré-téléchargement des dépendances (cache Docker layer)
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Sources Java + resources
COPY src/main/java/ src/main/java/
COPY src/main/resources/ src/main/resources/

# Frontend buildé → dans le classpath Quarkus
COPY --from=frontend-build /app/webui/dist/ src/main/resources/META-INF/resources/

# Build du JAR (sans tests)
RUN ./gradlew build -x test --no-daemon

# ---- Stage 3: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /deployments

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=backend-build --chown=appuser:appgroup /app/build/quarkus-app/lib/ lib/
COPY --from=backend-build --chown=appuser:appgroup /app/build/quarkus-app/*.jar ./
COPY --from=backend-build --chown=appuser:appgroup /app/build/quarkus-app/app/ app/
COPY --from=backend-build --chown=appuser:appgroup /app/build/quarkus-app/quarkus/ quarkus/

EXPOSE 8085

ENV QUARKUS_PROFILE=prod

CMD ["java", \
     "-Dquarkus.http.host=0.0.0.0", \
     "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
     "-jar", "quarkus-run.jar"]

