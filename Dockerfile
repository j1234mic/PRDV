# ============ PRDV backend : build multi-stage ============
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Cache des dependances : les poms d'abord, les sources ensuite
COPY pom.xml ./
COPY prdv-core/pom.xml prdv-core/
COPY prdv-adapters-in/pom.xml prdv-adapters-in/
COPY prdv-adapters-out/pom.xml prdv-adapters-out/
COPY prdv-app/pom.xml prdv-app/
RUN mvn -B -q -e dependency:go-offline || true
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre AS run
WORKDIR /app
COPY --from=build /workspace/prdv-app/target/prdv-app.jar app.jar
EXPOSE 8080
# JVM adaptee au conteneur (limitation memoire via cgroup)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
