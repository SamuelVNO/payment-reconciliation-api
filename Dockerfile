# syntax=docker/dockerfile:1

# ---- Estágio 1: build com JDK + Maven (não vai para a imagem final) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# pom.xml antes do código: se só o código mudar, a camada de dependências vem do cache
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests
# Separa o jar em camadas: dependências (mudam pouco) e código da app (muda sempre)
RUN java -Djarmode=tools -jar target/payment-reconciliation-api-*.jar extract --layers --launcher --destination extracted

# ---- Estágio 2: só o JRE + a aplicação ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
RUN mkdir -p /app/payout-inbox && chown app:app /app/payout-inbox
# Nunca rodar como root dentro do container
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
# MaxRAMPercentage: a JVM respeita o limite de memória do container em vez da RAM da máquina
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
