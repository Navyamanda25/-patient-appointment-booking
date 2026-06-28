package com.clinic.booking.model;

import java.time.LocalDateTime;

/**
 * Appointment represents one row in the appointments table.
 *
 * This is a POJO — Plain Old Java Object.
 * It only holds data. No SQL. No business rules.
 *
 * Every field matches a column in the appointments table:
 *   id               → INT PRIMARY KEY AUTO_INCREMENT
 *   patientId        → INT (foreign key → patients.id)
 *   doctorId         → INT (foreign key → doctors.id)
 *   appointmentTime  → DATETIME
 *   durationMinutes  → INT (15, 30, 45, or 60)
 *   status           → ENUM (SCHEDULED / COMPLETED / CANCELLED)
 *   notes            → VARCHAR(500)
 *
 * Notice:
 *   We use LocalDateTime for the appointment time (not old java.util.Date).
 *   We use AppointmentStatus enum for status (not a plain String).
 *   Both of these make the code safer and easier to work with.
 */
public class Appointment {

    // ── Fields ────────────────────────────────────────────────────────────
    private int               id;
    private int               patientId;
    private int               doctorId;
    private LocalDateTime     appointmentTime;
    private int               durationMinutes;
    private AppointmentStatus status;
    private String            notes;

    // ── Constructors ──────────────────────────────────────────────────────

    /**
     * No-argument constructor.
     * Required by the DAO layer when mapping ResultSet rows to Appointment objects.
     */
    public Appointment() {}

    /**
     * Full constructor.
     * Used in AppointmentService when creating a new appointment to save.
     * We pass id=0 because MySQL will generate the real ID.
     */
    public Appointment(int id, int patientId, int doctorId,
                       LocalDateTime appointmentTime, int durationMinutes,
                       AppointmentStatus status, String notes) {
        this.id              = id;
        this.patientId       = patientId;
        this.doctorId        = doctorId;
        this.appointmentTime = appointmentTime;
        this.durationMinutes = durationMinutes;
        this.status          = status;
        this.notes           = notes;
    }

    // ── Getters and Setters ───────────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getPatientId()                       { return patientId; }
    public void setPatientId(int patientId)         { this.patientId = patientId; }

    public int getDoctorId()                        { return doctorId; }
    public void setDoctorId(int doctorId)           { this.doctorId = doctorId; }

    public LocalDateTime getAppointmentTime()               { return appointmentTime; }
    public void setAppointmentTime(LocalDateTime t)         { this.appointmentTime = t; }

    public int getDurationMinutes()                         { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes)     { this.durationMinutes = durationMinutes; }

    public AppointmentStatus getStatus()                    { return status; }
    public void setStatus(AppointmentStatus status)         { this.status = status; }

    public String getNotes()                        { return notes; }
    public void setNotes(String notes)              { this.notes = notes; }

    // ── toString ──────────────────────────────────────────────────────────
    /**
     * Called automatically when you do System.out.println(appointment).
     * Prints all appointment details in a clean readable format.
     *
     * We replace the "T" in LocalDateTime output with a space.
     * Example: "2026-08-01T10:00" becomes "2026-08-01 10:00"
     */
    @Override
    public String toString() {
        return String.format(
            "Appointment { id=%-4d | patientId=%-4d | doctorId=%-4d | time=%-18s | %d min | status=%-10s | notes='%s' }",
            id,
            patientId,
            doctorId,
            appointmentTime.toString().replace("T", " "),
            durationMinutes,
            status,
            (notes != null ? notes : "")
        );
    }
}