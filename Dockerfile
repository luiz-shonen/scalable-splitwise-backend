# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the locally built JAR
# Ensure you run 'mvn clean package -s settings-local.xml -DskipTests' before building the image
COPY target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
