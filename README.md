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

Response bodies are `ProblemDetail`-based and are built in
`src/main/java/org/kshrd/hrdroomservice/api/exception/ProblemDetailSupport.java`.
Common custom properties include:

- `errorCode` (for machine-readable handling)
- `field` (when a specific input field caused the issue)
- `violations` (validation errors list)

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
  "type": "about:blank",
  "title": "Resource not found",
  "status": 404,
  "detail": "Classroom not found",
  "instance": "/api/v4/classrooms/...",
  "errorCode": "NOT_FOUND"
}
```

Validation failure (`@Valid` + `MethodArgumentNotValidException`):

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "email: must be a well-formed email address; password: must not be blank",
  "instance": "/api/v4/auth/register",
  "errorCode": "VALIDATION_ERROR",
  "violations": [
    { "field": "email", "message": "must be a well-formed email address" },
    { "field": "password", "message": "must not be blank" }
  ]
}
```

Authentication / authorization:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required for this resource.",
  "instance": "/api/v4/...",
  "errorCode": "UNAUTHORIZED"
}
```

```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "You do not have permission to perform this action.",
  "instance": "/api/v4/...",
  "errorCode": "ACCESS_DENIED"
}
```

### Extending handlers

If you need a new standardized error shape:

1. Add a new `@ExceptionHandler(...)` method in `GlobalExceptionHandler`.
2. Build the response with `ProblemDetailSupport.simple(...)` or add a new helper there.
3. Reuse an existing `errorCode` or introduce a new one intentionally.

Known gap: malformed JSON (`HttpMessageNotReadableException`) is not handled explicitly in
`GlobalExceptionHandler` right now. Depending on security routing, it may be forwarded to `/error`.
If your API contract expects a strict `400` ProblemDetail for malformed JSON, add a dedicated
`@ExceptionHandler(HttpMessageNotReadableException.class)` and map it to `BAD_REQUEST`.
