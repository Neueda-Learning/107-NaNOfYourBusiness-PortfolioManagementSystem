# Docker Deployment Guide

## Goal
Run the backend API and MySQL with consistent local setup and easy team onboarding.

## Container Strategy
- Build Spring Boot app into a Docker image.
- Run app and MySQL together with Docker Compose.
- Persist DB data with a named volume.

## Expected Files
- `Dockerfile` at repository root for Spring Boot JAR runtime.
- `docker-compose.yml` at repository root for multi-container setup.

## Example `Dockerfile`
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## Example `docker-compose.yml`
```yaml
version: "3.9"
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: portfolio_db
      MYSQL_USER: portfolio_user
      MYSQL_PASSWORD: portfolio_pass
      MYSQL_ROOT_PASSWORD: root_pass
    ports:
      - "3306:3306"
    volumes:
      - portfolio_mysql_data:/var/lib/mysql

  portfolio-api:
    build: .
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/portfolio_db
      SPRING_DATASOURCE_USERNAME: portfolio_user
      SPRING_DATASOURCE_PASSWORD: portfolio_pass
      MARKET_API_BASE_URL: https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default
    ports:
      - "8080:8080"

volumes:
  portfolio_mysql_data:
```

## Run Commands
```powershell
docker compose build
docker compose up -d
docker compose ps
docker compose logs portfolio-api --tail 200
```

## Verify
- API reachable at `http://localhost:8080`.
- Swagger (if enabled) loads.
- CRUD and summary endpoints respond.

## Notes
- Replace sample credentials before shared deployment.
- If startup race occurs, add DB health check and condition-based dependency.

