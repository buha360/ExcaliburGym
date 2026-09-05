FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
COPY event-contracts ./event-contracts
COPY backend ./backend
COPY reporting-service/pom.xml ./reporting-service/pom.xml
COPY sauna-service/pom.xml ./sauna-service/pom.xml
COPY solarium-service/pom.xml ./solarium-service/pom.xml
COPY openapi ./openapi
COPY frontend ./frontend

RUN mvn -B -pl backend -am package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S excalibur && adduser -S -G excalibur excalibur
WORKDIR /app
COPY --from=build /workspace/backend/target/*.jar /app/app.jar
USER excalibur
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

