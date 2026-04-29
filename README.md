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

## Error Handling

This project centralizes API exception mapping in `GlobalExceptionHandler` so controllers and services can
throw exceptions without repeating response-building code.

### GlobalExceptionHandler flow

`src/main/java/org/kshrd/hrdroomservice/api/exception/GlobalExceptionHandler.java` is annotated with
`@RestControllerAdvice`, so Spring applies it to all controllers.

Current mappings:

- `ApiException` -> HTTP status from the exception
- `MethodArgumentNotValidException` -> `400 Bad Request`
- `AuthenticationException` -> `401 Unauthorized`
- `AccessDeniedException` -> `403 Forbidden`

All error responses are now returned using the same `ApiResponse` envelope used by success
responses, so clients can parse one stable shape everywhere. Common fields include:

- `errorCode` (for machine-readable handling)
- `details.field` (when a specific input field caused the issue)
- `details.violations` (validation errors list)

### When to throw ApiException

Use the factory methods in `src/main/java/org/kshrd/hrdroomservice/api/exception/ApiException.java`
from service/business logic:

- `ApiException.badRequest("...")` for invalid input/business preconditions
- `ApiException.notFound("...")` when a resource cannot be found
- `ApiException.conflict("...")` for duplicate/conflicting state
- `ApiException.forbidden("...")` for business-level access denial

Example in service code:

```java
if (classroom == null) {
    throw ApiException.notFound("Classroom not found");
}
```

Controllers should generally avoid broad try/catch for these cases and let
`GlobalExceptionHandler` produce the response consistently.

### Standard error response examples

Business not found (`ApiException.notFound`):

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Classroom not found",
  "errorCode": "NOT_FOUND",
  "path": "/api/v4/classrooms/..."
}
```

Validation failure (`@Valid` + `MethodArgumentNotValidException`):

```json
{
  "success": false,
  "statusCode": 400,
  "message": "email: must be a well-formed email address; password: must not be blank",
  "errorCode": "VALIDATION_ERROR",
  "path": "/api/v4/auth/register",
  "details": {
    "violations": [
      { "field": "email", "message": "must be a well-formed email address" },
      { "field": "password", "message": "must not be blank" }
    ]
  }
}
```

Authentication / authorization:

```json
{
  "success": false,
  "statusCode": 401,
  "message": "Authentication is required for this resource.",
  "errorCode": "UNAUTHORIZED",
  "path": "/api/v4/..."
}
```

```json
{
  "success": false,
  "statusCode": 403,
  "message": "You do not have permission to perform this action.",
  "errorCode": "ACCESS_DENIED",
  "path": "/api/v4/..."
}
```

### Extending handlers

If you need a new standardized error shape:

1. Add a new `@ExceptionHandler(...)` method in `GlobalExceptionHandler`.
2. Build the response with `ApiResponse.error(...)`, and set `details` when extra context is useful.
3. Reuse an existing `errorCode` or introduce a new one intentionally.

Known gap: malformed JSON (`HttpMessageNotReadableException`) is not handled explicitly in
`GlobalExceptionHandler` right now. Depending on security routing, it may be forwarded to `/error`.
If your API contract expects a strict `400` JSON error envelope for malformed JSON, add a dedicated
`@ExceptionHandler(HttpMessageNotReadableException.class)` and map it to `BAD_REQUEST`.

## OpenAPI Contract Snapshot

The API contract is snapshot-tested in:

- `src/test/java/org/kshrd/hrdroomservice/api/OpenApiContractIT.java`
- `src/test/resources/openapi/openapi.json`

### Updating the snapshot

When API changes are intentional:

```bash
./gradlew --no-daemon test --tests "org.kshrd.hrdroomservice.api.OpenApiContractIT" -Dopenapi.update=true
```

Then run the same test again without `-Dopenapi.update=true` to confirm it is stable, and commit the
updated `src/test/resources/openapi/openapi.json`.
