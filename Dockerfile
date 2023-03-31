FROM maven:3-eclipse-temurin-11 AS build

WORKDIR /app
COPY pom.xml pom.xml
COPY src/ src/

RUN mvn package

FROM eclipse-temurin:11-jre AS runtime
WORKDIR /app

COPY soyka.yaml /app/soyka.yaml
COPY --from=build /app/target/soyka.jar /app/soyka.jar

CMD ["java", "-jar", "/app/soyka.jar", "server", "/app/soyka.yaml"]
