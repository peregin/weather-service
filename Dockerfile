# ---- Stage 1: Build a minimal custom JRE on a glibc image
FROM eclipse-temurin:21-jdk-jammy AS jre-builder

WORKDIR /work
COPY build/libs/service.jar /work/service.jar

# Inspect module deps, then build a trimmed JRE.
# Add jdk.crypto.ec for TLS and jdk.unsupported for sun.misc.Unsafe (often needed by Scala/Play).
RUN jdeps \
      --multi-release 21 \
      --ignore-missing-deps \
      --print-module-deps /work/service.jar > /work/deps.txt && \
    $JAVA_HOME/bin/jlink \
      --add-modules $(cat /work/deps.txt),jdk.crypto.ec,jdk.unsupported \
      --strip-debug \
      --no-header-files \
      --no-man-pages \
      --compress=2 \
      --output /customjre && \
    /customjre/bin/java -version

# ---- Stage 2: Ultra-small runtime (glibc)
FROM gcr.io/distroless/base-debian12

# Use the built-in nonroot user (UID/GID 65532)
USER 65532:65532

WORKDIR /app
COPY --from=jre-builder /customjre /jre
COPY build/libs/service.jar /app/service.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75" \
    TZ=UTC \
    PATH="/jre/bin:${PATH}"

EXPOSE 9015

# Use absolute path (no shell in Distroless; PATH lookup isn’t used by Docker exec)
ENTRYPOINT ["/jre/bin/java", "-Duser.timezone=UTC", "-jar", "/app/service.jar"]
