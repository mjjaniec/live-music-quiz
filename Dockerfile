# Importing JDK and copying required files
FROM eclipse-temurin:21 AS build

WORKDIR /app
COPY pom.xml .
COPY src src

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn

# Set execution permission for the Maven wrapper
RUN chmod +x ./mvnw
RUN ./mvnw --no-transfer-progress clean package -Pproduction

# Stage 2: Create the final Docker image using OpenJDK 19
FROM eclipse-temurin:21

ENV db_pass $db_pass
ENV db_url $db_url
ENV db_user $db_user
VOLUME /tmp

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8080