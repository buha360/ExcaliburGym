FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY event-contracts ./event-contracts
COPY sauna-service ./sauna-service
COPY backend/pom.xml ./backend/pom.xml
COPY solarium-service/pom.xml ./solarium-service/pom.xml
COPY reporting-service/pom.xml ./reporting-service/pom.xml
COPY openapi ./openapi
RUN mvn -B -pl sauna-service -am package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S excalibur && adduser -S -G excalibur excalibur
WORKDIR /app
COPY --from=build /workspace/sauna-service/target/*.jar /app/app.jar
USER excalibur
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
