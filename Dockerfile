FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew && ./gradlew build -x test

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
ENV TZ="Europe/Oslo"
EXPOSE 8081
CMD ["-jar", "app.jar"]