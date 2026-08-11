# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-noble AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress -DskipTests clean package \
    && cp target/mise-en-dice-*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre-noble AS runtime

ARG VCS_REF=unknown
ARG BUILD_REF=unknown

LABEL org.opencontainers.image.title="Mise en Dice" \
      org.opencontainers.image.description="Private cooking challenge application" \
      org.opencontainers.image.source="https://github.com/venomenon328/mise-en-dice" \
      org.opencontainers.image.revision="$VCS_REF" \
      io.mise-en-dice.source-ref="$BUILD_REF"

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 miseendice \
    && useradd --system --uid 10001 --gid miseendice --home-dir /app --shell /usr/sbin/nologin miseendice

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/application.jar /app/application.jar

USER 10001:10001
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=20 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
