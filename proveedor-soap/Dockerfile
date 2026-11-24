# Proveedor mayorista externo (sistema legado SOAP). Proyecto independiente de Áurea.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system proveedor && useradd --system --gid proveedor proveedor \
    && mkdir -p /app/data && chown proveedor:proveedor /app/data
COPY --from=build /app/target/*.jar app.jar
COPY contratos contratos
USER proveedor
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
