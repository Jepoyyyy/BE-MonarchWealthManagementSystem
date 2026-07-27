# ==============================================================================
# Stage 1: Build & Package (Compile application dependencies and build target JAR)
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven Wrapper configuration and project configuration first to leverage Docker layer cache
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# WORKAROUND: Clean up CRLF line endings in the Maven wrapper script (in case of Windows checkout)
# and ensure execution permission on the wrapper.
RUN tr -d '\r' < mvnw > mvnw.tmp && mv mvnw.tmp mvnw && chmod +x mvnw

# Pre-download dependencies (Offline mode) to optimize Docker rebuild speed
RUN ./mvnw dependency:go-offline -B

# Copy project source code
COPY src ./src

# Compile and package application, totally skipping test compilation and execution
RUN ./mvnw clean package -Dmaven.test.skip=true -B

# ==============================================================================
# Stage 2: Minimal Runtime Environment
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Security Best Practice: Create non-root system user and group (UID/GID 10001)
RUN addgroup -g 10001 -S wmsgroup && \
    adduser -u 10001 -S wmsuser -G wmsgroup

# Copy the compiled JAR artifact from Stage 1
COPY --from=builder /app/target/jdt17wms-*.jar app.jar

# Adjust ownership of application directory to the non-root user
RUN chown -R wmsuser:wmsgroup /app

# Run subsequent commands and executable under the non-privileged user context
USER wmsuser

# EXPOSE PORT INFO:
# While standard HTTP uses port 80, we use port 8080 inside the container because we run
# the process as a non-root user ('wmsuser'). In Linux, binding to ports below 1024 
# (like 80) requires root privileges.
# To serve traffic on HTTP port 80, map host port 80 to container port 8080 at runtime:
#   docker run -p 80:8080 wms-app
EXPOSE 8080

# Environment configurations - Spring Boot Active Profile
ENV SPRING_PROFILES_ACTIVE=local

# Database Connection (Defaulting directly to the Supabase URL configuration)
ENV DB_URL=jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?prepareThreshold=0
ENV DB_USERNAME=postgres.vhssovcozxdwpmsvrvhr
ENV DB_PASSWORD=HXrfUmI15vxDsXrm

# JWT Config (Left blank for runtime injection as requested)
ENV JWT_SECRET=""
ENV JWT_ACCESS_EXPIRATION_MS=9000000
ENV JWT_REFRESH_EXPIRATION_MS=604800000

# Container-aware JVM parameters
# Limits JVM Heap usage to 75% of the total Docker container memory ceiling.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Timezone fallback
ENV TZ=UTC

# Command to bootstrap application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
