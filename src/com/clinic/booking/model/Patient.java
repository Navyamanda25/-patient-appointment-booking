package com.clinic.booking.model;

import java.time.LocalDate;

/**
 * Patient represents one row in the patients table.
 *
 * This is a POJO — Plain Old Java Object.
 * It only holds data. No SQL. No business rules.
 *
 * Every field matches a column in the patients table:
 *   id        → INT PRIMARY KEY AUTO_INCREMENT
 *   name      → VARCHAR(100)
 *   phone     → VARCHAR(20)
 *   email     → VARCHAR(120)
 *   dob       → DATE
 *   gender    → VARCHAR(10)
 */
public class Patient {

    // ── Fields ────────────────────────────────────────────────────────────
    private int       id;
    private String    name;
    private String    phone;
    private String    email;
    private LocalDate dob;     // Date of Birth — uses Java's LocalDate, not old java.util.Date
    private String    gender;

    // ── Constructors ──────────────────────────────────────────────────────

    /**
     * No-argument constructor.
     * Required by the DAO layer when it reads rows from the database.
     * The DAO creates an empty Patient first, then fills each field one by one.
     */
    public Patient() {}

    /**
     * Full constructor.
     * Used in Main.java when we have all the data ready from user input.
     * We pass id=0 when creating a new patient because MySQL will generate the real ID.
     */
    public Patient(int id, String name, String phone, String email,
                   LocalDate dob, String gender) {
        this.id     = id;
        this.name   = name;
        this.phone  = phone;
        this.email  = email;
        this.dob    = dob;
        this.gender = gender;
    }

    // ── Getters and Setters ───────────────────────────────────────────────
    // Getters let other classes READ the value of a private field.
    // Setters let other classes CHANGE the value of a private field.
    // We make fields private so nothing can change them accidentally.

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    public String getName()             { return name; }
    public void setName(String name)    { this.name = name; }

    public String getPhone()            { return phone; }
    public void setPhone(String phone)  { this.phone = phone; }

    public String getEmail()            { return email; }
    public void setEmail(String email)  { this.email = email; }

    public LocalDate getDob()           { return dob; }
    public void setDob(LocalDate dob)   { this.dob = dob; }

    public String getGender()               { return gender; }
    public void setGender(String gender)    { this.gender = gender; }

    // ── toString ──────────────────────────────────────────────────────────
    /**
     * toString is called automatically when you do System.out.println(patient).
     * Without this, Java would print something useless like "Patient@7d4991ad".
     * With this, it prints all the patient's data in a readable format.
     */
    @Override
    public String toString() {
        return String.format(
            "Patient { id=%-4d | name=%-20s | phone=%-15s | email=%-30s | dob=%-12s | gender=%s }",
            id,
            name,
            phone,
            email,
            (dob != null ? dob.toString() : "N/A"),
            gender
        );
    }
}