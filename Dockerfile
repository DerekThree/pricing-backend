FROM eclipse-temurin:21-jdk-jammy AS build-env
WORKDIR /app/pricing-backend
COPY pricing-backend/ .
COPY pricing-engine/src/main/resources/db/migration/ \
    /app/pricing-engine/src/main/resources/db/migration/
COPY pricing-engine/src/main/resources/db/postgresql/ \
    /app/pricing-engine/src/main/resources/db/postgresql/
RUN ./mvnw clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build-env /app/pricing-backend/target/backend-0.0.1-SNAPSHOT.jar ./

CMD ["java", "-jar", "backend-0.0.1-SNAPSHOT.jar"]
