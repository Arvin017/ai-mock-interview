FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /app

COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml dependency:go-offline

COPY backend backend

RUN mvn -f backend/pom.xml clean package -DskipTests

CMD ["java","-jar","backend/target/*.jar"]