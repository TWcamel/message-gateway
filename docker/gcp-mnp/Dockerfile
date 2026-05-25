# Build stage: needs mnp-commons from host Maven repo
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
# Copy host Maven local repo so mnp-commons resolves
ARG MAVEN_REPO=.m2
COPY ${MAVEN_REPO} /root/.m2
COPY pom.xml ./pom.xml
RUN apk add --no-cache maven && \
    mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/gcp-mnp.jar /app/app.jar
ENV JAVA_OPTS="-Xms128m -Xmx256m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
