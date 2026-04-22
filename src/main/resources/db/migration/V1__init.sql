CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS academic_years (
    academic_year_id    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    generation          INTEGER,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ARCHIVED',
    start_date          TIMESTAMP    NOT NULL,
    end_date            TIMESTAMP    NOT NULL,
    version             BIGINT                DEFAULT 0,
    created_by          UUID,
    updated_by          UUID,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_academic_year_status ON academic_years (status);

CREATE TABLE IF NOT EXISTS assessments (
    assessment_id       UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    assessment_date     DATE,
    is_result_released  BOOLEAN               DEFAULT FALSE,
    is_quiz             BOOLEAN               DEFAULT FALSE,
    max_attempts        INTEGER,
    start_time          TIME,
    end_time            TIME,
    subject_id          UUID,
    academic_year_id    UUID         REFERENCES academic_years (academic_year_id),
    version             BIGINT                DEFAULT 0,
    created_by          UUID,
    updated_by          UUID,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assessment_academic_year ON assessments (academic_year_id);

CREATE TABLE IF NOT EXISTS courses (
    course_id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code                    VARCHAR(50),
    name                    VARCHAR(255),
    type                    VARCHAR(20) NOT NULL,
    academic_year_id        UUID        REFERENCES academic_years (academic_year_id),
    prerequisite_course_id  UUID        REFERENCES courses (course_id),
    description             TEXT,
    is_archived             BOOLEAN      NOT NULL DEFAULT FALSE,
    version                 BIGINT               DEFAULT 0,
    created_by              UUID,
    updated_by              UUID,
    created_at              TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_course_academic_year ON courses (academic_year_id);
CREATE INDEX IF NOT EXISTS idx_course_type          ON courses (type);

CREATE TABLE IF NOT EXISTS classrooms (
    classroom_id        UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    class_name          VARCHAR(255) NOT NULL,
    classroom_abbre     VARCHAR(50),
    description         TEXT,
    image               VARCHAR(512),
    academic_year_id    UUID         REFERENCES academic_years (academic_year_id),
    created_by          UUID,
    updated_by          UUID,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id       UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id          UUID      NOT NULL,
    course_id           UUID      NOT NULL REFERENCES courses (course_id),
    academic_year_id    UUID      NOT NULL,
    enrolled_at         TIMESTAMP NOT NULL DEFAULT now(),
    grade               DOUBLE PRECISION,
    is_passed           BOOLEAN            DEFAULT FALSE,
    completed_at        TIMESTAMP,
    is_terminated       BOOLEAN            DEFAULT FALSE,
    terminated_at       TIMESTAMP,
    termination_reason  TEXT,
    version             BIGINT             DEFAULT 0,
    created_by          UUID,
    updated_by          UUID,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP,
    CONSTRAINT uniq_student_course UNIQUE (student_id, course_id)
);

CREATE INDEX IF NOT EXISTS idx_enrollment_student       ON enrollments (student_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_course        ON enrollments (course_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_academic_year ON enrollments (academic_year_id);

CREATE TABLE IF NOT EXISTS assessment_classroom (
    id              UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    assessment_id   UUID      NOT NULL REFERENCES assessments (assessment_id),
    classroom_id    UUID      NOT NULL REFERENCES classrooms  (classroom_id),
    assigned_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uniq_assessment_classroom UNIQUE (assessment_id, classroom_id)
);

CREATE TABLE IF NOT EXISTS classroom_student (
    id              UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    classroom_id    UUID      NOT NULL REFERENCES classrooms (classroom_id),
    student_id      UUID      NOT NULL,
    assigned_at     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uniq_classroom_student UNIQUE (classroom_id, student_id)
);

CREATE TABLE IF NOT EXISTS classroom_subject (
    id              UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    classroom_id    UUID NOT NULL REFERENCES classrooms (classroom_id),
    subject_id      UUID NOT NULL,
    CONSTRAINT uniq_classroom_subject UNIQUE (classroom_id, subject_id)
);

CREATE TABLE IF NOT EXISTS classroom_teacher (
    id              UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    classroom_id    UUID      NOT NULL REFERENCES classrooms (classroom_id),
    teacher_id      UUID      NOT NULL,
    assigned_at     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uniq_classroom_teacher UNIQUE (classroom_id, teacher_id)
);
