# Etapa 1: compilación con Maven y JDK 25
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src src
RUN mvn -B -q -DskipTests package

# Etapa 2: imagen de ejecución, solo con el JRE
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system aurea && useradd --system --gid aurea aurea
COPY --from=build /app/target/*.jar app.jar
USER aurea
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
