# Multi-stage build
FROM maven:3.8.5-openjdk-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Финальный образ
FROM openjdk:17-jdk-slim

RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
RUN python3 -m pip install --upgrade pip && \
    pip3 install pandas openpyxl


WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Создаем папку parsing и копируем Python скрипт
RUN mkdir -p /app/parsing
COPY parsing/*.py /app/parsing/
EXPOSE 8080
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Dspring.flyway.enabled=true", \
    "-Dspring.flyway.baseline-on-migrate=true", \
    "-Dspring.flyway.locations=classpath:db/migration", \
    "-jar", "app.jar"]