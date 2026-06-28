package com.clinic.booking.dao.jdbc;

import com.clinic.booking.dao.DoctorDao;
import com.clinic.booking.model.Doctor;
import com.clinic.booking.service.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JdbcDoctorDao implements DoctorDao using JDBC + MySQL.
 *
 * Same patterns as JdbcPatientDao:
 *   - PreparedStatement for all SQL (SQL injection safe)
 *   - try-with-resources for all connections (no leaks)
 *   - private mapRowToDoctor() helper to avoid code duplication
 */
public class JdbcDoctorDao implements DoctorDao {

    /**
     * Inserts a new doctor into the doctors table.
     *
     * SQL used:
     *   INSERT INTO doctors (name, specialization, phone, email)
     *   VALUES (?, ?, ?, ?)
     */
    @Override
    public int add(Doctor doctor) throws SQLException {

        String sql = "INSERT INTO doctors (name, specialization, phone, email) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, doctor.getName());
            ps.setString(2, doctor.getSpecialization());
            ps.setString(3, doctor.getPhone());
            ps.setString(4, doctor.getEmail());

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
     * Finds one doctor by their id.
     *
     * SQL used:
     *   SELECT id, name, specialization, phone, email
     *   FROM doctors
     *   WHERE id = ?
     */
    @Override
    public Optional<Doctor> findById(int id) throws SQLException {

        String sql = "SELECT id, name, specialization, phone, email " +
                     "FROM doctors " +
                     "WHERE id = ?";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToDoctor(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all doctors ordered by id.
     *
     * SQL used:
     *   SELECT id, name, specialization, phone, email
     *   FROM doctors
     *   ORDER BY id
     */
    @Override
    public List<Doctor> findAll() throws SQLException {

        String sql = "SELECT id, name, specialization, phone, email " +
                     "FROM doctors " +
                     "ORDER BY id";

        List<Doctor> doctors = new ArrayList<>();

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                doctors.add(mapRowToDoctor(rs));
            }
        }
        return doctors;
    }

    /**
     * Converts one ResultSet row into a Doctor object.
     * Used by both findById() and findAll() to avoid code duplication.
     */
    private Doctor mapRowToDoctor(ResultSet rs) throws SQLException {
        return new Doctor(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("specialization"),
            rs.getString("phone"),
            rs.getString("email")
        );
    }
}