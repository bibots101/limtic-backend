# Step 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the project files
COPY . .

# Ensure the maven wrapper is executable (fixes the permission error)
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Step 2: Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/limtic-backend-0.0.1-SNAPSHOT.jar app.jar

# Create a directory for uploads
RUN mkdir -p uploads

# Start the application
# We disable SSL because Render handles it for us, and we use the $PORT variable provided by Render.
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT} --server.ssl.enabled=false"]
