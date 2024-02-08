# Use a base image with Java and Maven pre-installed
FROM maven:3.6.3-openjdk-14 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the project files into the container
COPY . .

# Build the project using Maven
RUN mvn clean package -DskipTests

FROM openjdk:14-slim
WORKDIR /app
COPY --from=build app/server/target/server-*-jar-with-dependencies.jar /app/server.jar

CMD ["java", "-Djava.rmi.server.hostname=0.0.0.0","-Dcom.sun.management.jmxremote","-Dcom.sun.management.jmxremote.port=9999","-Dcom.sun.management.jmxremote.rmi.port=9999","-Dcom.sun.management.jmxremote.authenticate=false","-Dcom.sun.management.jmxremote.ssl=false","-jar", "server.jar"]