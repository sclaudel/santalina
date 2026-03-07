####
# Dockerfile — Lac Plongée
#
# Pré-requis : lancer build.bat (Windows) ou build.sh (Linux/Mac) avant docker build
#   build.bat compile le frontend React ET le backend Quarkus localement.
#   Ce Dockerfile copie uniquement l'artefact déjà buildé → aucun téléchargement réseau.
####

FROM eclipse-temurin:21-jre-alpine
WORKDIR /deployments

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --chown=appuser:appgroup build/quarkus-app/lib/ lib/
COPY --chown=appuser:appgroup build/quarkus-app/*.jar ./
COPY --chown=appuser:appgroup build/quarkus-app/app/ app/
COPY --chown=appuser:appgroup build/quarkus-app/quarkus/ quarkus/

EXPOSE 8085

ENV QUARKUS_PROFILE=prod

CMD ["java", \
     "-Dquarkus.http.host=0.0.0.0", \
     "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
     "-jar", "quarkus-run.jar"]
