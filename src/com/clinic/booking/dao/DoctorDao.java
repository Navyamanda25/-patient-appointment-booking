package com.clinic.booking.dao;

import com.clinic.booking.model.Doctor;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DoctorDao defines the contract for all Doctor database operations.
 *
 * This is an INTERFACE — SQL lives in JdbcDoctorDao, not here.
 */
public interface DoctorDao {

    /**
     * Inserts a new doctor into the doctors table.
     *
     * @param doctor the doctor to insert
     *               (pass id=0, MySQL will generate the real id)
     * @return the auto-generated id assigned by MySQL
     * @throws SQLException if a database error occurs
     */
    int add(Doctor doctor) throws SQLException;

    /**
     * Finds a doctor by their primary key id.
     *
     * @param id the doctor id to search for
     * @return Optional containing the doctor if found, empty if not found
     * @throws SQLException if a database error occurs
     */
    Optional<Doctor> findById(int id) throws SQLException;

    /**
     * Returns every doctor in the database ordered by id.
     *
     * @return list of all doctors, empty list if none exist
     * @throws SQLException if a database error occurs
     */
    List<Doctor> findAll() throws SQLException;
}