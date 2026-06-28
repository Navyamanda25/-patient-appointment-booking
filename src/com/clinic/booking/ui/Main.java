package com.clinic.booking.ui;

import com.clinic.booking.dao.AppointmentDao;
import com.clinic.booking.dao.DoctorDao;
import com.clinic.booking.dao.PatientDao;
import com.clinic.booking.dao.jdbc.JdbcAppointmentDao;
import com.clinic.booking.dao.jdbc.JdbcDoctorDao;
import com.clinic.booking.dao.jdbc.JdbcPatientDao;
import com.clinic.booking.model.Appointment;
import com.clinic.booking.model.Doctor;
import com.clinic.booking.model.Patient;
import com.clinic.booking.service.AppointmentService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);

    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {

        PatientDao     patientDao     = new JdbcPatientDao();
        DoctorDao      doctorDao      = new JdbcDoctorDao();
        AppointmentDao appointmentDao = new JdbcAppointmentDao();

        AppointmentService service = new AppointmentService(
            appointmentDao, patientDao, doctorDao
        );

        printWelcomeBanner();

        boolean running = true;

        while (running) {

            printMainMenu();

            int choice = readInt("  Enter your choice: ");
            System.out.println();

            try {
                switch (choice) {
                    case 1 -> registerPatient(patientDao);
                    case 2 -> addDoctor(doctorDao);
                    case 3 -> bookAppointment(service);
                    case 4 -> listAllPatients(patientDao);
                    case 5 -> listAllDoctors(doctorDao);
                    case 6 -> viewDoctorSchedule(appointmentDao);
                    case 7 -> viewPatientAppointments(appointmentDao);
                    case 8 -> cancelAppointment(service);
                    case 0 -> {
                        System.out.println("  Goodbye!");
                        running = false;
                    }
                    default -> System.out.println(
                        "  [!] Invalid choice. Enter 0 to 8."
                    );
                }

            } catch (IllegalArgumentException e) {
                System.out.println("\n  [VALIDATION ERROR] " + e.getMessage());

            } catch (IllegalStateException e) {
                System.out.println("\n  [BOOKING CONFLICT] " + e.getMessage());

            } catch (DateTimeParseException e) {
                System.out.println(
                    "\n  [FORMAT ERROR] Wrong date or time format." +
                    "\n  For date and time : yyyy-MM-dd HH:mm  " +
                    "\n  Example           : 2026-08-01 10:00  " +
                    "\n  For date only     : yyyy-MM-dd        " +
                    "\n  Example           : 2026-08-01        "
                );

            } catch (SQLException e) {
                System.out.println(
                    "\n  [DATABASE ERROR] " + e.getMessage() +
                    "\n  SQL State  : " + e.getSQLState() +
                    "\n  Error Code : " + e.getErrorCode()
                );
            }
        }

        sc.close();
    }

    // --------------------------------------------------------
    // DISPLAY METHODS
    // --------------------------------------------------------

    private static void printWelcomeBanner() {
        System.out.println();
        System.out.println("  ==========================================");
        System.out.println("    PATIENT APPOINTMENT BOOKING SYSTEM      ");
        System.out.println("    Core Java + JDBC + MySQL                ");
        System.out.println("  ==========================================");
        System.out.println();
        System.out.println("  Connecting to database...");
        System.out.println();
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("  ==========================================");
        System.out.println("                 MAIN MENU                 ");
        System.out.println("  ==========================================");
        System.out.println("   1. Register New Patient");
        System.out.println("   2. Add New Doctor");
        System.out.println("   3. Book Appointment");
        System.out.println("   4. View All Patients");
        System.out.println("   5. View All Doctors");
        System.out.println("   6. View Doctor Schedule (by date)");
        System.out.println("   7. View Patient Appointments");
        System.out.println("   8. Cancel Appointment");
        System.out.println("   0. Exit");
        System.out.println("  ------------------------------------------");
    }

    // --------------------------------------------------------
    // MENU ACTION METHODS
    // --------------------------------------------------------

    private static void registerPatient(PatientDao dao) throws SQLException {

        System.out.println("  -- Register New Patient --");
        System.out.println();

        System.out.print("  Full Name              : ");
        String name = sc.nextLine().trim();

        System.out.print("  Phone Number           : ");
        String phone = sc.nextLine().trim();

        System.out.print("  Email Address          : ");
        String email = sc.nextLine().trim();

        System.out.print("  Date of Birth (yyyy-MM-dd or Enter to skip): ");
        String dobInput = sc.nextLine().trim();

        LocalDate dob = null;
        if (!dobInput.isEmpty()) {
            dob = LocalDate.parse(dobInput, DATE_FORMAT);
        }

        System.out.print("  Gender (M / F / Other) : ");
        String gender = sc.nextLine().trim();

        Patient patient = new Patient(0, name, phone, email, dob, gender);
        int generatedId = dao.add(patient);

        System.out.println();
        System.out.println("  SUCCESS: Patient registered!");
        System.out.println("  Patient ID assigned: " + generatedId);
        System.out.println("  Remember this ID for booking appointments.");
    }

    private static void addDoctor(DoctorDao dao) throws SQLException {

        System.out.println("  -- Add New Doctor --");
        System.out.println();

        System.out.print("  Full Name       : ");
        String name = sc.nextLine().trim();

        System.out.print("  Specialization  : ");
        String specialization = sc.nextLine().trim();

        System.out.print("  Phone Number    : ");
        String phone = sc.nextLine().trim();

        System.out.print("  Email Address   : ");
        String email = sc.nextLine().trim();

        Doctor doctor = new Doctor(0, name, specialization, phone, email);
        int generatedId = dao.add(doctor);

        System.out.println();
        System.out.println("  SUCCESS: Doctor added!");
        System.out.println("  Doctor ID assigned: " + generatedId);
        System.out.println("  Remember this ID for booking appointments.");
    }

    private static void bookAppointment(AppointmentService service)
            throws SQLException {

        System.out.println("  -- Book Appointment --");
        System.out.println();

        int patientId = readInt("  Patient ID                              : ");
        int doctorId  = readInt("  Doctor ID                               : ");

        System.out.print("  Date and Time (yyyy-MM-dd HH:mm)        : ");
        String dateTimeInput = sc.nextLine().trim();
        LocalDateTime when = LocalDateTime.parse(dateTimeInput, DATE_TIME_FORMAT);

        System.out.print("  Duration in minutes (15/30/45/60)       : ");
        System.out.print("  Press Enter for default 30              : ");
        String durationInput = sc.nextLine().trim();
        int duration = durationInput.isEmpty() ? 30 : Integer.parseInt(durationInput);

        System.out.print("  Notes (optional, press Enter to skip)   : ");
        String notes = sc.nextLine().trim();

        int appointmentId = service.bookAppointment(
            patientId, doctorId, when, duration, notes
        );

        System.out.println();
        System.out.println("  SUCCESS: Appointment booked!");
        System.out.println("  Appointment ID : " + appointmentId);
        System.out.println("  Status         : SCHEDULED");
        System.out.println("  Time           : " + dateTimeInput);
        System.out.println("  Duration       : " + duration + " minutes");
    }

    private static void listAllPatients(PatientDao dao) throws SQLException {

        System.out.println("  -- All Registered Patients --");
        System.out.println();

        List<Patient> patients = dao.findAll();

        if (patients.isEmpty()) {
            System.out.println("  No patients registered yet.");
            System.out.println("  Use option 1 to register a patient.");
        } else {
            System.out.println("  Total: " + patients.size() + " patient(s)");
            System.out.println();
            for (Patient p : patients) {
                System.out.println("  " + p);
            }
        }
    }

    private static void listAllDoctors(DoctorDao dao) throws SQLException {

        System.out.println("  -- All Doctors --");
        System.out.println();

        List<Doctor> doctors = dao.findAll();

        if (doctors.isEmpty()) {
            System.out.println("  No doctors added yet.");
            System.out.println("  Use option 2 to add a doctor.");
        } else {
            System.out.println("  Total: " + doctors.size() + " doctor(s)");
            System.out.println();
            for (Doctor d : doctors) {
                System.out.println("  " + d);
            }
        }
    }

    private static void viewDoctorSchedule(AppointmentDao dao)
            throws SQLException {

        System.out.println("  -- Doctor Schedule --");
        System.out.println();

        int doctorId = readInt("  Doctor ID         : ");

        System.out.print("  Date (yyyy-MM-dd)  : ");
        String dateInput = sc.nextLine().trim();
        LocalDate date = LocalDate.parse(dateInput, DATE_FORMAT);

        List<Appointment> appointments =
            dao.findByDoctorAndDate(doctorId, date);

        System.out.println();
        System.out.println(
            "  Schedule for Doctor ID " + doctorId + " on " + dateInput + ":"
        );
        System.out.println();

        if (appointments.isEmpty()) {
            System.out.println("  No appointments on this date.");
        } else {
            System.out.println("  Total: " + appointments.size() + " appointment(s)");
            System.out.println();
            for (Appointment a : appointments) {
                System.out.println("  " + a);
            }
        }
    }

    private static void viewPatientAppointments(AppointmentDao dao)
            throws SQLException {

        System.out.println("  -- Patient Appointments --");
        System.out.println();

        int patientId = readInt("  Patient ID: ");

        List<Appointment> appointments = dao.findByPatient(patientId);

        System.out.println();
        System.out.println("  Appointments for Patient ID " + patientId + ":");
        System.out.println();

        if (appointments.isEmpty()) {
            System.out.println("  No appointments found.");
        } else {
            System.out.println("  Total: " + appointments.size() + " appointment(s)");
            System.out.println();
            for (Appointment a : appointments) {
                System.out.println("  " + a);
            }
        }
    }

    private static void cancelAppointment(AppointmentService service)
            throws SQLException {

        System.out.println("  -- Cancel Appointment --");
        System.out.println();

        int appointmentId = readInt("  Appointment ID to cancel: ");

        boolean cancelled = service.cancelAppointment(appointmentId);

        System.out.println();

        if (cancelled) {
            System.out.println(
                "  SUCCESS: Appointment ID " + appointmentId + " cancelled."
            );
            System.out.println("  Status is now: CANCELLED");
        } else {
            System.out.println(
                "  FAILED: Could not cancel Appointment ID " + appointmentId
            );
            System.out.println(
                "  Reason: Not found, or already CANCELLED or COMPLETED."
            );
        }
    }

    // --------------------------------------------------------
    // UTILITY
    // --------------------------------------------------------

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(
                    "  [!] '" + input + "' is not valid. Enter a whole number."
                );
            }
        }
    }
}