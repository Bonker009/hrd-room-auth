# HRD Room Service

Spring Boot service with PostgreSQL, runnable locally with Docker Compose.

## Prerequisites

- Docker Desktop (with Compose v2)
- Java 21 (for local Gradle runs)

## Run with Docker Compose

1) Copy environment template (first time only):

```bash
cp .env.example .env
```

1) Start services:

```bash
docker compose up --build
```

The app is available at `http://localhost:8080`.

## Stop services

```bash
docker compose down
```

## Stop and remove database data

```bash
docker compose down -v
```

## Optional environment overrides

You can override defaults using environment variables:

- `APP_PORT` (default `8080`)
- `DB_PORT` (default `5432`)
- `POSTGRES_DB` (default `hrd_room_service`)
- `POSTGRES_USER` (default `postgres`)
- `POSTGRES_PASSWORD` (default `postgres`)

Example:

```bash
POSTGRES_PASSWORD=secret docker compose up --build
```
