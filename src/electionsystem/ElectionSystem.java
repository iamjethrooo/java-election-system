package electionsystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Scanner;

public class ElectionSystem {

    private final String DB_URL = "jdbc:derby://localhost:1527/db_election";
    private final String USERNAME = "root";
    private final String PASSWORD = "root";

    private final String DRIVER1 = "org.apache.derby.jdbc.ClientDriver";
    private final String DRIVER2 = "org.apache.derby.jdbc.EmbeddedDriver";

    private static int current_user_id = -1;
    private static boolean isLoggedIn = false;

    Scanner sc;
    Statement stmt;
    ResultSet rs;
    Connection con;

    public static void main(String[] args) {
        ElectionSystem es = new ElectionSystem();

        es.createSQLConnection();
        es.start();
    }

    private Connection createSQLConnection() {
        try {
            Class.forName(DRIVER1);
            Class.forName(DRIVER2);
            con = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            System.out.println("Connected to the database!");
            return con;
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Cannot connect to the database!");
        }
        return null;
    }

    private void start() {
        while (true) {
            try {
                sc = new Scanner(System.in);
                stmt = con.createStatement();

                System.out.print("[1] Login\n"
                        + "[2] Sign-up\n"
                        + "[3] Exit\n"
                        + "Enter choice: ");
                String choice = sc.nextLine();
                switch (choice) {
                    case "1":
                        login();
                        break;
                    case "2":
                        register();
                        break;
                    case "3":
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid input!");
                        break;
                }

            } catch (SQLException | InputMismatchException ex) {
                System.out.println("Please enter correct input!");
            }
        }

    }

    private boolean checkUniqueUser(String username) {
        try {
            stmt = con.createStatement();
            String select = String.format("SELECT username FROM tbl_users WHERE username = '%s'", username);
            rs = stmt.executeQuery(select);
            if (rs.next()) {
                System.out.println("Username is taken!");
                return true;// username is not unique
            } else {
                return false;// username is unique
            }
        } catch (SQLException ex) {
            System.out.println("Error!");
        }
        return false;
    }

    private boolean isDate(String s) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            Date date = sdf.parse(s);
            System.out.println(date);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    private int getUserId(String username) {
        try {
            stmt = con.createStatement();
            String select = String.format("SELECT user_id FROM tbl_users WHERE username = '%s'", username);
            rs = stmt.executeQuery(select);
            if (rs.next()) {
                int user_id = rs.getInt("user_id");
                return user_id;
            } else {
                System.out.println("ERRORR");
            }
        } catch (SQLException ex) {
            System.out.println("Error!");            
        }
        return -1;
    }

    private void verifyAccount() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);
            String select = "SELECT * FROM temp_users";
            ResultSet temp_rs = stmt.executeQuery(select);
            System.out.printf("%5s %32s %32s %32s %5s %32s\n", "ID", "Username", "First Name", "Last Name", "Sex", "Birth Date");
            while (temp_rs.next()) {
                int temp_id = temp_rs.getInt("temp_id");
                String temp_username = temp_rs.getString("username");
                String temp_password = temp_rs.getString("password");
                String temp_fname = temp_rs.getString("f_name");
                String temp_lname = temp_rs.getString("l_name");
                String temp_sex = temp_rs.getString("gender");
                String temp_bdate = temp_rs.getString("b_date");
                System.out.printf("%5d %32s %32s %32s %5s %32s\n", temp_id, temp_username, temp_fname, temp_lname, temp_sex, temp_bdate);
            }
            System.out.print("Enter ID to verify: ");
            int input = sc.nextInt();
            sc.nextLine();

            String sql = String.format("SELECT * FROM temp_users WHERE temp_id = %d", input);
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                int account_type_id = 3;
                String username = rs.getString("username");
                String password = rs.getString("password");
                String fname = rs.getString("f_name");
                String lname = rs.getString("l_name");
                String sex = rs.getString("gender");
                String bdate = rs.getString("b_date");
                String insert = String.format("INSERT INTO tbl_users (username, password, f_name, l_name, gender, b_date, account_type_id) "
                        + "VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %d)", username, password, fname, lname, sex, bdate, account_type_id);

                int i = stmt.executeUpdate(insert);
                boolean success = (i > 0);
                if (success) {
                    String delete = String.format("DELETE FROM temp_users WHERE temp_id = %d", input);
                    stmt.executeUpdate(delete);
                    System.out.printf("Added new user: %s\n", username);
                    String insert_voter = String.format("INSERT INTO tbl_voters (user_id, has_voted) VALUES (%d, %s)", getUserId(username), "false");
                    stmt.executeUpdate(insert_voter);
                } else {
                    System.out.println("User does not exist!");
                }
            } else {
                System.out.println("User does not exist!");
            }

        } catch (SQLException | InputMismatchException ex) {
            System.out.println("Please enter correct input!");
        }
    }

    private void login() {
        try {
            sc = new Scanner(System.in);
            System.out.print("Enter username: ");
            String username = sc.nextLine();
            System.out.print("Enter password: ");
            String password = sc.nextLine();

            stmt = con.createStatement();
            String query = "SELECT * FROM tbl_users WHERE username = '" + username + "' AND password = '" + password + "'";
            stmt.executeQuery(query);

            rs = stmt.getResultSet();
            if (rs.next()) {
                int user_id = rs.getInt("user_id");
                int accessLevel = rs.getInt("account_type_id");
                String f_name = rs.getString("f_name");
                String l_name = rs.getString("l_name");

                if (accessLevel == 3) {
                    String select = String.format("SELECT has_voted FROM tbl_voters WHERE user_id = %d", user_id);
                    ResultSet temp_rs = stmt.executeQuery(select);

                    if (temp_rs.next()) {
                        boolean has_voted = temp_rs.getBoolean("has_voted");

                        if (has_voted) {
                            System.out.println("Sorry, you have already voted. You will now be logged out.");
                        } else {
                            this.isLoggedIn = true;
                            this.current_user_id = user_id;

                            System.out.print("Login success! ");
                            System.out.printf("Welcome %s %s!\n", f_name, l_name);

                            showOptions(accessLevel);
                        }
                    }
                } else {
                    this.isLoggedIn = true;
                    this.current_user_id = rs.getInt("user_id");

                    System.out.print("Login success! ");
                    System.out.printf("Welcome %s %s!\n", f_name, l_name);

                    showOptions(accessLevel);
                }
            } else {
                System.out.println("Login failed :(");
            }
        } catch (SQLException e) {
            System.out.println("An error has ocurred and you cannot login at the moment.");
        }
    }

    private void register() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);
            int accountType = 3;

            String username;
            String password;
            String fName;
            String lName;
            String gender;
            String bDate;

            String insert = "";

            int privilege = checkPrivilege();
            if (privilege == 1) {
                System.out.print("Enter account type id: ");
                accountType = sc.nextInt();
                sc.nextLine();
            }

            boolean notUnique;
            do {
                System.out.print("Enter new username: ");
                username = sc.nextLine();

                notUnique = checkUniqueUser(username);
            } while (notUnique);

            System.out.print("Enter new password: ");
            password = sc.nextLine();

            System.out.print("Enter first name: ");
            fName = sc.nextLine();

            System.out.print("Enter last name: ");
            lName = sc.nextLine();

            do {
                System.out.print("Enter sex (m or f ONLY): ");
                gender = sc.nextLine();
            } while (!gender.equalsIgnoreCase("m") && !gender.equalsIgnoreCase("f"));

            do {
                System.out.print("Enter bdate (mm/dd/yy): ");
                bDate = sc.nextLine();
            } while (!isDate(bDate));

            if (isLoggedIn) {
                insert = String.format("INSERT INTO tbl_users (username, password, f_name, l_name, gender, b_date, account_type_id) "
                        + "VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %d)", username, password, fName, lName, gender.toLowerCase(), bDate, accountType);
                stmt.executeUpdate(insert);

                String sql = String.format("SELECT username, user_id FROM tbl_users "
                        + "WHERE username = '%s' AND password = '%s'", username, password);
                rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String uname = rs.getString("username");
                    if (accountType == 3) {
                        int user_id = rs.getInt("user_id");
                        String insertVoter = String.format("INSERT INTO tbl_voters (user_id, has_voted) VALUES (%d, %s)", user_id, "false");
                        stmt.executeUpdate(insertVoter);
                    }

                    System.out.print("Registration Successful! ");
                    System.out.printf("Registered new account: %s!\n", uname);
                }
            } else if (isLoggedIn == false) {
                insert = String.format("INSERT INTO temp_users (username, password, f_name, l_name, gender, b_date) "
                        + "VALUES ('%s', '%s', '%s', '%s', '%s', '%s')", username, password, fName, lName, gender.toLowerCase(), bDate);
                stmt.executeUpdate(insert);
                System.out.println("Thank you for signing up! Please wait for an admin to approve your registration");
            } else {
                System.err.println("Error!");
            }

        } catch (SQLException e) {
            System.out.printf("An error has occurred and you cannot register at the moment. Error message: %s\n", e);
        }
    }

    private int checkPrivilege() {
        try {
            stmt = con.createStatement();
            String sql = String.format("SELECT account_type_id FROM tbl_users WHERE user_id = %d", this.current_user_id);
            stmt.executeQuery(sql);
            rs = stmt.getResultSet();
            if (rs.next()) {
                int privilege = rs.getInt("account_type_id");
                return privilege;
            }
        } catch (SQLException ex) {
            System.out.println("Error!");
        }
        return 0;
    }

    private void viewAccountTypes() {
        try {
            stmt = con.createStatement();
            String select = String.format("SELECT * FROM tbl_account_type");
            rs = stmt.executeQuery(select);
            System.out.printf("%5s %32s\n", "ID", "Account Type");
            while (rs.next()) {
                int account_type_id = rs.getInt("account_type_id");
                String account_type = rs.getString("account_type");
                System.out.printf("%5s %32s\n", account_type_id, account_type);
            }
        } catch (SQLException ex) {
            System.out.println("Error!");
        }
    }

    private void showOptions(int accessLevel) {
        loggin:
        while (true) {
            try {
                String choice;

                stmt = con.createStatement();
                sc = new Scanner(System.in);

                switch (accessLevel) {
                    case 1:// system admin
                        System.out.print("[1] Add new account\n"
                                + "[2] Manage positions\n"
                                + "[3] Manage partylists\n"
                                + "[4] Manage candidates\n"
                                + "[5] View results\n"
                                + "[6] Verify accounts\n"
                                + "[0] Log-out\n"
                                + "Enter choice: ");
                        choice = sc.nextLine();
                        switch (choice.charAt(0)) {
                            case '1':
                                viewAccountTypes();
                                register();
                                break;
                            case '2':
                                managePositions();
                                break;
                            case '3':
                                managePartylists();
                                break;
                            case '4':
                                manageCandidates();
                                break;
                            case '5':
                                viewResults();
                                break;
                            case '6':
                                verifyAccount();
                                break;
                            case '0':
                                System.out.println("Logging out...");
                                break loggin;
                            default:
                                System.out.println("Invalid input!");
                                break;
                        }
                        break;
                    case 2:// admin
                        System.out.print("[1] Register new voter\n"
                                + "[2] Manage positions\n"
                                + "[3] Manage partylists\n"
                                + "[4] Manage candidates\n"
                                + "[5] View results\n"
                                + "[0] Log-out\n"
                                + "Enter choice: ");
                        choice = sc.nextLine();
                        switch (choice.charAt(0)) {
                            case '1':
                                register();
                                break;
                            case '2':
                                managePositions();
                                break;
                            case '3':
                                managePartylists();
                                break;
                            case '4':
                                manageCandidates();
                                break;
                            case '5':
                                viewResults();
                                break;
                            case '0':
                                System.out.println("Logging out...");
                                break loggin;
                            default:
                                System.out.println("Invalid input!");
                                break;
                        }
                        break;
                    case 3:// voter
                        System.out.print("[1] View Candidates\n"
                                + "[2] Cast Vote\n"
                                + "[0] Log-out\n"
                                + "Enter choice: ");
                        choice = sc.nextLine();
                        switch (choice.charAt(0)) {
                            case '1':
                                viewCandidates();
                                break;
                            case '2':
                                vote();
                                break;
                            case '0':
                                System.out.println("Logging out...");
                                break loggin;
                            default:
                                System.out.println("Invalid input!");
                                break;
                        }
                        break;
                    default:
                        System.out.println("Error!");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Please enter correct input!");
            } finally {
                this.current_user_id = -1;
                this.isLoggedIn = false;
            }
        }

    }

    private void manageCandidates() {
        scanner:
        while (true) {
            sc = new Scanner(System.in);

            System.out.print("[1] Register new candidate\n"
                    + "[2] Delete candidate\n"
                    + "[3] Edit candidate information\n"
                    + "[4] View candidates\n"
                    + "[0] Back\n"
                    + "Enter your choice: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    addCandidate();
                    break;
                case "2":
                    viewCandidates();
                    deleteCandidate();
                    break;
                case "3":
                    viewCandidates();
                    editCandidate();
                    break;
                case "4":
                    viewCandidates();
                    break;
                case "0":
                    break scanner;
                default:
                    System.out.println("Invalid input!");
                    break;
            }
        }

    }

    private void viewCandidates() {
        try {
            String sql = "SELECT candidate_id, first_name, last_name, sex, birth_date, position_name, partylist "
                    + "FROM tbl_candidates,tbl_positions,tbl_partylists "
                    + "WHERE tbl_candidates.position_id = tbl_positions.position_id AND tbl_candidates.partylist_id = tbl_partylists.partylist_id";
            stmt = con.createStatement();
            stmt.executeQuery(sql);
            rs = stmt.getResultSet();
            System.out.printf("%3s %32s %32s %32s %5s %32s %32s\n", "#", "First Name", "Last Name", "Sex", "Birth Date", "Position", "Partylist");
            while (rs.next()) {
                int c_id = rs.getInt("candidate_id");
                String fname = rs.getString("first_name");
                String lname = rs.getString("last_name");
                String sex = rs.getString("sex");
                String bdate = rs.getString("birth_date");
                String pname = rs.getString("position_name");
                String partylist = rs.getString("partylist");
                System.out.printf("%3d %32s %32s %5s %32s %32s %32s\n", c_id, fname, lname, sex, bdate, pname, partylist);
            }

        } catch (SQLException e) {
            System.out.println("Due to an error, you cannot view candidates at the moment.");
        }

    }

    private void addCandidate() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);
            System.out.print("Enter candidate first name: ");
            String fname = sc.nextLine();

            System.out.print("Enter candidate last name: ");
            String lname = sc.nextLine();

            String sex;
            do {
                System.out.print("Enter candidate sex (m or f):");
                sex = sc.nextLine();
            } while (!sex.equalsIgnoreCase("m") && !sex.equalsIgnoreCase("f"));

            String bdate;
            do {
                System.out.print("Enter candidate birth date (mm/dd/yyyy): ");
                bdate = sc.nextLine();
            } while (!isDate(bdate));

            int positionId;
            viewPositions();
            scanner:
            while (true) {
                System.out.print("Enter candidate position: ");
                positionId = sc.nextInt();
                String positionSelect = String.format("SELECT position_id FROM tbl_positions WHERE position_id = %d", positionId);
                ResultSet position_rs = stmt.executeQuery(positionSelect);
                if (position_rs.next()) {
                    break scanner;
                }
            }

            int partylistId;
            viewPartylists();
            scanner:
            while (true) {
                System.out.print("Enter candidate partylist: ");
                partylistId = sc.nextInt();
                String partylistSelect = String.format("SELECT partylist_id FROM tbl_partylists WHERE partylist_id = %d", partylistId);
                ResultSet partylist_rs = stmt.executeQuery(partylistSelect);
                if (partylist_rs.next()) {
                    int votes = 0;
                    String sql = String.format("INSERT INTO tbl_candidates (first_name, last_name, sex, birth_date, position_id, partylist_id, votes) "
                            + "VALUES ('%s', '%s', '%s', '%s', %d, %d, %d)", fname, lname, sex.toLowerCase(), bdate, positionId, partylistId, votes);
                    stmt.executeUpdate(sql);
                    System.out.println("Added candidate!");
                    break scanner;
                }
            }

        } catch (SQLException | InputMismatchException ex) {
            System.out.print("Incorrect input! Candidate will not be registered.\n");
        }
    }

    private void deleteCandidate() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);
            String candidateId;

            System.out.print("Enter candidate ID to delete: ");
            candidateId = sc.nextLine();

            String sql = String.format("DELETE FROM tbl_candidates WHERE candidate_id = %s", candidateId);
            int i = stmt.executeUpdate(sql);
            boolean success = (i > 0);
            if (success) {
                System.out.println("Deleted candidate successfully!");
            } else {
                System.out.printf("Delete failed! Candidate with ID # %s does not exist!\n", candidateId);
            }
        } catch (SQLException ex) {
            System.out.print("Incorrect input! Candidate will not be deleted.");
        }

    }

    private void editCandidate() {
        try {
            scanner:
            while (true) {

                sc = new Scanner(System.in);
                stmt = con.createStatement();

                int candidateId = 0;
                System.out.print("Enter candidate ID to edit: ");
                candidateId = sc.nextInt();
                sc.nextLine();

                String select = String.format("SELECT candidate_id FROM tbl_candidates WHERE candidate_id = %d", candidateId);

                stmt.executeQuery(select);
                rs = stmt.getResultSet();
                if (rs.next()) {

                    String columnName = "";

                    System.out.print("[1] First Name\n"
                            + "[2] Last Name\n"
                            + "[3] Sex (M or F)\n"
                            + "[4] Birth Date\n"
                            + "[5] Position ID\n"
                            + "[6] Partylist ID\n"
                            + "[0] Back\n"
                            + "Enter column # to edit: ");
                    String choice = sc.nextLine();
                    switch (choice) {
                        case "1":
                            columnName = "first_name";
                            break;
                        case "2":
                            columnName = "last_name";
                            break;
                        case "3":
                            columnName = "sex";
                            break;
                        case "4":
                            columnName = "birth_date";
                            break;
                        case "5":
                            viewPositions();
                            columnName = "position_id";
                            break;
                        case "6":
                            viewPartylists();
                            columnName = "partylist_id";
                            break;
                        case "0":
                            break scanner;
                        default:
                            System.out.println("Invalid input!");
                            break;
                    }

                    if (choice.equals("1") || choice.equals("2") || choice.equals("3") || choice.equals("4")) {
                        System.out.print("Enter new value: ");
                        String newValue = sc.nextLine();
                        String sql = String.format("UPDATE tbl_candidates SET %s = '%s' WHERE candidate_id = %d", columnName, newValue, candidateId);
                        stmt.executeUpdate(sql);
                        System.out.println("Success!");
                    } else if (choice.equals("5") || choice.equals("6")) {
                        System.out.print("Enter new value: ");
                        String newValue = sc.nextLine();
                        String sql = String.format("UPDATE tbl_candidates SET %s = %s WHERE candidate_id = %d", columnName, newValue, candidateId);
                        stmt.executeUpdate(sql);
                        System.out.println("Success!");
                    }

                } else {
                    System.out.println("Candidate does not exist!");
                }
                break scanner;
            }
        } catch (SQLException | InputMismatchException ex) {
            System.out.println("Please enter correct input!");
        }
    }

    private void vote() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);

            String select = "SELECT * FROM tbl_positions";

            stmt.executeQuery(select);

            ResultSet position_rs;
            position_rs = stmt.getResultSet();

            while (position_rs.next()) {
                int position_id = position_rs.getInt("position_id");
                int votes_allowed = position_rs.getInt("votes_allowed");
                String pname = position_rs.getString("position_name");

                System.out.printf("Voting for: %s\n", pname);
                System.out.printf("%3s %32s %32s %5s %32s %32s %32s\n", "#", "First Name", "Last Name", "Sex", "Birth Date", "Position", "Partylist");

                String sql = String.format("SELECT candidate_id, first_name, last_name, sex, birth_date, position_name, partylist "
                        + "FROM tbl_candidates,tbl_positions,tbl_partylists "
                        + "WHERE tbl_candidates.position_id = tbl_positions.position_id "
                        + "AND tbl_candidates.partylist_id = tbl_partylists.partylist_id "
                        + "AND tbl_candidates.position_id = %d", position_id);

                Statement candidate_stmt;
                candidate_stmt = con.createStatement();
                candidate_stmt.executeQuery(sql);

                ResultSet candidate_rs;
                candidate_rs = candidate_stmt.getResultSet();

                while (candidate_rs.next()) {

                    int c_id = candidate_rs.getInt("candidate_id");
                    String fname = candidate_rs.getString("first_name");
                    String lname = candidate_rs.getString("last_name");
                    String sex = candidate_rs.getString("sex");
                    String bdate = candidate_rs.getString("birth_date");
                    String pName = candidate_rs.getString("position_name");
                    String partylist = candidate_rs.getString("partylist");

                    System.out.printf("%3d %32s %32s %5s %32s %32s %32s\n", c_id, fname, lname, sex, bdate, pname, partylist);
                }

                for (int i = 1; i <= votes_allowed; i++) {
                    try {
                        System.out.print("Enter vote #: ");

                        int candidateId = sc.nextInt();
                        sc.nextLine();
                        String c_select = String.format("SELECT candidate_id FROM tbl_candidates WHERE candidate_id = %d AND position_id = %d", candidateId, position_id);

                        String update = String.format("UPDATE tbl_candidates SET votes = votes + 1 WHERE candidate_id = %d", candidateId);

                        ResultSet verification_rs;// checks kung merong candidate with user-inputted id
                        Statement verification_stmt = con.createStatement();
                        verification_rs = verification_stmt.executeQuery(c_select);
                        if (verification_rs.next()) {
                            verification_stmt.executeUpdate(update);
                            System.out.print("Vote added!\n");
                        } else if (candidateId == 000) {
                            break;
                        } else {
                            i--;
                            System.out.println("Due to an error, your vote will not be counted.");
                        }
                    } catch (InputMismatchException ex) {
                        System.out.print("Please enter correct inputt!\n");
                        i--;
                        sc.next();
                    }

                }

            }
        } catch (SQLException ex) {
            System.out.print("Please enter correct input!\n");
        } finally {
            try {
                String update = String.format("UPDATE tbl_voters SET has_voted = true WHERE user_id = %d", this.current_user_id);
                stmt = con.createStatement();
                stmt.executeUpdate(update);
            } catch (SQLException ex) {
                System.out.println("CHINA NAMBA WAN");
            }

        }
    }

    private void managePositions() {
        sc = new Scanner(System.in);

        String choices = "[1] Add Positions\n"
                + "[2] Delete Positions\n"
                + "[3] Edit Position\n"
                + "[4] View Positions\n"
                + "[0] Back\n"
                + "Enter your choice: ";
        String position;
        scanner:
        while (true) {
            System.out.print(choices);
            switch (sc.nextLine()) {
                case "1":
                    addPosition();
                    break;
                case "2":
                    viewPositions();
                    deletePosition();
                    break;
                case "3":
                    viewPositions();
                    editPosition();
                    break;
                case "4":
                    viewPositions();
                    break;
                case "0":
                    break scanner;
                default:
                    System.out.println("Invalid input!");
                    break;
            }
        }
    }

    private void viewPositions() {
        try {
            stmt = con.createStatement();
            String select = "SELECT * FROM tbl_positions";
            stmt.executeQuery(select);

            rs = stmt.getResultSet();
            System.out.printf("%12s %32s %13s", "Position ID", "Position Name", "Votes Allowed\n");
            while (rs.next()) {
                int pid = rs.getInt("position_id");
                String pname = rs.getString("position_name");
                int votesAllowed = rs.getInt("votes_allowed");
                System.out.printf("%12d %32s %13s\n", pid, pname, votesAllowed);
            }

        } catch (SQLException ex) {
            System.out.println("Error!");
        }
    }

    private void addPosition() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);

            System.out.print("Enter position: ");
            String newPosition = sc.nextLine();

            System.out.print("Enter number of winners: ");
            int votes_allowed = sc.nextInt();
            sc.nextLine();
            String sql = String.format("INSERT INTO tbl_positions (position_name, votes_allowed) VALUES ('%s', %d)", newPosition, votes_allowed);
            stmt.executeUpdate(sql);

            System.out.println("Success!");
        } catch (SQLException | InputMismatchException e) {
            System.out.println("Please enter correct input!");
        }
    }

    private void deletePosition() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);
            System.out.print("Enter position ID to delete: ");
            String positionId = sc.nextLine();

            String select = String.format("SELECT position_id FROM tbl_positions WHERE position_id = %s", positionId);
            rs = stmt.executeQuery(select);
            if (rs.next()) {
                String sql = String.format("DELETE FROM tbl_positions WHERE position_id = %s", positionId);
                stmt.executeUpdate(sql);
                System.out.println("Success!");
            } else {
                System.out.println("Entry does not exist!");
            }

        } catch (SQLException ex) {
            System.out.println("Please enter correct input!");
        }
    }

    private void editPosition() {
        try {
            scanner:
            while (true) {
                stmt = con.createStatement();
                sc = new Scanner(System.in);

                System.out.print("Enter position ID to edit: ");
                String positionId = sc.nextLine();

                String sql;
                String columnName;
                String value;

                String select = String.format("SELECT position_id FROM tbl_positions WHERE position_id = %s", positionId);
                rs = stmt.executeQuery(select);
                if (rs.next()) {
                    System.out.print("[1] Position Name\n"
                            + "[2] Votes Allowed\n"
                            + "[0] Back\n"
                            + "Enter column # to edit: ");
                    String choice = sc.nextLine();

                    switch (choice) {
                        case "1":// position name
                            columnName = "position_name";
                            System.out.print("Enter new value: ");
                            value = sc.nextLine();
                            sql = String.format("UPDATE tbl_positions SET %s = '%s' WHERE position_id = %s", columnName, value, positionId);
                            stmt.executeUpdate(sql);
                            System.out.println("Success!");
                            break scanner;
                        case "2":// votes allowed
                            columnName = "votes_allowed";
                            System.out.print("Enter new value: ");
                            value = sc.nextLine();
                            sql = String.format("UPDATE tbl_positions SET %s = %s WHERE position_id = %s", columnName, value, positionId);
                            stmt.executeUpdate(sql);
                            System.out.println("Success!");
                            break scanner;
                        case "0":// back
                            break scanner;
                        default:
                            System.out.println("Invalid input!");
                            break;
                    }

                } else {
                    System.out.println("Entry does not exist!");
                }

            }

        } catch (SQLException ex) {
            System.out.println("Please enter correct input!");
        }
    }

    private void managePartylists() {
        sc = new Scanner(System.in);
        String choices = "[1] Add Partylist\n"
                + "[2] Delete Partylist\n"
                + "[3] Edit Partylist\n"
                + "[4] View Partylist\n"
                + "[0] Back\n"
                + "Enter your choice: ";
        String party;
        scanner:
        while (true) {
            System.out.print(choices);
            switch (sc.nextLine()) {
                case "1":
                    viewPartylists();
                    addPartylist();
                    break;
                case "2":
                    viewPartylists();
                    deletePartylist();
                    break;
                case "3":
                    viewPartylists();
                    editPartylist();
                    break;
                case "4":
                    viewPartylists();
                    break;
                case "0":
                    break scanner;
                default:
                    System.out.println("Invalid input!");
                    break;
            }
        }
    }

    private void viewPartylists() {
        try {
            stmt = con.createStatement();
            String select = "SELECT * FROM tbl_partylists";
            stmt.executeQuery(select);

            rs = stmt.getResultSet();
            System.out.printf("%12s %32s\n", "Partylist ID", "Partylist Name");
            while (rs.next()) {
                int partylistId = rs.getInt("partylist_id");
                String partylistName = rs.getString("partylist");
                System.out.printf("%12d %32s\n", partylistId, partylistName);
            }

        } catch (SQLException ex) {
            System.out.println("Error!");
        }
    }

    private void addPartylist() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);

            System.out.print("Enter partylist: ");
            String newPartylist = sc.nextLine();
            String select = String.format("SELECT partylist FROM tbl_partylists WHERE lower(partylist) = '%s'", newPartylist.toLowerCase());
            rs = stmt.executeQuery(select);
            if (rs.next()) {
                System.out.println("Partylist with same name already exists!");
            } else {
                String sql = String.format("INSERT INTO tbl_partylists (partylist) VALUES ('%s')", newPartylist);
                stmt.executeUpdate(sql);

                System.out.println("Success!");
            }

        } catch (SQLException ex) {
            System.out.println("Error!");
        }
    }

    private void deletePartylist() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);
            System.out.print("Enter partylist ID to delete: ");
            String partylistId = sc.nextLine();

            String select = String.format("SELECT partylist_id FROM tbl_partylists WHERE partylist_id = %s", partylistId);
            rs = stmt.executeQuery(select);
            if (rs.next()) {
                String sql = String.format("DELETE FROM tbl_partylists WHERE partylist_id = %s", partylistId);
                stmt.executeUpdate(sql);

                System.out.println("Success!");
            } else {
                System.out.println("Entry does not exist!");
            }

        } catch (SQLException ex) {
            System.out.println("Please enter correct input!");
        }
    }

    private void editPartylist() {
        try {
            stmt = con.createStatement();
            sc = new Scanner(System.in);

            System.out.print("Enter partylist ID to edit: ");
            String partylistId = sc.nextLine();

            String select = String.format("SELECT partylist_id FROM tbl_partylists WHERE partylist_id = %s", partylistId);
            rs = stmt.executeQuery(select);
            if (rs.next()) {
                System.out.print("Enter new partylist name: ");
                String partylistName = sc.nextLine();
                String sql = String.format("UPDATE tbl_partylists SET partylist = '%s' WHERE partylist_id = %s", partylistName, partylistId);
                stmt.executeUpdate(sql);

                System.out.println("Success!");
            } else {
                System.out.println("Entry does not exist!");
            }

        } catch (SQLException ex) {
            System.out.println("Please enter correct input!");
        }
    }

    private void viewResults() {
        try {
            stmt = con.createStatement();
            String position_sql = "SELECT * FROM tbl_positions";
            stmt.executeQuery(position_sql);
            rs = stmt.getResultSet();
            while (rs.next()) {
                String position = rs.getString("position_name");
                int votes_allowed = rs.getInt("votes_allowed");
                int position_id = rs.getInt("position_id");
                System.out.print(position + "\n");
                String select = String.format("SELECT votes, candidate_id, first_name, last_name, sex, birth_date, position_name, partylist "
                        + "FROM tbl_candidates,tbl_positions,tbl_partylists "
                        + "WHERE tbl_candidates.position_id = tbl_positions.position_id "
                        + "AND tbl_candidates.partylist_id = tbl_partylists.partylist_id "
                        + "AND tbl_candidates.position_id = %d ORDER BY tbl_candidates.position_id ASC, votes DESC FETCH FIRST %d ROWS ONLY", position_id, votes_allowed);

                Statement result_stmt = con.createStatement();
                ResultSet result_rs = result_stmt.executeQuery(select);
                System.out.printf("%32s %32s %5s %32s %6s\n", "First Name", "Last Name", "Sex", "Partylist", "Votes");
                while (result_rs.next()) {
                    String first_name = result_rs.getString("first_name");
                    String last_name = result_rs.getString("last_name");
                    String sex = result_rs.getString("sex");
                    String partylist = result_rs.getString("partylist");
                    int votes = result_rs.getInt("votes");
                    System.out.printf("%32s %32s %5s %32s %6d\n", first_name, last_name, sex, partylist, votes);
                }
            }

        } catch (SQLException ex) {
            System.out.println("Error! You cannot view results at the moment.");
        }
    }
}