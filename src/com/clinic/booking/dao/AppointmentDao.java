package com.clinic.booking.dao;

import com.clinic.booking.model.Appointment;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AppointmentDao defines the contract for all Appointment database operations.
 *
 * This is an INTERFACE — SQL lives in JdbcAppointmentDao, not here.
 *
 * The most important method here is hasOverlap() —
 * it is what prevents double booking.
 */
public interface AppointmentDao {

    /**
     * Inserts a new appointment into the appointments table.
     *
     * @param appointment the appointment to insert
     *                    (pass id=0, MySQL will generate the real id)
     * @return the auto-generated id assigned by MySQL
     * @throws SQLException if a database error occurs
     */
    int create(Appointment appointment) throws SQLException;

    /**
     * Checks if a doctor already has a SCHEDULED appointment that
     * overlaps with the proposed new time slot.
     *
     * This is the CORE of double-booking prevention.
     *
     * Two appointments overlap when BOTH of these are true:
     *   new start time  <  existing end time
     *   new end time    >  existing start time
     *
     * Existing end time = existing start time + existing duration
     * New end time      = new start time + new duration
     *
     * @param doctorId     the doctor to check
     * @param start        proposed start time for new appointment
     * @param durationMins proposed duration in minutes for new appointment
     * @return true  if overlap EXISTS   → block the booking
     *         false if no overlap found → allow the booking
     * @throws SQLException if a database error occurs
     */
    boolean hasOverlap(int doctorId, LocalDateTime start,
                       int durationMins) throws SQLException;

    /**
     * Returns all appointments for a specific doctor on a specific date.
     * Ordered by appointment time ascending (earliest first).
     *
     * @param doctorId the doctor whose schedule to view
     * @param date     the calendar date to check
     * @return list of appointments, empty list if none found
     * @throws SQLException if a database error occurs
     */
    List<Appointment> findByDoctorAndDate(int doctorId,
                                          LocalDate date) throws SQLException;

    /**
     * Returns all appointments for a specific patient.
     * Ordered by appointment time descending (newest first).
     *
     * @param patientId the patient whose appointments to view
     * @return list of appointments, empty list if none found
     * @throws SQLException if a database error occurs
     */
    List<Appointment> findByPatient(int patientId) throws SQLException;

    /**
     * Cancels a SCHEDULED appointment by setting its status to CANCELLED.
     * Does nothing if the appointment is already CANCELLED or COMPLETED.
     *
     * @param appointmentId the id of the appointment to cancel
     * @return true  if the appointment was found and cancelled
     *         false if not found or already cancelled/completed
     * @throws SQLException if a database error occurs
     */
    boolean cancel(int appointmentId) throws SQLException;
}