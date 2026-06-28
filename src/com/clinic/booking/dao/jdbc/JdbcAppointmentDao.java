package com.clinic.booking.dao.jdbc;

import com.clinic.booking.dao.AppointmentDao;
import com.clinic.booking.model.Appointment;
import com.clinic.booking.model.AppointmentStatus;
import com.clinic.booking.service.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JdbcAppointmentDao implements AppointmentDao using JDBC + MySQL.
 *
 * The most critical method is hasOverlap() — this is what prevents
 * a doctor from being double-booked.
 *
 * Key type conversions used:
 *   LocalDateTime → Timestamp   (for DATETIME columns in MySQL)
 *   Timestamp     → LocalDateTime (when reading back from MySQL)
 *   enum          → String       (status stored as VARCHAR in DB)
 *   String        → enum         (when reading status back from DB)
 */
public class JdbcAppointmentDao implements AppointmentDao {

    /**
     * Inserts a new appointment into the appointments table.
     *
     * SQL used:
     *   INSERT INTO appointments
     *     (patient_id, doctor_id, appointment_time, duration_minutes, status, notes)
     *   VALUES (?, ?, ?, ?, ?, ?)
     *
     * Notice we do NOT insert id — MySQL generates it automatically.
     * Notice we do NOT insert created_at — MySQL fills it automatically.
     */
    @Override
    public int create(Appointment appointment) throws SQLException {

        String sql =
            "INSERT INTO appointments " +
            "(patient_id, doctor_id, appointment_time, duration_minutes, status, notes) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, appointment.getPatientId());
            ps.setInt(2, appointment.getDoctorId());

            // LocalDateTime → java.sql.Timestamp
            // MySQL DATETIME columns need Timestamp, not LocalDateTime directly
            ps.setTimestamp(3, Timestamp.valueOf(appointment.getAppointmentTime()));

            ps.setInt(4, appointment.getDurationMinutes());

            // enum → String   e.g. AppointmentStatus.SCHEDULED → "SCHEDULED"
            ps.setString(5, appointment.getStatus().name());

            ps.setString(6, appointment.getNotes());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Checks if a doctor has any SCHEDULED appointment that overlaps
     * with the proposed new time slot.
     *
     * ── HOW OVERLAP DETECTION WORKS ─────────────────────────────────────
     *
     * Let us say a doctor already has this appointment:
     *   Existing: starts at 10:00, duration 30 min → runs from 10:00 to 10:30
     *
     * Now someone wants to book:
     *   New: starts at 10:15, duration 30 min → would run from 10:15 to 10:45
     *
     * Do they overlap? YES — the doctor is busy from 10:00 to 10:30,
     * and the new appointment tries to start at 10:15 (inside that window).
     *
     * The mathematical rule for overlap:
     *   Two time ranges [A_start, A_end] and [B_start, B_end] overlap when:
     *   A_start < B_end   AND   A_end > B_start
     *
     * In our case:
     *   A = existing appointment
     *   B = new proposed appointment
     *
     *   existing_start < new_end          → existing starts before new ends
     *   existing_end   > new_start        → existing ends after new starts
     *
     * Rewritten from new appointment's perspective:
     *   new_start < existing_end          → ? < DATE_ADD(appointment_time, INTERVAL duration MINUTE)
     *   new_end   > existing_start        → DATE_ADD(?, INTERVAL ? MINUTE) > appointment_time
     *
     * ── THE SQL QUERY ────────────────────────────────────────────────────
     *
     *   SELECT COUNT(*)
     *   FROM appointments
     *   WHERE doctor_id = ?
     *     AND status = 'SCHEDULED'
     *     AND ? < DATE_ADD(appointment_time, INTERVAL duration_minutes MINUTE)
     *     AND DATE_ADD(?, INTERVAL ? MINUTE) > appointment_time
     *
     * Parameters:
     *   ?1 = doctorId
     *   ?2 = new start time (N)
     *   ?3 = new start time (N) again — for DATE_ADD
     *   ?4 = new duration in minutes
     *
     * If COUNT(*) > 0 → overlap found → block the booking
     * If COUNT(*) = 0 → no overlap    → allow the booking
     *
     * We only check SCHEDULED appointments.
     * CANCELLED and COMPLETED ones do not block new bookings.
     * ─────────────────────────────────────────────────────────────────────
     */
    @Override
    public boolean hasOverlap(int doctorId, LocalDateTime start,
                               int durationMins) throws SQLException {

        String sql =
            "SELECT COUNT(*) " +
            "FROM appointments " +
            "WHERE doctor_id = ? " +
            "  AND status = 'SCHEDULED' " +
            "  AND ? < DATE_ADD(appointment_time, INTERVAL duration_minutes MINUTE) " +
            "  AND DATE_ADD(?, INTERVAL ? MINUTE) > appointment_time";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            ps.setTimestamp(2, Timestamp.valueOf(start));  // new start time
            ps.setTimestamp(3, Timestamp.valueOf(start));  // new start time (for DATE_ADD)
            ps.setInt(4, durationMins);                    // new duration

            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); // COUNT(*) always returns exactly one row
                int count = rs.getInt(1);
                return count > 0; // true = overlap exists = block booking
            }
        }
    }

    /**
     * Returns all appointments for a doctor on a specific date.
     *
     * SQL used:
     *   SELECT id, patient_id, doctor_id, appointment_time,
     *          duration_minutes, status, notes
     *   FROM appointments
     *   WHERE doctor_id = ?
     *     AND appointment_time BETWEEN ? AND ?
     *   ORDER BY appointment_time
     *
     * BETWEEN start and end:
     *   start = the date at 00:00:00 (midnight, start of day)
     *   end   = the date at 23:59:59 (end of day)
     *
     * Example for 2026-08-01:
     *   start = 2026-08-01 00:00:00
     *   end   = 2026-08-01 23:59:59
     *
     * This captures ALL appointments on that date regardless of time.
     */
    @Override
    public List<Appointment> findByDoctorAndDate(int doctorId,
                                                  LocalDate date) throws SQLException {

        String sql =
            "SELECT id, patient_id, doctor_id, appointment_time, " +
            "       duration_minutes, status, notes " +
            "FROM appointments " +
            "WHERE doctor_id = ? " +
            "  AND appointment_time BETWEEN ? AND ? " +
            "ORDER BY appointment_time";

        List<Appointment> results = new ArrayList<>();

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, doctorId);

            // Start of day: 2026-08-01 00:00:00
            ps.setTimestamp(2, Timestamp.valueOf(date.atStartOfDay()));

            // End of day: 2026-08-01 23:59:59
            ps.setTimestamp(3, Timestamp.valueOf(date.atTime(23, 59, 59)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToAppointment(rs));
                }
            }
        }
        return results;
    }

    /**
     * Returns all appointments for a patient, newest first.
     *
     * SQL used:
     *   SELECT id, patient_id, doctor_id, appointment_time,
     *          duration_minutes, status, notes
     *   FROM appointments
     *   WHERE patient_id = ?
     *   ORDER BY appointment_time DESC
     *
     * ORDER BY appointment_time DESC means:
     * The most recent appointment appears first in the list.
     */
    @Override
    public List<Appointment> findByPatient(int patientId) throws SQLException {

        String sql =
            "SELECT id, patient_id, doctor_id, appointment_time, " +
            "       duration_minutes, status, notes " +
            "FROM appointments " +
            "WHERE patient_id = ? " +
            "ORDER BY appointment_time DESC";

        List<Appointment> results = new ArrayList<>();

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToAppointment(rs));
                }
            }
        }
        return results;
    }

    /**
     * Cancels a SCHEDULED appointment.
     *
     * SQL used:
     *   UPDATE appointments
     *   SET status = 'CANCELLED'
     *   WHERE id = ?
     *     AND status = 'SCHEDULED'
     *
     * WHY does the WHERE clause include status = 'SCHEDULED'?
     *
     * Safety reason:
     * If someone tries to cancel an appointment that is already CANCELLED
     * or COMPLETED, this query simply updates 0 rows.
     * executeUpdate() returns 0, so we return false to the caller.
     * We never accidentally change a COMPLETED appointment to CANCELLED.
     *
     * @return true  if 1 row was updated (appointment found and was SCHEDULED)
     *         false if 0 rows updated (not found or wrong status)
     */
    @Override
    public boolean cancel(int appointmentId) throws SQLException {

        String sql =
            "UPDATE appointments " +
            "SET status = 'CANCELLED' " +
            "WHERE id = ? " +
            "  AND status = 'SCHEDULED'";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);

            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;
        }
    }

    /**
     * Converts one ResultSet row into an Appointment object.
     *
     * Type conversions:
     *   rs.getTimestamp("appointment_time").toLocalDateTime()
     *     → Timestamp (from MySQL) → LocalDateTime (Java)
     *
     *   AppointmentStatus.valueOf(rs.getString("status"))
     *     → "SCHEDULED" (String from MySQL) → AppointmentStatus.SCHEDULED (enum)
     *
     * This private helper is used by findByDoctorAndDate() and findByPatient()
     * to avoid writing the same mapping code twice.
     */
    private Appointment mapRowToAppointment(ResultSet rs) throws SQLException {
        return new Appointment(
            rs.getInt("id"),
            rs.getInt("patient_id"),
            rs.getInt("doctor_id"),
            rs.getTimestamp("appointment_time").toLocalDateTime(),
            rs.getInt("duration_minutes"),
            AppointmentStatus.valueOf(rs.getString("status")),
            rs.getString("notes")
        );
    }
}