# ---------- build stage ----------
# Compiles the app and runs the test suite (build fails if any test fails).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x build.sh && ./build.sh

# ---------- run stage ----------
# Slim JRE image. The PostgreSQL JDBC driver is the project's ONLY runtime
# dependency, added here (not at compile time) so the codebase stays
# dependency-free. Docker's ADD fetches it during the image build.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/out/pos-system.jar ./pos-system.jar
ADD https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar ./postgresql.jar

# Render injects $PORT and $DATABASE_URL. With DATABASE_URL set the app uses
# PostgreSQL; without it, it falls back to local CSV files.
ENV PORT=8080
EXPOSE 8080
CMD ["sh", "-c", "java -cp pos-system.jar:postgresql.jar com.rudra.pos.Main --web"]
