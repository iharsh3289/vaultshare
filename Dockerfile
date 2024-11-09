FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /src

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget \
	&& addgroup -S vaultshare \
	&& adduser -S -G vaultshare vaultshare \
	&& mkdir -p /app/data /app/uploads \
	&& chown -R vaultshare:vaultshare /app

COPY --from=builder /src/target/vaultshare-*.jar /app/vaultshare.jar

USER vaultshare

ENV PORT=8080 \
	VAULTSHARE_DATA_DIR=/app/data \
	VAULTSHARE_UPLOAD_DIR=/app/uploads

EXPOSE 8080

VOLUME ["/app/data", "/app/uploads"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
	CMD wget -qO- "http://127.0.0.1:${PORT}/healthz" || exit 1

CMD ["java", "-jar", "/app/vaultshare.jar"]
