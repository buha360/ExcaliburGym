FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
COPY event-contracts ./event-contracts
COPY backend/pom.xml ./backend/pom.xml
COPY sauna-service/pom.xml ./sauna-service/pom.xml
COPY solarium-service/pom.xml ./solarium-service/pom.xml
COPY reporting-service ./reporting-service
COPY openapi ./openapi

RUN mvn -B -pl reporting-service -am package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S excalibur && adduser -S -G excalibur excalibur
WORKDIR /app
COPY --from=build /workspace/reporting-service/target/*.jar /app/app.jar
USER excalibur
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

