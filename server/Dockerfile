# ── Build stage ────────────────────────────────────────────────────────────
# :server has no native (JNI/CUDA) dependencies — it's a plain JVM Ktor module,
# so this is a single Gradle build stage, not a multi-stage native+JVM pipeline.
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build
COPY . .
RUN chmod +x gradlew && ./gradlew :server:buildFatJar --no-daemon

# ── Runtime stage ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre

# FileWorkflowStore (the default WorkflowStore) persists under $HOME/.graphyn/workflows —
# HOME=/data here so published workflows survive container restarts via the /data volume.
ENV HOME=/data
WORKDIR /app

COPY --from=builder /build/server/build/libs/server-all.jar /app/server.jar

# GRAPHYN_API_KEY is intentionally left unset here — GraphynAuthPlugin treats a *present but
# empty* env var as a real (empty-string) key to match, not "no auth", so `ENV GRAPHYN_API_KEY=""`
# would silently require every caller to send an empty Bearer token. Pass -e GRAPHYN_API_KEY=<secret>
# at runtime to enable auth; omit it (the default) for no-op dev auth.

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
