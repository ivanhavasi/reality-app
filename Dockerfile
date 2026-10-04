FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew :application:quarkusBuild --no-daemon --console=plain -x test

FROM eclipse-temurin:21-jre
WORKDIR /work
COPY --from=build --chown=1001:root /src/application/build/quarkus-app/ /work/
EXPOSE 8080
USER 1001
# Leaves headroom under the 512Mi pod limit
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dquarkus.http.host=0.0.0.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /work/quarkus-run.jar"]
