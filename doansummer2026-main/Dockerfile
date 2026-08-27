# Giai đoạn build
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew clean bootJar -x test --no-daemon

# Giai đoạn chay
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENV UPLOAD_ROOT=/app/uploads
RUN mkdir -p /app/uploads/test-results
VOLUME ["/app/uploads"]

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx256m", "-Xss256k", "-XX:MaxMetaspaceSize=150m", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
