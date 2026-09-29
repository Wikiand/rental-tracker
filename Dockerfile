FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -q clean package -DskipTests dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency


FROM eclipse-temurin:17-jre

WORKDIR /app

RUN mkdir -p /app/data /app/seed

COPY --from=build /build/target/classes /app/classes
COPY --from=build /build/target/dependency /app/lib

COPY rental-tracker.db /app/seed/rental-tracker.db
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]

CMD ["java", "-cp", "/app/classes:/app/lib/*", "-Drental.tracker.db.url=jdbc:sqlite:/app/data/rental-tracker.db", "com.kood.rentaltracker.transport.Main"]
