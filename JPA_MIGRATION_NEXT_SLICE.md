# JPA Migration Next Slice

This file tracks the next migration slice after `CourseServiceImpl` moved to JPA.

## Current State

- JPA is enabled and validates schema against Flyway migrations.
- Course read/update flow now uses Spring Data JPA repositories.
- Legacy `CourseMapper` remains available but is marked deprecated for transition safety.

## Recommended Next Slice: Academic Year

1. Create `AcademicYearRepository` query methods for:
   - active academic year lookup
   - active count
   - archived/active listing
2. Refactor `AcademicYearServiceImpl` to JPA repository usage.
3. Keep mapper fallback until service-level tests are green.
4. Add tests for activation/archive transitions and ordering.

## Follow-up Slice: Classroom

1. Convert `ClassroomMapper` reads/writes to repository + JPQL/specification.
2. Keep relation-table mappers (`classroom_student`, `classroom_subject`, `classroom_teacher`) until junction handling is modeled cleanly.
3. Add tests for assignment and filtering behavior.

## Removal Gate for MyBatis

Only remove MyBatis starter/config when all mapper injections are gone:

- `AcademicYearMapper`
- `ClassroomMapper`
- `ClassroomRelationMapper`
- `EnrollmentMapper`
- `CourseMapper`
