FROM openjdk:11-jdk AS builder

# tools
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# working directory
RUN useradd --create-home app && \
    mkdir -p /project && \
    chown app /project
WORKDIR /project
USER app

# cache Maven dependencies
COPY --chown=app pom.xml /project/
RUN mvn dependency:go-offline --batch-mode --errors

# do the build
COPY --chown=app src/main/resources /project/src/main/resources/
COPY --chown=app src/main/java /project/src/main/java/
COPY --chown=app src/test/java /project/src/test/java/
RUN mvn clean package --offline --batch-mode --errors

# ------------------------------------------------------------

FROM openjdk:11-jre-slim

RUN adduser --system --home /app app
WORKDIR /app
USER app

EXPOSE 8080
CMD ["java", "-Xmx200m", "-XX:MaxMetaspaceSize=64m", "--illegal-access=deny", "-jar", "cqrs-hotel.jar", "--spring.profiles.active=docker"]

COPY --from=builder /project/target/cqrs-hotel.jar /app/cqrs-hotel.jar
