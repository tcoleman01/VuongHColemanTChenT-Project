import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.*;

public class AdminCli {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DB = "soccer_analytics_db";

    private static Map<String, String> dotEnv = new HashMap<>();

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 is always supported
        }
        loadDotEnv();

        DbConfig config = loadConfig();
        try (Connection conn = connect(config); Scanner scanner = new Scanner(System.in)) {
            System.out.println("Connected to MySQL at " + config.host + ":" + config.port + "/" + config.database);

            Role role = null;
            while (role == null) {
                role = login(conn, scanner);
            }

            if (role == Role.ADMIN) {
                adminMenu(conn, scanner);
            } else {
                userMenu(conn, scanner);
            }
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    private static void loadDotEnv() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(envPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                dotEnv.put(key, value);
            }
        } catch (IOException e) {
            System.out.println("Warning: failed to read .env: " + e.getMessage());
        }
    }

    private static DbConfig loadConfig() {
        String host = getConfig("MYSQL_HOST", DEFAULT_HOST);
        int port = parseInt(getConfig("MYSQL_PORT", String.valueOf(DEFAULT_PORT)), DEFAULT_PORT);
        String user = getConfig("MYSQL_USER", "root");
        String password = getConfig("MYSQL_PASSWORD", "");
        String database = getConfig("MYSQL_DATABASE", DEFAULT_DB);
        return new DbConfig(host, port, user, password, database);
    }

    private static String getConfig(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = dotEnv.get(key);
        }
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Connection connect(DbConfig config) throws SQLException {
        String url = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            config.host,
            config.port,
            config.database
        );
        return DriverManager.getConnection(url, config.user, config.password);
    }

    
    private static void pauseForEnter(Scanner scanner) {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // Handle Logins for both admin and user
    private static Role login(Connection conn, Scanner scanner) {
        String adminUser = getConfig("ADMIN_USER", "");
        String adminPass = getConfig("ADMIN_PASS", "");

        System.out.println("\nLogin (type 'q' to quit)");
        String roleInput = prompt(scanner, "Role (admin|user)");

        if (roleInput.equalsIgnoreCase("q")) {
            System.out.println("Goodbye.");
            System.exit(0);
        }

        Role role = parseRole(roleInput);
        if (role == null) {
            System.out.println("Invalid role. Use admin or user.");
            return null;
        }

        String username = prompt(scanner, "Username");
        String password = prompt(scanner, "Password");

        if (role == Role.ADMIN) {
            if (adminUser.isEmpty() || adminPass.isEmpty()) {
                System.out.println("Warning: ADMIN_USER/ADMIN_PASS not set. Allowing admin login for any credentials.");
                return Role.ADMIN;
            }
            if (adminUser.equals(username) && adminPass.equals(password)) {
                return Role.ADMIN;
            } else {
                System.out.println("Invalid username or password.");
                return null;
            }
        }

        String sql = "SELECT is_admin FROM user WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean isAdmin = rs.getBoolean("is_admin");
                if (isAdmin) {
                    System.out.println("Access denied. Use admin role for admin accounts.");
                    return null;
                }
                return Role.USER;
            } else {
                System.out.println("Invalid username or password.");
                return null;
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
            return null;
        }
    }

    // ============================================================
    // ADMIN MENU
    // ============================================================
    private static void adminMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nAdmin Menu");
            System.out.println("1. Player");
            System.out.println("2. League");
            System.out.println("3. Match");
            System.out.println("4. Market Value");
            System.out.println("5. Match Performance");
            System.out.println("6. Transfer");
            System.out.println("7. User View");
            System.out.println("8. Exit");

            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1": playerMenu(conn, scanner); break;
                case "2": leagueMenu(conn, scanner); break;
                case "3": matchMenu(conn, scanner); break;
                case "4": marketValueMenu(conn, scanner); break;
                case "5": matchPerformanceMenu(conn, scanner); break;
                case "6": transferMenu(conn, scanner); break;
                case "7": userMenu(conn, scanner); break;
                case "8": System.out.println("Goodbye."); return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void playerMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nPlayer Menu");
            System.out.println("1. Create");
            System.out.println("2. Update");
            System.out.println("3. Delete");
            System.out.println("4. Read");
            System.out.println("5. Back");
            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1": createPlayer(conn, scanner); pauseForEnter(scanner); break;
                case "2": updatePlayer(conn, scanner); pauseForEnter(scanner); break;
                case "3": deleteById(conn, scanner, "Player", "player_id"); pauseForEnter(scanner); break;
                case "4": readPlayers(conn, scanner); pauseForEnter(scanner); break;
                case "5": return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void leagueMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nLeague Menu");
            System.out.println("1. Create");
            System.out.println("2. Delete");
            System.out.println("3. Read");
            System.out.println("4. Back");
            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1": createLeague(conn, scanner); pauseForEnter(scanner); break;
                case "2": deleteById(conn, scanner, "League", "league_id"); pauseForEnter(scanner); break;
                case "3": readLeagues(conn, scanner); pauseForEnter(scanner); break;
                case "4": return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void matchMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nMatch Menu");
            System.out.println("1. Create");
            System.out.println("2. Update");
            System.out.println("3. Delete");
            System.out.println("4. Read");
            System.out.println("5. Back");
            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1": createMatch(conn, scanner); pauseForEnter(scanner); break;
                case "2": updateMatch(conn, scanner); pauseForEnter(scanner); break;
                case "3": deleteById(conn, scanner, "`Match`", "match_id"); pauseForEnter(scanner); break;
                case "4": readMatches(conn, scanner); pauseForEnter(scanner); break;
                case "5": return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    
    private static void marketValueMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nMarket Value Menu");
            System.out.println("1. Create");
            System.out.println("2. Delete");
            System.out.println("3. Read");
            System.out.println("4. Back");
            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1": createMarketValue(conn, scanner); pauseForEnter(scanner); break;
                case "2": deleteMarketValue(conn, scanner); pauseForEnter(scanner); break;
                case "3": readMarketValues(conn, scanner); pauseForEnter(scanner); break;
                case "4": return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void matchPerformanceMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nMatch Performance Menu");
            System.out.println("1. Create");
            System.out.println("2. Delete");
            System.out.println("3. Read");
            System.out.println("4. Back");
            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1": createMatchPerformance(conn, scanner); pauseForEnter(scanner); break;
                case "2": deleteMatchPerformance(conn, scanner); pauseForEnter(scanner); break;
                case "3": readMatchPerformances(conn, scanner); pauseForEnter(scanner); break;
                case "4": return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void transferMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nTransfer Menu");
            System.out.println("1. Create");
            System.out.println("2. Delete");
            System.out.println("3. Read");
            System.out.println("4. Back");
            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1": recordTransfer(conn, scanner); pauseForEnter(scanner); break;
                case "2": deleteById(conn, scanner, "Transfer", "transfer_id"); pauseForEnter(scanner); break;
                case "3": readTransfers(conn, scanner); pauseForEnter(scanner); break;
                case "4": return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void userMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nUser Menu (Read Only)");
            System.out.println("1.  View all players in a league");
            System.out.println("2.  View statistics for a player");
            System.out.println("3.  View top scorers for a season");
            System.out.println("4.  View teams in a league");
            System.out.println("5.  View match results");
            System.out.println("6.  View coach info");
            System.out.println("7.  View stadium info");
            System.out.println("8.  View player market value");
            System.out.println("9.  View player transfer history");
            System.out.println("10. Exit");

            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1":  viewPlayersInLeague(conn, scanner);    pauseForEnter(scanner); break;
                case "2":  viewPlayerStats(conn, scanner);        pauseForEnter(scanner); break;
                case "3":  viewTopScorers(conn, scanner);         pauseForEnter(scanner); break;
                case "4":  viewTeamsInLeague(conn, scanner);      pauseForEnter(scanner); break;
                case "5":  viewMatchResults(conn, scanner);       pauseForEnter(scanner); break;
                case "6":  viewCoachStats(conn, scanner);         pauseForEnter(scanner); break;
                case "7":  viewStadiumStats(conn, scanner);       pauseForEnter(scanner); break;
                case "8":  viewPlayerMarketValue(conn, scanner);  pauseForEnter(scanner); break;
                case "9":  viewPlayerTransfers(conn, scanner);    pauseForEnter(scanner); break;
                case "10": System.out.println("Goodbye."); return;
                default:   System.out.println("Invalid option.");
            }
        }
    }

    // ============================================================
    // PLAYER CRUD
    // ============================================================
    private static void createPlayer(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO Player (first_name, last_name, dob, place_of_birth, height_cm, preferred_foot, position_id, country_abbr, club_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "first_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            // FIX 1: use promptOptional for nullable fields
            setNullableDate(stmt, 3, promptOptional(scanner, "dob (YYYY-MM-DD or blank)"));
            setNullableString(stmt, 4, promptOptional(scanner, "place_of_birth (blank ok)"));
            setNullableDecimal(stmt, 5, promptOptional(scanner, "height_cm (blank ok)"));
            setNullableString(stmt, 6, promptOptional(scanner, "preferred_foot [Left|Right|Both] (blank ok)"));
            setNullableInt(stmt, 7, promptOptional(scanner, "position_id (blank ok)"));
            setNullableString(stmt, 8, promptOptional(scanner, "country_abbr (3-letter code, blank ok)"));
            setNullableInt(stmt, 9, promptOptional(scanner, "club_id (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void updatePlayer(Connection conn, Scanner scanner) {
        String sql = "UPDATE Player SET first_name=?, last_name=?, dob=?, place_of_birth=?, height_cm=?, preferred_foot=?, position_id=?, country_abbr=?, club_id=? "
                + "WHERE player_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "first_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            setNullableDate(stmt, 3, promptOptional(scanner, "dob (YYYY-MM-DD or blank)"));
            setNullableString(stmt, 4, promptOptional(scanner, "place_of_birth (blank ok)"));
            setNullableDecimal(stmt, 5, promptOptional(scanner, "height_cm (blank ok)"));
            setNullableString(stmt, 6, promptOptional(scanner, "preferred_foot [Left|Right|Both] (blank ok)"));
            setNullableInt(stmt, 7, promptOptional(scanner, "position_id (blank ok)"));
            setNullableString(stmt, 8, promptOptional(scanner, "country_abbr (3-letter code, blank ok)"));
            setNullableInt(stmt, 9, promptOptional(scanner, "club_id (blank ok)"));
            setRequiredInt(stmt, 10, prompt(scanner, "player_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readPlayers(Connection conn, Scanner scanner) {
        String id = promptOptional(scanner, "player_id (blank for all)");
        String sql = (id == null || id.isBlank())
                ? "SELECT player_id, first_name, last_name, dob, fn_player_age(dob) AS age, place_of_birth, height_cm, preferred_foot, position_id, country_abbr, club_id FROM Player"
                : "SELECT player_id, first_name, last_name, dob, fn_player_age(dob) AS age, place_of_birth, height_cm, preferred_foot, position_id, country_abbr, club_id FROM Player WHERE player_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id != null && !id.isBlank()) {
                setRequiredInt(stmt, 1, id);
            }
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // CLUB CRUD (defined but kept for internal use)
    // ============================================================
    private static void createClub(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO Club (club_name, country_abbr, league_id, stadium_id, coach_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "club_name"));
            stmt.setString(2, prompt(scanner, "country_abbr"));
            setNullableInt(stmt, 3, promptOptional(scanner, "league_id (blank ok)"));
            setNullableInt(stmt, 4, promptOptional(scanner, "stadium_id (blank ok)"));
            setNullableInt(stmt, 5, promptOptional(scanner, "coach_id (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void updateClub(Connection conn, Scanner scanner) {
        String sql = "UPDATE Club SET club_name=?, country_abbr=?, league_id=?, stadium_id=?, coach_id=? WHERE club_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "club_name"));
            stmt.setString(2, prompt(scanner, "country_abbr"));
            setNullableInt(stmt, 3, promptOptional(scanner, "league_id (blank ok)"));
            setNullableInt(stmt, 4, promptOptional(scanner, "stadium_id (blank ok)"));
            setNullableInt(stmt, 5, promptOptional(scanner, "coach_id (blank ok)"));
            setRequiredInt(stmt, 6, prompt(scanner, "club_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readClubs(Connection conn, Scanner scanner) {
        String id = promptOptional(scanner, "club_id (blank for all)");
        String sql = (id == null || id.isBlank())
                ? "SELECT club_id, club_name, country_abbr, league_id, stadium_id, coach_id FROM Club"
                : "SELECT club_id, club_name, country_abbr, league_id, stadium_id, coach_id FROM Club WHERE club_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id != null && !id.isBlank()) {
                setRequiredInt(stmt, 1, id);
            }
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // LEAGUE CRUD
    // ============================================================
    private static void createLeague(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO League (league_name, season_name) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "league_name"));
            stmt.setString(2, prompt(scanner, "season_name"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readLeagues(Connection conn, Scanner scanner) {
        String id = promptOptional(scanner, "league_id (blank for all)");
        String sql = (id == null || id.isBlank())
                ? "SELECT league_id, league_name, season_name FROM League"
                : "SELECT league_id, league_name, season_name FROM League WHERE league_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id != null && !id.isBlank()) {
                setRequiredInt(stmt, 1, id);
            }
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // MATCH CRUD
    // ============================================================
    private static void createMatch(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO `Match` (home_team_id, away_team_id, match_date, home_score, away_score) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "home_team_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "away_team_id"));
            setRequiredDate(stmt, 3, prompt(scanner, "match_date (YYYY-MM-DD)"));
            setNullableInt(stmt, 4, promptOptional(scanner, "home_score (blank ok)"));
            setNullableInt(stmt, 5, promptOptional(scanner, "away_score (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void updateMatch(Connection conn, Scanner scanner) {
        String sql = "UPDATE `Match` SET home_team_id=?, away_team_id=?, match_date=?, home_score=?, away_score=? WHERE match_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "home_team_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "away_team_id"));
            setRequiredDate(stmt, 3, prompt(scanner, "match_date (YYYY-MM-DD)"));
            setNullableInt(stmt, 4, promptOptional(scanner, "home_score (blank ok)"));
            setNullableInt(stmt, 5, promptOptional(scanner, "away_score (blank ok)"));
            setRequiredInt(stmt, 6, prompt(scanner, "match_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readMatches(Connection conn, Scanner scanner) {
        String id = promptOptional(scanner, "match_id (blank for all)");
        String sql = (id == null || id.isBlank())
                ? "SELECT match_id, home_team_id, away_team_id, match_date, home_score, away_score, home_result, away_result FROM `Match`"
                : "SELECT match_id, home_team_id, away_team_id, match_date, home_score, away_score, home_result, away_result FROM `Match` WHERE match_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id != null && !id.isBlank()) {
                setRequiredInt(stmt, 1, id);
            }
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // MARKET VALUE CRUD
    // ============================================================
    private static void createMarketValue(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO MarketValue (player_id, market_value_date, market_value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "player_id"));
            setRequiredDate(stmt, 2, prompt(scanner, "market_value_date (YYYY-MM-DD)"));
            setRequiredDecimal(stmt, 3, prompt(scanner, "market_value"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void deleteMarketValue(Connection conn, Scanner scanner) {
        String sql = "DELETE FROM MarketValue WHERE player_id=? AND market_value_date=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "player_id"));
            setRequiredDate(stmt, 2, prompt(scanner, "market_value_date (YYYY-MM-DD)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readMarketValues(Connection conn, Scanner scanner) {
        String playerId = promptOptional(scanner, "player_id (blank for all)");
        String sql = (playerId == null || playerId.isBlank())
                ? "SELECT player_id, market_value_date, market_value FROM MarketValue ORDER BY market_value_date"
                : "SELECT player_id, market_value_date, market_value FROM MarketValue WHERE player_id = ? ORDER BY market_value_date";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (playerId != null && !playerId.isBlank()) {
                setRequiredInt(stmt, 1, playerId);
            }
            // Collect data for chart too
            ResultSet rs = stmt.executeQuery();
            List<String> dates = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            List<String[]> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            int[] colWidths = new int[cols];
            for (int i = 0; i < cols; i++) colWidths[i] = meta.getColumnLabel(i + 1).length();
            while (rs.next()) {
                String[] row = new String[cols];
                for (int i = 0; i < cols; i++) {
                    String v = rs.getString(i + 1);
                    row[i] = v == null ? "NULL" : v;
                    if (row[i].length() > colWidths[i]) colWidths[i] = row[i].length();
                }
                rows.add(row);
                // Collect date (col index 1) and value (col index 2) for chart
                try {
                    dates.add(rs.getString(2) != null ? rs.getString(2) : row[1]);
                    String valStr = row[2].replace(",", "");
                    values.add(Double.parseDouble(valStr));
                } catch (Exception ignored) {}
            }
            // Print table
            printCollectedRows(rows, colWidths, meta);
            // FIX 3: Show chart if we have data for a specific player
            if (!values.isEmpty() && playerId != null && !playerId.isBlank()) {
                showMarketValueChart(dates, values, "Player ID " + playerId);
            }
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // MATCH PERFORMANCE CRUD
    // ============================================================
    private static void createMatchPerformance(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO MatchPerformance (match_id, player_id, play_time, performance_rating) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "match_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "player_id"));
            setNullableInt(stmt, 3, promptOptional(scanner, "play_time (minutes, blank ok)"));
            setNullableDecimal(stmt, 4, promptOptional(scanner, "performance_rating (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void deleteMatchPerformance(Connection conn, Scanner scanner) {
        String sql = "DELETE FROM MatchPerformance WHERE match_id=? AND player_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "match_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "player_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readMatchPerformances(Connection conn, Scanner scanner) {
        String matchId = promptOptional(scanner, "match_id (blank for all)");
        String playerId = promptOptional(scanner, "player_id (blank for all)");
        StringBuilder sql = new StringBuilder("SELECT match_id, player_id, play_time, performance_rating FROM MatchPerformance");
        List<String> conditions = new ArrayList<>();
        if (matchId != null && !matchId.isBlank()) conditions.add("match_id = ?");
        if (playerId != null && !playerId.isBlank()) conditions.add("player_id = ?");
        if (!conditions.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", conditions));
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (matchId != null && !matchId.isBlank()) setRequiredInt(stmt, idx++, matchId);
            if (playerId != null && !playerId.isBlank()) setRequiredInt(stmt, idx, playerId);
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // COACH CRUD (defined, available for future menu wiring)
    // ============================================================
    private static void createCoach(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO Coach (first_name, last_name, dob, nationality, club_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "first_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            setNullableDate(stmt, 3, promptOptional(scanner, "dob (YYYY-MM-DD or blank)"));
            setNullableString(stmt, 4, promptOptional(scanner, "nationality (3-letter code, blank ok)"));
            setNullableInt(stmt, 5, promptOptional(scanner, "club_id (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void updateCoach(Connection conn, Scanner scanner) {
        String sql = "UPDATE Coach SET first_name=?, last_name=?, dob=?, nationality=?, club_id=? WHERE coach_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "first_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            setNullableDate(stmt, 3, promptOptional(scanner, "dob (YYYY-MM-DD or blank)"));
            setNullableString(stmt, 4, promptOptional(scanner, "nationality (3-letter code, blank ok)"));
            setNullableInt(stmt, 5, promptOptional(scanner, "club_id (blank ok)"));
            setRequiredInt(stmt, 6, prompt(scanner, "coach_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readCoaches(Connection conn, Scanner scanner) {
        String id = promptOptional(scanner, "coach_id (blank for all)");
        String sql = (id == null || id.isBlank())
                ? "SELECT coach_id, first_name, last_name, dob, nationality, club_id FROM Coach"
                : "SELECT coach_id, first_name, last_name, dob, nationality, club_id FROM Coach WHERE coach_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id != null && !id.isBlank()) setRequiredInt(stmt, 1, id);
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // SEASON PERFORMANCE CRUD (defined, available for future menu wiring)
    // ============================================================
    private static void createSeasonPerformance(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO SeasonPerformance (player_id, league_id, appearance_count, goal_count, assist_count, play_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "player_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "league_id"));
            setNullableInt(stmt, 3, promptOptional(scanner, "appearance_count (blank ok)"));
            setNullableInt(stmt, 4, promptOptional(scanner, "goal_count (blank ok)"));
            setNullableInt(stmt, 5, promptOptional(scanner, "assist_count (blank ok)"));
            setNullableInt(stmt, 6, promptOptional(scanner, "play_time in minutes (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void updateSeasonPerformance(Connection conn, Scanner scanner) {
        String sql = "UPDATE SeasonPerformance SET appearance_count=?, goal_count=?, assist_count=?, play_time=? WHERE player_id=? AND league_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setNullableInt(stmt, 1, promptOptional(scanner, "appearance_count (blank ok)"));
            setNullableInt(stmt, 2, promptOptional(scanner, "goal_count (blank ok)"));
            setNullableInt(stmt, 3, promptOptional(scanner, "assist_count (blank ok)"));
            setNullableInt(stmt, 4, promptOptional(scanner, "play_time in minutes (blank ok)"));
            setRequiredInt(stmt, 5, prompt(scanner, "player_id"));
            setRequiredInt(stmt, 6, prompt(scanner, "league_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void deleteSeasonPerformance(Connection conn, Scanner scanner) {
        String sql = "DELETE FROM SeasonPerformance WHERE player_id=? AND league_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "player_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "league_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // TRANSFER CRUD
    // ============================================================
    private static void readTransfers(Connection conn, Scanner scanner) {
        String id = promptOptional(scanner, "player_id (blank for all)");
        String sql = (id == null || id.isBlank())
                ? "SELECT transfer_id, player_id, old_club_id, new_club_id, transfer_date, transfer_fee FROM Transfer"
                : "SELECT transfer_id, player_id, old_club_id, new_club_id, transfer_date, transfer_fee FROM Transfer WHERE player_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id != null && !id.isBlank()) setRequiredInt(stmt, 1, id);
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void recordTransfer(Connection conn, Scanner scanner) {
        String sql = "CALL sp_record_transfer(?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "player_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "new_club_id"));
            setRequiredDate(stmt, 3, prompt(scanner, "transfer_date (YYYY-MM-DD)"));
            setNullableDecimal(stmt, 4, promptOptional(scanner, "transfer_fee (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void deleteById(Connection conn, Scanner scanner, String table, String idColumn) {
        String sql = "DELETE FROM " + table + " WHERE " + idColumn + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, idColumn));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    // ============================================================
    // USER VIEW METHODS
    // ============================================================
    private static void viewPlayersInLeague(Connection conn, Scanner scanner) {
        String listSql = "SELECT l.league_name, l.season_name, COUNT(DISTINCT sp.player_id) AS player_count " +
                "FROM League l JOIN SeasonPerformance sp ON l.league_id = sp.league_id " +
                "GROUP BY l.league_id, l.league_name, l.season_name ORDER BY player_count DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Leagues (sample):");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_players_in_league(?, ?)")) {
            stmt.setString(1, prompt(scanner, "league_name (e.g. Premier League)"));
            stmt.setString(2, prompt(scanner, "season_name (e.g. 22/23)"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewPlayerStats(Connection conn, Scanner scanner) {
        String listSql = "SELECT first_name, last_name FROM Player ORDER BY last_name LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nSample Players:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        String firstName = prompt(scanner, "\nfirst_name");
        String lastName = prompt(scanner, "last_name");
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_player_stats(?, ?)")) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) header.append(" | ");
                header.append(meta.getColumnLabel(i));
            }
            int rowCount = 0;
            StringBuilder rows = new StringBuilder();
            while (rs.next()) {
                rowCount++;
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) row.append(" | ");
                    String value = rs.getString(i);
                    row.append(value == null ? "NULL" : value);
                }
                rows.append(row).append("\n");
            }
            if (rowCount > 0) {
                System.out.println(header);
                System.out.print(rows);
            } else {
                String fallbackSql = "SELECT player_id, first_name, last_name, dob, place_of_birth, height_cm, preferred_foot FROM Player WHERE first_name = ? AND last_name = ?";
                try (PreparedStatement fallback = conn.prepareStatement(fallbackSql)) {
                    fallback.setString(1, firstName);
                    fallback.setString(2, lastName);
                    executeQuery(fallback);
                    System.out.println("Note: Player found but has no season performance data.");
                }
            }
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewTopScorers(Connection conn, Scanner scanner) {
        String listSql = "SELECT l.league_name, l.season_name, COUNT(DISTINCT sp.player_id) AS player_count FROM League l JOIN SeasonPerformance sp ON l.league_id = sp.league_id GROUP BY l.league_id, l.league_name, l.season_name ORDER BY player_count DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Leagues (sample):");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_top_scorers(?, ?, ?)")) {
            stmt.setString(1, prompt(scanner, "\nleague_name"));
            stmt.setString(2, prompt(scanner, "season_name (e.g. 22/23)"));
            setRequiredInt(stmt, 3, prompt(scanner, "how many results (e.g. 10)"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewTeamsInLeague(Connection conn, Scanner scanner) {
        String listSql = "SELECT l.league_name, l.season_name, COUNT(DISTINCT sp.player_id) AS player_count FROM League l JOIN SeasonPerformance sp ON l.league_id = sp.league_id GROUP BY l.league_id, l.league_name, l.season_name ORDER BY player_count DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Leagues (sample):");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_teams_in_league(?, ?)")) {
            stmt.setString(1, prompt(scanner, "\nleague_name"));
            stmt.setString(2, prompt(scanner, "season_name (e.g. 22/23)"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewMatchResults(Connection conn, Scanner scanner) {
        String listSql = "SELECT DISTINCT l.league_name, l.season_name, COUNT(m.match_id) AS match_count FROM League l JOIN `Match` m ON l.league_id = m.league_id GROUP BY l.league_id, l.league_name, l.season_name ORDER BY match_count DESC";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nLeagues with match data:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_match_results(?, ?)")) {
            stmt.setString(1, prompt(scanner, "\nleague_name"));
            stmt.setString(2, prompt(scanner, "season_name (e.g. 25/26)"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewCoachStats(Connection conn, Scanner scanner) {
        String listSql = "SELECT first_name, last_name FROM Coach ORDER BY last_name";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Coaches:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_coach_stats(?, ?)")) {
            stmt.setString(1, prompt(scanner, "\nfirst_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewStadiumStats(Connection conn, Scanner scanner) {
        String listSql = "SELECT stadium_name, city FROM Stadium ORDER BY stadium_name";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Stadiums:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_stadium_stats(?)")) {
            stmt.setString(1, prompt(scanner, "\nstadium_name"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

   
    private static void viewPlayerMarketValue(Connection conn, Scanner scanner) {
        String listSql = "SELECT p.first_name, p.last_name FROM Player p JOIN MarketValue mv ON p.player_id = mv.player_id GROUP BY p.player_id ORDER BY COUNT(mv.market_value_date) DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nPlayers with market value data (top 20):");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        String firstName = prompt(scanner, "\nfirst_name");
        String lastName = prompt(scanner, "last_name");
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_player_market_value(?, ?)")) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            int[] colWidths = new int[cols];
            for (int i = 0; i < cols; i++) colWidths[i] = meta.getColumnLabel(i + 1).length();
            List<String[]> rows = new ArrayList<>();
            List<String> dates = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            while (rs.next()) {
                String[] row = new String[cols];
                for (int i = 0; i < cols; i++) {
                    String v = rs.getString(i + 1);
                    row[i] = v == null ? "NULL" : v;
                    if (row[i].length() > colWidths[i]) colWidths[i] = row[i].length();
                }
                rows.add(row);
                try {
                    dates.add(row[2]); // market_value_date column
                    values.add(Double.parseDouble(row[3].replace(",", ""))); // market_value column
                } catch (Exception ignored) {}
            }
            printCollectedRows(rows, colWidths, meta);
            if (!values.isEmpty()) {
                String playerName = firstName + " " + lastName;
                showMarketValueChart(dates, values, playerName);
            }
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewPlayerTransfers(Connection conn, Scanner scanner) {
        String listSql = "SELECT p.first_name, p.last_name, COUNT(t.transfer_id) AS transfer_count FROM Player p JOIN Transfer t ON p.player_id = t.player_id GROUP BY p.player_id ORDER BY transfer_count DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nPlayers with most transfers:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        String firstName = prompt(scanner, "\nfirst_name");
        String lastName = prompt(scanner, "last_name");
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_player_transfers(?, ?)")) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) header.append(" | ");
                header.append(meta.getColumnLabel(i));
            }
            int rowCount = 0;
            StringBuilder rows = new StringBuilder();
            while (rs.next()) {
                rowCount++;
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) row.append(" | ");
                    String value = rs.getString(i);
                    row.append(value == null ? "NULL" : value);
                }
                rows.append(row).append("\n");
            }
            if (rowCount > 0) {
                System.out.println(header);
                System.out.print(rows);
            } else {
                String fallbackSql = "SELECT player_id, first_name, last_name, dob, place_of_birth, height_cm, preferred_foot FROM Player WHERE first_name = ? AND last_name = ?";
                try (PreparedStatement fallback = conn.prepareStatement(fallbackSql)) {
                    fallback.setString(1, firstName);
                    fallback.setString(2, lastName);
                    executeQuery(fallback);
                    System.out.println("Note: Player found but has no transfer data.");
                }
            }
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    
    private static void showMarketValueChart(List<String> dates, List<Double> values, String playerName) {
        if (dates.size() < 2) {
            System.out.println("\n[Chart] Not enough data points to draw a chart (need at least 2).");
            return;
        }

        // FIX: reverse so oldest date is on the left and newest on the right
        java.util.Collections.reverse(dates);
        java.util.Collections.reverse(values);

        int W = 900, H = 520;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(18, 18, 30));
        g.fillRect(0, 0, W, H);

        // Extra left padding to accommodate rotated y-axis title
        int padL = 110, padR = 40, padT = 60, padB = 80;
        int chartW = W - padL - padR;
        int chartH = H - padT - padB;

        // Chart title
        g.setColor(new Color(220, 220, 255));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String title = "Market Value Over Time — " + playerName;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (W - fm.stringWidth(title)) / 2, 35);

        // Min/Max values
        double minVal = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxVal = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (maxVal == minVal) maxVal = minVal + 1;
        int n = values.size();

        // Grid lines + y-axis tick labels
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            int y = padT + chartH - (int) ((double) i / gridLines * chartH);
            g.setColor(new Color(50, 50, 80));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
            g.drawLine(padL, y, padL + chartW, y);
            double labelVal = minVal + (maxVal - minVal) * i / gridLines;
            g.setColor(new Color(140, 140, 180));
            g.setStroke(new BasicStroke(1f));
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            String label = formatValue(labelVal);
            g.drawString(label, padL - g.getFontMetrics().stringWidth(label) - 8, y + 4);
        }

        // Y-axis title — rotated 90° counter-clockwise
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(200, 200, 240));
        String yTitle = "Million Euros";
        FontMetrics yFm = g.getFontMetrics();
        int yTitleX = 18;
        int yTitleY = padT + chartH / 2 + yFm.stringWidth(yTitle) / 2;
        java.awt.geom.AffineTransform oldTransform = g.getTransform();
        g.translate(yTitleX, yTitleY);
        g.rotate(-Math.PI / 2);
        g.drawString(yTitle, 0, 0);
        g.setTransform(oldTransform);

        // X-axis title — centered below date labels
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(200, 200, 240));
        String xTitle = "Date";
        FontMetrics xFm = g.getFontMetrics();
        g.drawString(xTitle, padL + chartW / 2 - xFm.stringWidth(xTitle) / 2, H - 10);

        // Compute pixel coords
        int[] px = new int[n];
        int[] py = new int[n];
        for (int i = 0; i < n; i++) {
            px[i] = padL + (int) ((double) i / (n - 1) * chartW);
            py[i] = padT + chartH - (int) ((values.get(i) - minVal) / (maxVal - minVal) * chartH);
        }

        // Gradient fill under curve using GeneralPath
        GeneralPath fillPath = new GeneralPath();
        fillPath.moveTo(px[0], padT + chartH);
        fillPath.lineTo(px[0], py[0]);
        for (int i = 1; i < n; i++) {
            float cpx1 = px[i - 1] + (px[i] - px[i - 1]) / 3f;
            float cpy1 = py[i - 1];
            float cpx2 = px[i] - (px[i] - px[i - 1]) / 3f;
            float cpy2 = py[i];
            fillPath.curveTo(cpx1, cpy1, cpx2, cpy2, px[i], py[i]);
        }
        fillPath.lineTo(px[n - 1], padT + chartH);
        fillPath.closePath();
        GradientPaint gp = new GradientPaint(0, padT, new Color(99, 102, 241, 120), 0, padT + chartH, new Color(99, 102, 241, 0));
        g.setPaint(gp);
        g.fill(fillPath);

        // Smoothed line
        g.setColor(new Color(129, 140, 248));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GeneralPath linePath = new GeneralPath();
        linePath.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) {
            float cpx1 = px[i - 1] + (px[i] - px[i - 1]) / 3f;
            float cpy1 = py[i - 1];
            float cpx2 = px[i] - (px[i] - px[i - 1]) / 3f;
            float cpy2 = py[i];
            linePath.curveTo(cpx1, cpy1, cpx2, cpy2, px[i], py[i]);
        }
        g.draw(linePath);

        // Data points
        g.setColor(new Color(199, 210, 254));
        for (int i = 0; i < n; i++) {
            g.fillOval(px[i] - 4, py[i] - 4, 8, 8);
        }

        // X-axis date labels (show up to 8 evenly spaced)
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(140, 140, 180));
        int labelStep = Math.max(1, n / 8);
        for (int i = 0; i < n; i += labelStep) {
            String d = dates.get(i);
            if (d.length() > 10) d = d.substring(0, 10);
            int lw = g.getFontMetrics().stringWidth(d);
            g.drawString(d, px[i] - lw / 2, padT + chartH + 20);
        }

        // Axes
        g.setColor(new Color(80, 80, 120));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(padL, padT, padL, padT + chartH);
        g.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

        g.dispose();

        // Try to show in a popup window
        boolean headless = GraphicsEnvironment.isHeadless();
        if (!headless) {
            try {
                JFrame frame = new JFrame("Market Value Chart — " + playerName);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JLabel label = new JLabel(new ImageIcon(img));
                frame.getContentPane().add(label);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                System.out.println("\n[Chart] A chart window has opened. Close it when done.");
                return;
            } catch (Exception e) {
                // Fall through to file save
            }
        }

        // Fallback: save to PNG in current directory
        try {
            String safeName = playerName.replaceAll("[^a-zA-Z0-9_\\- ]", "").replace(" ", "_");
            String filename = "market_value_" + safeName + ".png";
            File outFile = new File(filename);
            ImageIO.write(img, "PNG", outFile);
            System.out.println("\n[Chart] Chart saved to: " + outFile.getAbsolutePath());
            System.out.println("        Please open this file to view the market value trend chart.");
        } catch (IOException e) {
            System.out.println("\n[Chart] Could not save chart: " + e.getMessage());
        }
    }

    private static String formatValue(double val) {
        if (val >= 1_000_000) return String.format("%.1fM", val / 1_000_000);
        if (val >= 1_000) return String.format("%.0fK", val / 1_000);
        return String.format("%.0f", val);
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private static void executeUpdate(PreparedStatement stmt) throws SQLException {
        int updated = stmt.executeUpdate();
        System.out.println("Rows affected: " + updated);
    }

    private static void executeQuery(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            printResultSet(rs);
        }
    }

    private static void setNullableString(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) stmt.setNull(idx, Types.VARCHAR);
        else stmt.setString(idx, value);
    }

    private static void setNullableInt(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(idx, Types.INTEGER);
        } else {
            try { stmt.setInt(idx, Integer.parseInt(value)); }
            catch (NumberFormatException e) { throw new SQLException("Invalid integer value: " + value); }
        }
    }

    private static void setRequiredInt(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) throw new SQLException("Required integer value is missing.");
        try { stmt.setInt(idx, Integer.parseInt(value)); }
        catch (NumberFormatException e) { throw new SQLException("Invalid integer value: " + value); }
    }

    private static void setNullableDecimal(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(idx, Types.DECIMAL);
        } else {
            try { stmt.setBigDecimal(idx, new java.math.BigDecimal(value)); }
            catch (NumberFormatException e) { throw new SQLException("Invalid decimal value: " + value); }
        }
    }

    private static void setRequiredDecimal(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) throw new SQLException("Required decimal value is missing.");
        try { stmt.setBigDecimal(idx, new java.math.BigDecimal(value)); }
        catch (NumberFormatException e) { throw new SQLException("Invalid decimal value: " + value); }
    }

    private static void setNullableDate(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(idx, Types.DATE);
        } else {
            try { stmt.setDate(idx, java.sql.Date.valueOf(value)); }
            catch (IllegalArgumentException e) { throw new SQLException("Invalid date value (expected YYYY-MM-DD): " + value); }
        }
    }

    private static void setRequiredDate(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) throw new SQLException("Required date value is missing.");
        try { stmt.setDate(idx, java.sql.Date.valueOf(value)); }
        catch (IllegalArgumentException e) { throw new SQLException("Invalid date value (expected YYYY-MM-DD): " + value); }
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        List<String[]> allRows = new ArrayList<>();
        int[] colWidths = new int[cols];
        for (int i = 0; i < cols; i++) colWidths[i] = meta.getColumnLabel(i + 1).length();
        while (rs.next()) {
            String[] row = new String[cols];
            for (int i = 0; i < cols; i++) {
                String value = rs.getString(i + 1);
                row[i] = value == null ? "NULL" : value;
                if (row[i].length() > colWidths[i]) colWidths[i] = row[i].length();
            }
            allRows.add(row);
        }
        printCollectedRows(allRows, colWidths, meta);
    }

    // Shared pretty-print method used by both printResultSet and readMarketValues
    private static void printCollectedRows(List<String[]> allRows, int[] colWidths, ResultSetMetaData meta) throws SQLException {
        if (allRows.isEmpty()) {
            System.out.println("No data found for the given input. The input may be incorrect.");
            return;
        }
        int cols = colWidths.length;
        StringBuilder separator = new StringBuilder("+");
        for (int w : colWidths) separator.append("-".repeat(w + 2)).append("+");

        System.out.println(separator);
        StringBuilder header = new StringBuilder("|");
        for (int i = 0; i < cols; i++) {
            String label = meta.getColumnLabel(i + 1);
            header.append(" ").append(label).append(" ".repeat(colWidths[i] - label.length())).append(" |");
        }
        System.out.println(header);
        System.out.println(separator);
        for (String[] row : allRows) {
            StringBuilder line = new StringBuilder("|");
            for (int i = 0; i < cols; i++) {
                line.append(" ").append(row[i]).append(" ".repeat(colWidths[i] - row[i].length())).append(" |");
            }
            System.out.println(line);
        }
        System.out.println(separator);
    }

    private static void printSqlError(SQLException e) {
        System.out.println("Database error: " + e.getMessage());
        System.out.println("SQLState: " + e.getSQLState());
    }

    
    private static String prompt(Scanner scanner, String label) {
        String value = "";
        while (value.isBlank()) {
            System.out.print(label + ": ");
            value = scanner.nextLine().trim();
            if (value.isBlank()) {
                System.out.println("Input cannot be empty. Please try again.");
            }
        }
        return value;
    }

    private static String promptOptional(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private static class DbConfig {
        final String host;
        final int port;
        final String user;
        final String password;
        final String database;

        DbConfig(String host, int port, String user, String password, String database) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.database = database;
        }
    }

    private enum Role { ADMIN, USER }

    private static Role parseRole(String roleInput) {
        if (roleInput == null) return null;
        String normalized = roleInput.trim().toLowerCase();
        if ("admin".equals(normalized)) return Role.ADMIN;
        if ("user".equals(normalized)) return Role.USER;
        return null;
    }
}
