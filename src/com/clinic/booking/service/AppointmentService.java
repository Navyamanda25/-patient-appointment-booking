package com.clinic.booking.service;

import com.clinic.booking.dao.AppointmentDao;
import com.clinic.booking.dao.DoctorDao;
import com.clinic.booking.dao.PatientDao;
import com.clinic.booking.model.Appointment;
import com.clinic.booking.model.AppointmentStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * AppointmentService is the BUSINESS RULES layer.
 *
 * ── WHY DO WE NEED A SERVICE LAYER? ─────────────────────────────────────────
 *
 * Imagine Main.java calls the DAO directly to book an appointment.
 * Main.java would need to:
 *   - Check if the time is in the future
 *   - Check if duration is valid
 *   - Check if patient exists
 *   - Check if doctor exists
 *   - Check for overlaps
 *   - Then call appointmentDao.create()
 *
 * That is too much responsibility for the UI layer.
 * If you later add a web UI or a mobile UI, you would have to
 * copy all those checks into each new UI. That is a maintenance nightmare.
 *
 * Solution: Put ALL business rules in AppointmentService.
 * Any UI (console, web, mobile) just calls service.bookAppointment()
 * and all rules are enforced automatically.
 *
 * ── WHAT THIS CLASS DOES ─────────────────────────────────────────────────────
 *
 * bookAppointment() enforces 5 rules in order:
 *   Rule 1 → appointment time must be in the future
 *   Rule 2 → duration must be 15, 30, 45, or 60 minutes
 *   Rule 3 → patient must exist in the database
 *   Rule 4 → doctor must exist in the database
 *   Rule 5 → doctor must not have an overlapping appointment
 *
 * If ALL 5 rules pass → create the appointment in the database
 * If ANY rule fails   → throw an exception with a clear message
 *
 * cancelAppointment() delegates directly to the DAO.
 * No extra business rules needed for cancellation.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class AppointmentService {

    // ── Dependencies ──────────────────────────────────────────────────────
    // AppointmentService needs all 3 DAOs to do its job:
    //   appointmentDao → to check overlaps and create appointments
    //   patientDao     → to verify the patient exists
    //   doctorDao      → to verify the doctor exists
    private final AppointmentDao appointmentDao;
    private final PatientDao     patientDao;
    private final DoctorDao      doctorDao;

    // ── Constructor ───────────────────────────────────────────────────────
    /**
     * Constructor Injection.
     *
     * We receive the DAOs from outside (from Main.java) rather than
     * creating them inside this class.
     *
     * WHY?
     * This is called "Dependency Injection" — a very common pattern.
     * AppointmentService does not care whether it talks to MySQL, PostgreSQL,
     * or a test database. It just uses whatever DAOs it is given.
     * This makes testing easier and the code more flexible.
     *
     * Main.java will call:
     *   new AppointmentService(appointmentDao, patientDao, doctorDao)
     */
    public AppointmentService(AppointmentDao appointmentDao,
                               PatientDao patientDao,
                               DoctorDao doctorDao) {
        this.appointmentDao = appointmentDao;
        this.patientDao     = patientDao;
        this.doctorDao      = doctorDao;
    }

    // ── bookAppointment ───────────────────────────────────────────────────
    /**
     * Books a new appointment after validating ALL business rules.
     *
     * This is the most important method in the entire project.
     * It is the central point where everything comes together.
     *
     * @param patientId  the ID of the patient who wants the appointment
     * @param doctorId   the ID of the doctor the patient wants to see
     * @param when       the proposed start date and time
     * @param duration   the length of the appointment in minutes
     * @param notes      optional notes (can be empty string or null)
     *
     * @return the auto-generated appointment ID if booking succeeded
     *
     * @throws IllegalArgumentException if rules 1, 2, 3, or 4 fail
     *         (bad input — user's fault)
     * @throws IllegalStateException    if rule 5 fails
     *         (conflict — doctor is already booked)
     * @throws SQLException             if a database error occurs
     *         (system error — not user's fault)
     */
    public int bookAppointment(int patientId, int doctorId,
                                LocalDateTime when, int duration,
                                String notes)
            throws SQLException {

        // ── RULE 1: Time must be in the future ───────────────────────────
        /**
         * LocalDateTime.now() gives the current date and time.
         * when.isBefore(now) returns true if the proposed time is in the past.
         *
         * Example:
         *   Current time: 2026-01-15 14:30
         *   User enters:  2026-01-15 10:00  → isBefore = true → BLOCK
         *   User enters:  2026-01-15 15:00  → isBefore = false → ALLOW
         */
        if (when.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "Cannot book an appointment in the past.\n" +
                "  You entered : " + when.toString().replace("T", " ") + "\n" +
                "  Current time: " + LocalDateTime.now()
                                                  .toString()
                                                  .substring(0, 16)
                                                  .replace("T", " ") + "\n" +
                "  Please choose a future date and time."
            );
        }

        // ── RULE 2: Duration must be a valid slot size ───────────────────
        /**
         * We only allow 4 specific durations: 15, 30, 45, 60.
         * This matches the CHECK constraint in the database:
         *   CHECK (duration_minutes IN (15, 30, 45, 60))
         *
         * We validate in Java BEFORE hitting the database.
         * This gives the user a better error message than a raw SQL error.
         */
        if (duration != 15 && duration != 30
                && duration != 45 && duration != 60) {
            throw new IllegalArgumentException(
                "Invalid appointment duration: " + duration + " minutes.\n" +
                "  Allowed durations: 15, 30, 45, or 60 minutes."
            );
        }

        // ── RULE 3: Patient must exist ───────────────────────────────────
        /**
         * We call patientDao.findById() which returns Optional<Patient>.
         * If Optional is empty → no patient with that ID exists → block booking.
         *
         * isEmpty() returns true when Optional has no value.
         * We cannot book an appointment for a patient who is not registered.
         */
        if (patientDao.findById(patientId).isEmpty()) {
            throw new IllegalArgumentException(
                "Patient not found.\n" +
                "  No patient exists with ID: " + patientId + "\n" +
                "  Please register the patient first (Menu option 1)."
            );
        }

        // ── RULE 4: Doctor must exist ────────────────────────────────────
        /**
         * Same logic as Rule 3 but for doctors.
         * We cannot book with a doctor who is not in the system.
         */
        if (doctorDao.findById(doctorId).isEmpty()) {
            throw new IllegalArgumentException(
                "Doctor not found.\n" +
                "  No doctor exists with ID: " + doctorId + "\n" +
                "  Please add the doctor first (Menu option 2)."
            );
        }

        // ── RULE 5: No overlapping appointment for this doctor ───────────
        /**
         * This is the DOUBLE-BOOKING CHECK.
         *
         * We call appointmentDao.hasOverlap() which runs the time-range
         * SQL query we wrote in JdbcAppointmentDao.
         *
         * If hasOverlap returns true → doctor is already busy → block
         * If hasOverlap returns false → doctor is free → allow
         *
         * We use IllegalStateException (not IllegalArgumentException) here
         * because the input itself is valid — it is just that the slot
         * is already taken. This is a STATE conflict, not a bad input.
         */
        if (appointmentDao.hasOverlap(doctorId, when, duration)) {
            throw new IllegalStateException(
                "Booking conflict! Doctor ID " + doctorId +
                " already has an appointment that overlaps with:\n" +
                "  Requested time    : " +
                when.toString().replace("T", " ") + "\n" +
                "  Requested duration: " + duration + " minutes\n" +
                "  Please choose a different time slot."
            );
        }

        // ── ALL RULES PASSED: Create the appointment ─────────────────────
        /**
         * All 5 rules passed. We are safe to create the appointment.
         *
         * We build an Appointment object with:
         *   id = 0          → MySQL will generate the real ID
         *   status = SCHEDULED → new appointments always start as SCHEDULED
         *
         * Then we pass it to the DAO which runs the INSERT SQL.
         * The DAO returns the auto-generated ID from MySQL.
         * We return that ID to Main.java so it can show it to the user.
         */
        Appointment newAppointment = new Appointment(
            0,                            // id — MySQL generates this
            patientId,
            doctorId,
            when,
            duration,
            AppointmentStatus.SCHEDULED,  // always SCHEDULED when first created
            notes
        );

        int generatedId = appointmentDao.create(newAppointment);

        System.out.println("\n  [Service] Appointment created successfully in database.");

        return generatedId;
    }

    // ── cancelAppointment ─────────────────────────────────────────────────
    /**
     * Cancels a SCHEDULED appointment.
     *
     * We delegate directly to the DAO.
     * The DAO's SQL only cancels if status = 'SCHEDULED',
     * so there is no risk of cancelling a COMPLETED appointment.
     *
     * @param appointmentId the ID of the appointment to cancel
     * @return true  if the appointment was found and cancelled
     *         false if not found or already cancelled/completed
     * @throws SQLException if a database error occurs
     */
    public boolean cancelAppointment(int appointmentId) throws SQLException {
        return appointmentDao.cancel(appointmentId);
    }
}