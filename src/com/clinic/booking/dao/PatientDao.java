package com.clinic.booking.dao;

import com.clinic.booking.model.Patient;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * PatientDao defines the contract for all Patient database operations.
 *
 * This is an INTERFACE — it only declares method signatures.
 * It says WHAT operations are possible, not HOW they work.
 *
 * The actual SQL is written in JdbcPatientDao which implements this interface.
 *
 * WHY Optional<Patient> instead of Patient?
 * If we return Patient directly and no patient is found, we would return null.
 * Null causes NullPointerException if the caller forgets to check.
 * Optional forces the caller to handle the "not found" case explicitly.
 * Optional.of(patient) → found
 * Optional.empty()     → not found
 */
public interface PatientDao {

    /**
     * Inserts a new patient into the patients table.
     *
     * @param patient the patient to insert
     *                (pass id=0, MySQL will generate the real id)
     * @return the auto-generated id assigned by MySQL
     * @throws SQLException if a database error occurs
     */
    int add(Patient patient) throws SQLException;

    /**
     * Finds a patient by their primary key id.
     *
     * @param id the patient id to search for
     * @return Optional containing the patient if found, empty if not found
     * @throws SQLException if a database error occurs
     */
    Optional<Patient> findById(int id) throws SQLException;

    /**
     * Returns every patient in the database ordered by id.
     *
     * @return list of all patients, empty list if none exist
     * @throws SQLException if a database error occurs
     */
    List<Patient> findAll() throws SQLException;
}