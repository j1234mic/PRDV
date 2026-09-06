-- ============================================================
-- PRDV - schema initial (compatible MySQL 8.x et H2 MODE=MySQL)
-- Les agregats stockent leurs sous-objets (regles d'agenda, lieux
-- d'exercice...) en colonnes JSON TEXT : un agregat = une ligne.
-- ============================================================

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(190) NOT NULL,
    phone           VARCHAR(32),
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until    DATETIME NULL,
    created_at      DATETIME NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE otp_codes (
    email      VARCHAR(190) PRIMARY KEY,
    code_hash  VARCHAR(100) NOT NULL,
    expires_at DATETIME NOT NULL
);

CREATE TABLE patient_profiles (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT NOT NULL,
    first_name             VARCHAR(100) NOT NULL,
    last_name              VARCHAR(100) NOT NULL,
    birth_date             DATE NULL,
    gender                 VARCHAR(20),
    phone                  VARCHAR(32),
    address_line           VARCHAR(255),
    postal_code            VARCHAR(12),
    city                   VARCHAR(80),
    country                VARCHAR(60),
    social_security_number VARCHAR(32),
    mutual_insurance       VARCHAR(120),
    allergies              TEXT,
    chronic_conditions     TEXT,
    emergency_contact_name VARCHAR(120),
    emergency_contact_phone VARCHAR(32),
    CONSTRAINT uk_patient_user UNIQUE (user_id)
);

CREATE TABLE doctor_profiles (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT NOT NULL,
    full_name              VARCHAR(150) NOT NULL,
    rpps                   VARCHAR(11) NOT NULL,
    specialty              VARCHAR(120) NOT NULL,
    sub_specialties        TEXT,
    description            TEXT,
    sector                 INT NOT NULL,
    consultation_fee_cents INT NOT NULL,
    languages              TEXT,
    locations              TEXT,
    verification_status    VARCHAR(30) NOT NULL,
    rejection_reason       VARCHAR(255),
    CONSTRAINT uk_doctor_user UNIQUE (user_id),
    CONSTRAINT uk_doctor_rpps UNIQUE (rpps)
);
CREATE INDEX idx_doctor_specialty ON doctor_profiles (specialty);

CREATE TABLE doctor_schedules (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_user_id        BIGINT NOT NULL,
    weekly_rules          TEXT NOT NULL,
    exceptions            TEXT,
    min_notice_hours      INT NOT NULL,
    max_horizon_days      INT NOT NULL,
    cancellation_mode     VARCHAR(20) NOT NULL,
    requires_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_schedule_doctor UNIQUE (doctor_user_id)
);

CREATE TABLE appointments (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_user_id         BIGINT NOT NULL,
    doctor_user_id          BIGINT NOT NULL,
    start_time              DATETIME NOT NULL,
    end_time                DATETIME NOT NULL,
    duration_minutes        INT NOT NULL,
    type                    VARCHAR(30) NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    reason                  VARCHAR(500),
    teleconsultation_url    VARCHAR(400),
    cancellation_fee_cents  INT NOT NULL DEFAULT 0,
    cancellation_reason     VARCHAR(255),
    booked_by               VARCHAR(20),
    created_at              DATETIME NOT NULL,
    updated_at              DATETIME NOT NULL
);
CREATE INDEX idx_appointments_doctor_start ON appointments (doctor_user_id, start_time);
CREATE INDEX idx_appointments_patient ON appointments (patient_user_id, start_time);

CREATE TABLE waiting_list_entries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_user_id BIGINT NOT NULL,
    doctor_user_id  BIGINT NOT NULL,
    earliest_date   DATE NOT NULL,
    latest_date     DATE NOT NULL,
    urgent          BOOLEAN NOT NULL DEFAULT FALSE,
    state           VARCHAR(20) NOT NULL,
    created_at      DATETIME NOT NULL
);
CREATE INDEX idx_waitlist_doctor ON waiting_list_entries (doctor_user_id, state);
