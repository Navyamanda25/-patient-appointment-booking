-- ============================================================
-- Patient Appointment Booking System
-- Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS clinic_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE clinic_db;

-- Create a dedicated app user (run as root)
-- CREATE USER IF NOT EXISTS 'clinic_user'@'localhost' IDENTIFIED BY 'YourPassword';
-- GRANT ALL PRIVILEGES ON clinic_db.* TO 'clinic_user'@'localhost';
-- FLUSH PRIVILEGES;

-- Drop tables in correct order (appointments first due to foreign keys)
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS doctors;

-- Patients table
CREATE TABLE patients (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(120) UNIQUE,
    dob         DATE,
    gender      VARCHAR(10),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Doctors table
CREATE TABLE doctors (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    specialization  VARCHAR(100),
    phone           VARCHAR(20),
    email           VARCHAR(120) UNIQUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Appointments table
CREATE TABLE appointments (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    patient_id          INT NOT NULL,
    doctor_id           INT NOT NULL,
    appointment_time    DATETIME NOT NULL,
    duration_minutes    INT NOT NULL DEFAULT 30,
    status              ENUM('SCHEDULED','COMPLETED','CANCELLED')
                            NOT NULL DEFAULT 'SCHEDULED',
    notes               VARCHAR(500),
    CONSTRAINT fk_appt_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_appt_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctors(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_doctor_exact_slot
        UNIQUE (doctor_id, appointment_time),
    CONSTRAINT uq_patient_exact_slot
        UNIQUE (patient_id, appointment_time),
    CHECK (duration_minutes IN (15, 30, 45, 60))
);