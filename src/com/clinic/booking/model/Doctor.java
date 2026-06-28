package com.clinic.booking.model;

/**
 * Doctor represents one row in the doctors table.
 *
 * This is a POJO — Plain Old Java Object.
 * It only holds data. No SQL. No business rules.
 *
 * Every field matches a column in the doctors table:
 *   id             → INT PRIMARY KEY AUTO_INCREMENT
 *   name           → VARCHAR(100)
 *   specialization → VARCHAR(100)
 *   phone          → VARCHAR(20)
 *   email          → VARCHAR(120)
 */
public class Doctor {

    // ── Fields ────────────────────────────────────────────────────────────
    private int    id;
    private String name;
    private String specialization;
    private String phone;
    private String email;

    // ── Constructors ──────────────────────────────────────────────────────

    /**
     * No-argument constructor.
     * Required by the DAO layer when mapping ResultSet rows to Doctor objects.
     */
    public Doctor() {}

    /**
     * Full constructor.
     * Used in Main.java when we have all the data ready from user input.
     * We pass id=0 when adding a new doctor because MySQL generates the real ID.
     */
    public Doctor(int id, String name, String specialization,
                  String phone, String email) {
        this.id             = id;
        this.name           = name;
        this.specialization = specialization;
        this.phone          = phone;
        this.email          = email;
    }

    // ── Getters and Setters ───────────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getSpecialization()               { return specialization; }
    public void setSpecialization(String spec)      { this.specialization = spec; }

    public String getPhone()                    { return phone; }
    public void setPhone(String phone)          { this.phone = phone; }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }

    // ── toString ──────────────────────────────────────────────────────────
    /**
     * Called automatically when you do System.out.println(doctor).
     * Prints all the doctor's data in a clean readable format.
     */
    @Override
    public String toString() {
        return String.format(
            "Doctor { id=%-4d | name=%-20s | specialization=%-20s | phone=%-15s | email=%s }",
            id,
            name,
            specialization,
            phone,
            email
        );
    }
}