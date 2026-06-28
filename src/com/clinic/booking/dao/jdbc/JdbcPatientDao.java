package com.clinic.booking.dao.jdbc;

import com.clinic.booking.dao.PatientDao;
import com.clinic.booking.model.Patient;
import com.clinic.booking.service.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JdbcPatientDao implements PatientDao using JDBC + MySQL.
 *
 * KEY JDBC CONCEPTS USED HERE:
 *
 * Connection
 *   Represents the live link between Java and MySQL.
 *   Think of it as a phone call to the database.
 *   You open it, use it, then close it.
 *
 * PreparedStatement
 *   A pre-compiled SQL query with ? placeholders.
 *   You set each ? to a value before running the query.
 *   WHY not regular Statement?
 *   If you use Statement and build SQL by joining strings,
 *   a user could type: name = "'; DROP TABLE patients; --"
 *   and destroy your database. This is called SQL Injection.
 *   PreparedStatement treats all input as DATA, never as SQL.
 *   It is always safe.
 *
 * ResultSet
 *   The table of rows returned by a SELECT query.
 *   You loop through it with rs.next() to read each row.
 *
 * try-with-resources
 *   Syntax: try (Connection conn = ...; PreparedStatement ps = ...) { }
 *   Java automatically calls conn.close() and ps.close() when done.
 *   Even if an exception occurs inside, everything closes properly.
 *   Without this, a crash would leave the database connection open forever
 *   (called a connection leak).
 */
public class JdbcPatientDao implements PatientDao {

    /**
     * Inserts a new patient into the patients table.
     *
     * SQL used:
     *   INSERT INTO patients (name, phone, email, dob, gender)
     *   VALUES (?, ?, ?, ?, ?)
     *
     * We do NOT include id in the INSERT because MySQL generates it
     * automatically (AUTO_INCREMENT).
     *
     * Statement.RETURN_GENERATED_KEYS tells JDBC to give us back
     * the id that MySQL assigned, so we can show it to the user.
     */
    @Override
    public int add(Patient patient) throws SQLException {

        String sql = "INSERT INTO patients (name, phone, email, dob, gender) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            // Set each ? in order
            ps.setString(1, patient.getName());
            ps.setString(2, patient.getPhone());
            ps.setString(3, patient.getEmail());

            // dob is optional — the user can skip it
            // If null, we tell JDBC to insert NULL for that column
            if (patient.getDob() != null) {
                // LocalDate → java.sql.Date (what JDBC needs for DATE columns)
                ps.setDate(4, Date.valueOf(patient.getDob()));
            } else {
                ps.setNull(4, Types.DATE);
            }

            ps.setString(5, patient.getGender());

            // Run the INSERT
            ps.executeUpdate();

            // Read back the auto-generated id
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return -1; // should never reach here if INSERT succeeded
    }

    /**
     * Finds one patient by their id.
     *
     * SQL used:
     *   SELECT id, name, phone, email, dob, gender
     *   FROM patients
     *   WHERE id = ?
     *
     * Returns Optional.of(patient) if found.
     * Returns Optional.empty() if no row matched.
     */
    @Override
    public Optional<Patient> findById(int id) throws SQLException {

        String sql = "SELECT id, name, phone, email, dob, gender " +
                     "FROM patients " +
                     "WHERE id = ?";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Row found — convert it to a Patient object
                    return Optional.of(mapRowToPatient(rs));
                }
            }
        }
        // No row found
        return Optional.empty();
    }

    /**
     * Returns all patients ordered by id.
     *
     * SQL used:
     *   SELECT id, name, phone, email, dob, gender
     *   FROM patients
     *   ORDER BY id
     *
     * We loop through every row in the ResultSet and
     * convert each one to a Patient object.
     */
    @Override
    public List<Patient> findAll() throws SQLException {

        String sql = "SELECT id, name, phone, email, dob, gender " +
                     "FROM patients " +
                     "ORDER BY id";

        List<Patient> patients = new ArrayList<>();

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                patients.add(mapRowToPatient(rs));
            }
        }
        return patients;
    }

    /**
     * Converts one row from ResultSet into a Patient object.
     *
     * This private helper method is used by both findById and findAll.
     * Instead of copying the same mapping code twice, we put it here once.
     * This follows the DRY principle — Don't Repeat Yourself.
     *
     * rs.getInt("id")     reads the id column as an int
     * rs.getString("name") reads the name column as a String
     * rs.getDate("dob")   reads the dob column as java.sql.Date
     *                     then we convert it to LocalDate with .toLocalDate()
     */
    private Patient mapRowToPatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getInt("id"));
        patient.setName(rs.getString("name"));
        patient.setPhone(rs.getString("phone"));
        patient.setEmail(rs.getString("email"));

        // dob can be NULL in the database
        Date dob = rs.getDate("dob");
        patient.setDob(dob != null ? dob.toLocalDate() : null);

        patient.setGender(rs.getString("gender"));
        return patient;
    }
}