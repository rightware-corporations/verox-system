FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests clean package -Pproduction

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 verox
COPY --from=build /app/target/*.jar /app/verox-backend.jar

USER verox
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/verox-backend.jar"]
