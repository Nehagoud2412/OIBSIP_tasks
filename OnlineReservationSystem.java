import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * OnlineReservationSystem.java
 * Single-file console-based Online Reservation System.
 * - Simple login (default user: admin / password: admin123)
 * - Make reservations (saves to reservations.csv)
 * - Cancel reservations by PNR
 * - View your reservations
 * - Simple "central database" simulated by CSV files in working directory
 *
 * To compile: javac OnlineReservationSystem.java
 * To run:     java OnlineReservationSystem
 *
 * NOTE: This is a simple demo implementation meant for educational purposes.
 */
public class OnlineReservationSystem {

    // Data files (stored in working directory)
    private static final String USERS_FILE = "users.csv";
    private static final String RES_FILE = "reservations.csv";

    // Date format for user input/output
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // In-memory train directory (trainNo -> trainName)
    private static final Map<String, String> TRAIN_DIRECTORY = new LinkedHashMap<>();
    static {
        TRAIN_DIRECTORY.put("12301", "Mumbai Express");
        TRAIN_DIRECTORY.put("12010", "Rajdhani Express");
        TRAIN_DIRECTORY.put("22801", "Coastal Superfast");
        TRAIN_DIRECTORY.put("15645", "Heritage Mail");
        TRAIN_DIRECTORY.put("11022", "Intercity Local");
    }

    private final Scanner sc = new Scanner(System.in);
    private User currentUser = null;

    // Simple in-memory users cache loaded from users.csv
    private final Map<String, String> users = new HashMap<>(); // username -> password

    public static void main(String[] args) {
        OnlineReservationSystem app = new OnlineReservationSystem();
        app.start();
    }

    private void start() {
        System.out.println("=== Welcome to the Online Reservation System ===\n");
        try {
            ensureFilesExist();
            loadUsers();
            mainLoop();
        } catch (IOException e) {
            System.err.println("Failed to initialize data files: " + e.getMessage());
        }
    }

    private void ensureFilesExist() throws IOException {
        // Ensure users file exists and has a default user
        Path usersPath = Paths.get(USERS_FILE);
        if (!Files.exists(usersPath)) {
            System.out.println("Creating default users file...");
            Files.write(usersPath, Collections.singletonList("admin,admin123"), StandardOpenOption.CREATE);
        }

        // Ensure reservations file exists with header
        Path resPath = Paths.get(RES_FILE);
        if (!Files.exists(resPath)) {
            System.out.println("Creating reservations data file...");
            Files.write(resPath, Collections.singletonList("PNR,Username,Name,Age,Gender,TrainNo,TrainName,Class,Date,From,To"), StandardOpenOption.CREATE);
        }
    }

    private void loadUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 2);
                if (parts.length == 2) users.put(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException e) {
            System.err.println("Could not read users file: " + e.getMessage());
        }
    }

    private void mainLoop() {
        while (true) {
            if (currentUser == null) {
                showLoginMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private void showLoginMenu() {
        System.out.println("1) Login");
        System.out.println("2) Register (create new user)");
        System.out.println("3) Exit");
        System.out.print("Choose: ");
        String choice = sc.nextLine().trim();
        switch (choice) {
            case "1": login(); break;
            case "2": registerUser(); break;
            case "3": exitApp(); break;
            default: System.out.println("Invalid option\n");
        }
    }

    private void login() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();

        String stored = users.get(username);
        if (stored != null && stored.equals(password)) {
            currentUser = new User(username);
            System.out.println("Login successful. Welcome, " + username + "!\n");
        } else {
            System.out.println("Login failed. Check credentials.\n");
        }
    }

    private void registerUser() {
        System.out.print("Choose a username: ");
        String username = sc.nextLine().trim();
        if (username.isEmpty()) { System.out.println("Username cannot be empty\n"); return; }
        if (users.containsKey(username)) { System.out.println("Username already exists\n"); return; }
        System.out.print("Choose a password: ");
        String pwd = sc.nextLine().trim();
        if (pwd.isEmpty()) { System.out.println("Password cannot be empty\n"); return; }

        users.put(username, pwd);
        // append to users file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            bw.write(username + "," + pwd);
            bw.newLine();
            System.out.println("Registration successful. You may login now.\n");
        } catch (IOException e) {
            System.err.println("Failed to save user: " + e.getMessage());
        }
    }

    private void exitApp() {
        System.out.println("Goodbye!");
        System.exit(0);
    }

    private void showMainMenu() {
        System.out.println("--- Main Menu ---");
        System.out.println("1) Make Reservation");
        System.out.println("2) Cancel Reservation");
        System.out.println("3) View My Reservations");
        System.out.println("4) Logout");
        System.out.println("5) Exit");
        System.out.print("Choose: ");
        String choice = sc.nextLine().trim();
        switch (choice) {
            case "1": makeReservation(); break;
            case "2": cancelReservation(); break;
            case "3": viewMyReservations(); break;
            case "4": logout(); break;
            case "5": exitApp(); break;
            default: System.out.println("Invalid option\n");
        }
    }

    private void makeReservation() {
        System.out.println("--- Make Reservation ---");
        System.out.print("Passenger name: ");
        String name = sc.nextLine().trim();
        System.out.print("Age: ");
        String ageStr = sc.nextLine().trim();
        int age = 0;
        try { age = Integer.parseInt(ageStr); } catch (Exception e) { System.out.println("Invalid age. Using 0."); }
        System.out.print("Gender (M/F/O): ");
        String gender = sc.nextLine().trim();

        System.out.println("Available trains (TrainNo - TrainName):");
        TRAIN_DIRECTORY.forEach((k,v) -> System.out.println(k + " - " + v));
        System.out.print("Enter Train Number (e.g. 12301): ");
        String trainNo = sc.nextLine().trim();
        String trainName = TRAIN_DIRECTORY.getOrDefault(trainNo, "Unknown Train");
        if (trainName.equals("Unknown Train")) {
            System.out.println("Train number not found in directory. Train name will be set to 'Unknown Train'.");
        } else {
            System.out.println("Train selected: " + trainNo + " - " + trainName);
        }

        System.out.print("Class (Sleeper/AC/FirstClass): ");
        String classType = sc.nextLine().trim();
        System.out.print("Date of journey (yyyy-MM-dd): ");
        String dateStr = sc.nextLine().trim();
        LocalDate doj = null;
        try { doj = LocalDate.parse(dateStr, DATE_FMT); }
        catch (Exception e) { System.out.println("Invalid date, setting to today."); doj = LocalDate.now(); }

        System.out.print("From (place): ");
        String from = sc.nextLine().trim();
        System.out.print("To (destination): ");
        String to = sc.nextLine().trim();

        String pnr = generatePNR();
        Reservation res = new Reservation(pnr, currentUser.username, name, age, gender, trainNo, trainName, classType, doj, from, to);

        // append reservation to file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(RES_FILE, true))) {
            bw.write(res.toCsvLine());
            bw.newLine();
            System.out.println("Reservation successful! Your PNR is: " + pnr + "\n");
        } catch (IOException e) {
            System.err.println("Failed to save reservation: " + e.getMessage());
        }
    }

    private void cancelReservation() {
        System.out.println("--- Cancel Reservation ---");
        System.out.print("Enter PNR to cancel: ");
        String pnr = sc.nextLine().trim();
        if (pnr.isEmpty()) { System.out.println("PNR cannot be empty\n"); return; }

        try {
            List<String> all = Files.readAllLines(Paths.get(RES_FILE));
            if (all.size() <= 1) { System.out.println("No reservations found.\n"); return; }

            String header = all.get(0);
            List<String> remaining = new ArrayList<>();
            remaining.add(header);
            boolean found = false;
            String foundLine = null;
            for (int i = 1; i < all.size(); i++) {
                String line = all.get(i);
                if (line.startsWith(pnr + ",")) {
                    // candidate
                    String[] parts = line.split(",");
                    if (parts.length >= 1) {
                        String owner = parts[1];
                        if (!owner.equals(currentUser.username)) {
                            System.out.println("PNR found but it belongs to user '" + owner + "'. You can only cancel your own reservations.\n");
                            return;
                        }
                    }
                    found = true;
                    foundLine = line;
                    // skip adding (effectively deleting)
                } else {
                    remaining.add(line);
                }
            }

            if (!found) {
                System.out.println("PNR not found.\n");
                return;
            }

            // show reservation to user
            System.out.println("Reservation details:");
            System.out.println(describeCsvLine(foundLine));
            System.out.print("Confirm cancellation? (Y/N): ");
            String yn = sc.nextLine().trim().toUpperCase();
            if (!yn.equals("Y")) { System.out.println("Cancellation aborted.\n"); return; }

            // write back remaining lines
            Files.write(Paths.get(RES_FILE), remaining, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Cancellation confirmed. PNR " + pnr + " canceled.\n");

        } catch (IOException e) {
            System.err.println("Failed to process cancellation: " + e.getMessage());
        }
    }

    private void viewMyReservations() {
        System.out.println("--- My Reservations ---");
        try (BufferedReader br = new BufferedReader(new FileReader(RES_FILE))) {
            String header = br.readLine(); // skip header
            String line;
            boolean any = false;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[1].equals(currentUser.username)) {
                    System.out.println(describeCsvLine(line));
                    any = true;
                }
            }
            if (!any) System.out.println("No reservations found for your account.\n");
            else System.out.println();
        } catch (IOException e) {
            System.err.println("Failed to read reservations: " + e.getMessage());
        }

    }

    private void logout() {
        System.out.println("Logging out " + currentUser.username + "...\n");
        currentUser = null;
    }

    private String generatePNR() {
        // PNR: YYYYMMDDHHMMSS + random 3 digits
        String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int r = new Random().nextInt(900) + 100;
        return ts + r;
    }

    private String describeCsvLine(String line) {
        if (line == null) return "";
        String[] p = line.split(",");
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("PNR: ").append(p[0]).append(" | ");
            sb.append("Passenger: ").append(p[2]).append(" (Age: ").append(p[3]).append(", ").append(p[4]).append(") | ");
            sb.append("Train: ").append(p[5]).append(" - ").append(p[6]).append(" | ");
            sb.append("Class: ").append(p[7]).append(" | ");
            sb.append("Date: ").append(p[8]).append(" | ");
            sb.append("From: ").append(p[9]).append(" -> To: ").append(p[10]);
        } catch (Exception e) { return line; }
        return sb.toString();
    }

    // ------------ Helper classes ----------------

    private static class User {
        String username;
        User(String u) { this.username = u; }
    }

    private static class Reservation {
        String pnr;
        String username;
        String name;
        int age;
        String gender;
        String trainNo;
        String trainName;
        String classType;
        LocalDate date;
        String from;
        String to;

        Reservation(String pnr, String username, String name, int age, String gender, String trainNo, String trainName, String classType, LocalDate date, String from, String to) {
            this.pnr = pnr; this.username = username; this.name = name; this.age = age; this.gender = gender;
            this.trainNo = trainNo; this.trainName = trainName; this.classType = classType; this.date = date; this.from = from; this.to = to;
        }

        String toCsvLine() {
            return String.join(",",
                    pnr,
                    username,
                    escapeCsv(name),
                    String.valueOf(age),
                    escapeCsv(gender),
                    escapeCsv(trainNo),
                    escapeCsv(trainName),
                    escapeCsv(classType),
                    date.format(DATE_FMT),
                    escapeCsv(from),
                    escapeCsv(to)
            );
        }
        private String escapeCsv(String s) {
            if (s == null) return "";
            if (s.contains(",") || s.contains("\n") || s.contains("\r")) {
                s = s.replace("\"", "\"\"");
                return "\"" + s + "\"";
            }
            return s;
        }
    }
}
