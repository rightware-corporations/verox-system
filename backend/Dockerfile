FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY backend/.mvn .mvn
COPY backend/mvnw backend/mvnw.cmd backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY backend/src src
RUN ./mvnw -B -DskipTests clean package -Pproduction

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 verox
COPY --from=build /app/target/*.jar /app/verox-backend.jar

USER verox
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/verox-backend.jar"]
