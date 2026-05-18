FROM gradle:9.5.1-jdk21 AS build
WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew

COPY src src
ARG SKIP_TESTS=true
RUN if [ "$SKIP_TESTS" = "true" ]; then ./gradlew clean bootJar -x test --no-daemon; else ./gradlew clean bootJar --no-daemon; fi

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
