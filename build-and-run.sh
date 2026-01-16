#!/bin/bash
# Automation script for restricted network environments

echo "1. Building JAR locally..."
mvn clean package -s settings-local.xml -DskipTests

if [ $? -eq 0 ]; then
    echo "2. Building Docker Image and starting containers..."
    docker compose up --build -d
else
    echo "Error: Local build failed. Containers will not be updated."
    exit 1
fi
