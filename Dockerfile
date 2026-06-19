# ---------- build stage ----------
# Compiles the app and runs the test suite (build fails if any test fails).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x build.sh && ./build.sh

# ---------- run stage ----------
# Slim JRE image that just runs the packaged jar in web mode.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/out/pos-system.jar ./pos-system.jar

# Render (and most PaaS) inject $PORT; the app reads it, defaulting to 8080.
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-jar", "pos-system.jar", "--web"]
