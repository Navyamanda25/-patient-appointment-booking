# Patient Appointment Booking System

A console-based application built with **Core Java + JDBC + MySQL**
that allows patients to register and book appointments with doctors,
with complete double-booking prevention.

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Core language |
| MySQL | 8.0 | Database |
| JDBC | mysql-connector-j-9.7.0 | Java-MySQL bridge |
| VS Code | Latest | IDE |

---

## Features

- Register patients with full details (name, phone, email, DOB, gender)
- Add doctors with specialization
- Book appointments with duration (15 / 30 / 45 / 60 minutes)
- **Double-booking prevention** — doctor cannot have overlapping appointments
- Cancel appointments
- View doctor's full schedule by date
- View all appointments for a patient
- Input validation — cannot book in the past, invalid durations blocked
- Graceful error handling — program never crashes on bad input

---

## Project Architecture
User types in terminal
↓
Main.java (UI Layer) — menus, input, output
↓
AppointmentService (Service Layer) — business rules, validation
↓
PatientDao / DoctorDao / AppointmentDao (DAO Layer) — SQL queries
↓
ConnectionManager (JDBC Bridge) — reads db.properties, opens connection
↓
MySQL Database (clinic_db) — patients, doctors, appointments tables

---

patient-booking-console/

├── src/

│   └── com/clinic/booking/

│       ├── model/

│       │   ├── Patient.java

│       │   ├── Doctor.java

│       │   ├── Appointment.java

│       │   └── AppointmentStatus.java

│       ├── dao/

│       │   ├── PatientDao.java

│       │   ├── DoctorDao.java

│       │   └── AppointmentDao.java

│       ├── dao/jdbc/

│       │   ├── JdbcPatientDao.java

│       │   ├── JdbcDoctorDao.java

│       │   └── JdbcAppointmentDao.java

│       ├── service/

│       │   ├── ConnectionManager.java

│       │   └── AppointmentService.java

│       └── ui/

│           └── Main.java

├── lib/

│   └── mysql-connector-j-9.7.0.jar

├── bin/                          ← compiled classes (git ignored)

├── config/

│   ├── db.properties             ← credentials (git ignored)

│   └── db.properties.example

├── sql/

│   └── schema.sql

├── .vscode/

│   └── settings.json

├── .gitignore

└── README.md


---

## Database Schema

**Database:** `clinic_db`

### patients
| Column | Type | Notes |
|---|---|---|
| id | INT PK AUTO_INCREMENT | |
| name | VARCHAR(100) NOT NULL | |
| phone | VARCHAR(20) | |
| email | VARCHAR(120) UNIQUE | |
| dob | DATE | optional |
| gender | VARCHAR(10) | |
| created_at | TIMESTAMP | auto-filled |

### doctors
| Column | Type | Notes |
|---|---|---|
| id | INT PK AUTO_INCREMENT | |
| name | VARCHAR(100) NOT NULL | |
| specialization | VARCHAR(100) | |
| phone | VARCHAR(20) | |
| email | VARCHAR(120) UNIQUE | |
| created_at | TIMESTAMP | auto-filled |

### appointments
| Column | Type | Notes |
|---|---|---|
| id | INT PK AUTO_INCREMENT | |
| patient_id | INT FK | references patients.id |
| doctor_id | INT FK | references doctors.id |
| appointment_time | DATETIME NOT NULL | |
| duration_minutes | INT | 15/30/45/60 only |
| status | ENUM | SCHEDULED/COMPLETED/CANCELLED |
| notes | VARCHAR(500) | optional |

---

## How Double-Booking Prevention Works

When booking a new appointment, the system checks:

```sql
SELECT COUNT(*)
FROM appointments
WHERE doctor_id = ?
  AND status = 'SCHEDULED'
  AND ? < DATE_ADD(appointment_time, INTERVAL duration_minutes MINUTE)
  AND DATE_ADD(?, INTERVAL ? MINUTE) > appointment_time

Two time ranges overlap when:
-> New start < Existing end
-> New end > Existing start
Only SCHEDULED appointments are checked.
CANCELLED appointments do not block new bookings.

#Setup Instructions
===================
1. Clone the Repository
Bash
git clone https://github.com/Navyamanda25/patient-appointment-booking.git
cd patient-appointment-booking

2. Set Up MySQL Database
Log into MySQL as root:
Bash
mysql -u root -p
Run these commands:
SQL

CREATE DATABASE IF NOT EXISTS clinic_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'clinic_user'@'localhost'
  IDENTIFIED BY 'YourPassword';

GRANT ALL PRIVILEGES ON clinic_db.* TO 'clinic_user'@'localhost';
FLUSH PRIVILEGES;

Then run the schema:

Bash
mysql -u clinic_user -p clinic_db < sql/schema.sql

3. Configure Database Credentials
Bash
cp config/db.properties.example config/db.properties

Edit config/db.properties:

db.url=jdbc:mysql://localhost:3306/clinic_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
db.user=clinic_user
db.password=YourPassword

4. Add the JDBC Driver
Download mysql-connector-j-9.x.x.jar from:

https://dev.mysql.com/downloads/connector/j/
Place it in the lib/ folder.

5. Compile
cmd
dir /s /b src\*.java > sources.txt
javac -d bin -cp "lib\mysql-connector-j-9.7.0.jar;." @sources.txt

6. Run
cmd
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" com.clinic.booking.ui.Main


Key Design Decisions
========================
Layered Architecture
-----------------------
Each layer has exactly one responsibility:

 UI layer — display menus, read input
 Service layer — enforce business rules
 DAO layer — execute SQL queries
 Model layer — hold data (POJOs)

DAO Pattern
--------------
Interfaces (PatientDao, DoctorDao, AppointmentDao) define the contract.
JDBC implementations contain the actual SQL.
Switching databases only requires new implementations.

Singleton Pattern
------------------
ConnectionManager uses Singleton to ensure the JDBC driver
is loaded once and db.properties is read once.

PreparedStatement
-------------------
All SQL uses PreparedStatement with ? placeholders.
User input is never concatenated into SQL strings.
This completely prevents SQL injection attacks.

try-with-resources
---------------------
Every Connection, PreparedStatement, and ResultSet is opened
inside try-with-resources blocks.
This guarantees they are closed even if exceptions occur.
Prevents connection leaks.

## Author
Manda Navya Lakshmi
B.Tech CIC 
Vasireddy Venkatadri International Technological University
