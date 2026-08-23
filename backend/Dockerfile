FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -DskipTests clean package -Pproduction

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 verox
COPY --from=build /app/target/*.jar /app/verox-backend.jar

USER verox
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/verox-backend.jar"]
