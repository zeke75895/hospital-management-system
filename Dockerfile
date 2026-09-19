# Stage 1: build the jar with Maven. Uses the full (non-alpine) JDK image - fine, since
# multi-stage builds discard everything from this stage except what's explicitly COPY --from'd
# into the next one; only the runtime stage's base image affects the final image size.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: slim JRE runtime (no compiler/build tooling needed to just run a jar), as a non-root
# user - a compromised app process shouldn't run as root inside its container.
#
# Not the -alpine variant: eclipse-temurin:17-jre-alpine has no published manifest for arm64
# (confirmed by trying it - it fails to resolve on this arm64/Apple Silicon host), which would
# make local builds fail on exactly the kind of machine this is likely to be developed on. This
# tag is still the JRE (not JDK) image - substantially smaller than the build stage - just glibc-
# based rather than musl-based.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
