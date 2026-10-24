FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app && mkdir /data && chown app /data
WORKDIR /app
COPY --from=build /build/target/support-1.0.0.jar app.jar
USER 10001
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
