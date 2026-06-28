package com.clinic.booking.model;

/**
 * AppointmentStatus is an ENUM.
 *
 * What is an enum?
 * An enum is a special type that holds a fixed set of constants.
 * Instead of using plain strings like "SCHEDULED" everywhere
 * (which can have typos like "Scheduled" or "scheduled"),
 * we use this enum so Java enforces the correct value at compile time.
 *
 * Our appointment can only ever be in ONE of these 3 states:
 *   SCHEDULED  → the appointment is coming up
 *   COMPLETED  → the appointment happened
 *   CANCELLED  → the appointment was cancelled
 */
public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}