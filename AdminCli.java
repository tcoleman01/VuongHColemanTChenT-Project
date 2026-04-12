import java.io.BufferedReader;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AdminCli {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DB = "soccer_analytics_db";

    private static Map<String, String> dotEnv = new HashMap<>();

    public static void main(String[] args) {
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

    private static Role login(Connection conn, Scanner scanner) {
        String adminUser = getConfig("ADMIN_USER", "");
        String adminPass = getConfig("ADMIN_PASS", "");

        System.out.println("\nLogin (type 'q' to quit)");
        String roleInput = prompt(scanner, "Role (admin|user)");

        // =============================================
        // ALLOW USER TO QUIT AT ROLE SELECTION
        // =============================================
        if (roleInput.equalsIgnoreCase("q")) {
            System.out.println("Goodbye.");
            System.exit(0);
        }

        // =============================================
        // VALIDATE ROLE BEFORE ASKING FOR CREDENTIALS
        // =============================================
        Role role = parseRole(roleInput);
        if (role == null) {
            System.out.println("Invalid role. Use admin or user.");
            return null;
        }

        String username = prompt(scanner, "Username");
        String password = prompt(scanner, "Password");

        if (role == Role.ADMIN) {
            // =============================================
            // ADMIN LOGIN - CHECK AGAINST .ENV CREDENTIALS
            // =============================================
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

        // =============================================
        // USER LOGIN - VALIDATE AGAINST DATABASE USER TABLE
        // =============================================
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

    private static void adminMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nAdmin Menu");
            System.out.println("1. Create player");
            System.out.println("2. Update player");
            System.out.println("3. Delete player");
            System.out.println("4. Read players");
            System.out.println("5. Create club");
            System.out.println("6. Update club");
            System.out.println("7. Delete club");
            System.out.println("8. Read clubs");
            System.out.println("9. Create league");
            System.out.println("10. Update league");
            System.out.println("11. Delete league");
            System.out.println("12. Read leagues");
            System.out.println("13. Create match");
            System.out.println("14. Update match");
            System.out.println("15. Delete match");
            System.out.println("16. Read matches");
            System.out.println("17. Create market value");
            System.out.println("18. Update market value");
            System.out.println("19. Delete market value");
            System.out.println("20. Read market values");
            System.out.println("21. Create match performance");
            System.out.println("22. Update match performance");
            System.out.println("23. Delete match performance");
            System.out.println("24. Read match performances");
            System.out.println("25. Record transfer");
            System.out.println("26. Run read-only query");
            System.out.println("27. Create coach");
            System.out.println("28. Update coach");
            System.out.println("29. Delete coach");
            System.out.println("30. Read coaches");
            System.out.println("31. Create season performance");
            System.out.println("32. Update season performance");
            System.out.println("33. Delete season performance");
            System.out.println("34. Read season performances");
            System.out.println("35. Exit");

            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1":
                    createPlayer(conn, scanner);
                    break;
                case "2":
                    updatePlayer(conn, scanner);
                    break;
                case "3":
                    deleteById(conn, scanner, "Player", "player_id");
                    break;
                case "4":
                    readPlayers(conn, scanner);
                    break;
                case "5":
                    createClub(conn, scanner);
                    break;
                case "6":
                    updateClub(conn, scanner);
                    break;
                case "7":
                    deleteById(conn, scanner, "Club", "club_id");
                    break;
                case "8":
                    readClubs(conn, scanner);
                    break;
                case "9":
                    createLeague(conn, scanner);
                    break;
                case "10":
                    updateLeague(conn, scanner);
                    break;
                case "11":
                    deleteById(conn, scanner, "League", "league_id");
                    break;
                case "12":
                    readLeagues(conn, scanner);
                    break;
                case "13":
                    createMatch(conn, scanner);
                    break;
                case "14":
                    updateMatch(conn, scanner);
                    break;
                case "15":
                    deleteById(conn, scanner, "`Match`", "match_id");
                    break;
                case "16":
                    readMatches(conn, scanner);
                    break;
                case "17":
                    createMarketValue(conn, scanner);
                    break;
                case "18":
                    updateMarketValue(conn, scanner);
                    break;
                case "19":
                    deleteMarketValue(conn, scanner);
                    break;
                case "20":
                    readMarketValues(conn, scanner);
                    break;
                case "21":
                    createMatchPerformance(conn, scanner);
                    break;
                case "22":
                    updateMatchPerformance(conn, scanner);
                    break;
                case "23":
                    deleteMatchPerformance(conn, scanner);
                    break;
                case "24":
                    readMatchPerformances(conn, scanner);
                    break;
                case "25":
                    recordTransfer(conn, scanner);
                    break;
                case "26":
                    runReadOnlyQuery(conn, scanner);
                    break;
                case "27":
                    createCoach(conn, scanner);
                    break;
                case "28":
                    updateCoach(conn, scanner);
                    break;
                case "29":
                    deleteById(conn, scanner, "Coach", "coach_id");
                    break;
                case "30":
                    readCoaches(conn, scanner);
                    break;
                case "31":
                    createSeasonPerformance(conn, scanner);
                    break;
                case "32":
                    updateSeasonPerformance(conn, scanner);
                    break;
                case "33":
                    deleteSeasonPerformance(conn, scanner);
                    break;
                case "34":
                    readSeasonPerformances(conn, scanner);
                    break;
                case "35":
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println("Invalid option.");
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
                case "1": viewPlayersInLeague(conn, scanner); break;
                case "2": viewPlayerStats(conn, scanner); break;
                case "3": viewTopScorers(conn, scanner); break;
                case "4": viewTeamsInLeague(conn, scanner); break;
                case "5": viewMatchResults(conn, scanner); break;
                case "6": viewCoachStats(conn, scanner); break;
                case "7": viewStadiumStats(conn, scanner); break;
                case "8": viewPlayerMarketValue(conn, scanner); break;
                case "9": viewPlayerTransfers(conn, scanner); break;
                case "10": System.out.println("Goodbye."); return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private static void createPlayer(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO Player (first_name, last_name, dob, place_of_birth, height_cm, preferred_foot, position_id, country_abbr, club_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "first_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            setNullableDate(stmt, 3, prompt(scanner, "dob (YYYY-MM-DD or blank)"));
            setNullableString(stmt, 4, prompt(scanner, "place_of_birth (blank ok)"));
            setNullableDecimal(stmt, 5, prompt(scanner, "height_cm (blank ok)"));
            setNullableString(stmt, 6, prompt(scanner, "preferred_foot [Left|Right|Both]"));
            setNullableInt(stmt, 7, prompt(scanner, "position_id (blank ok)"));
            setNullableString(stmt, 8, prompt(scanner, "country_abbr (3-letter code, blank ok)"));
            setNullableInt(stmt, 9, prompt(scanner, "club_id (blank ok)"));
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
            setNullableDate(stmt, 3, prompt(scanner, "dob (YYYY-MM-DD or blank)"));
            setNullableString(stmt, 4, prompt(scanner, "place_of_birth (blank ok)"));
            setNullableDecimal(stmt, 5, prompt(scanner, "height_cm (blank ok)"));
            setNullableString(stmt, 6, prompt(scanner, "preferred_foot [Left|Right|Both]"));
            setNullableInt(stmt, 7, prompt(scanner, "position_id (blank ok)"));
            setNullableString(stmt, 8, prompt(scanner, "country_abbr (3-letter code, blank ok)"));
            setNullableInt(stmt, 9, prompt(scanner, "club_id (blank ok)"));
            setRequiredInt(stmt, 10, prompt(scanner, "player_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readPlayers(Connection conn, Scanner scanner) {
        String id = prompt(scanner, "player_id (blank for all)");
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

    private static void createClub(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO Club (club_name, country_abbr, league_id, stadium_id, coach_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "club_name"));
            stmt.setString(2, prompt(scanner, "country_abbr"));
            setNullableInt(stmt, 3, prompt(scanner, "league_id (blank ok)"));
            setNullableInt(stmt, 4, prompt(scanner, "stadium_id (blank ok)"));
            setNullableInt(stmt, 5, prompt(scanner, "coach_id (blank ok)"));
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
            setNullableInt(stmt, 3, prompt(scanner, "league_id (blank ok)"));
            setNullableInt(stmt, 4, prompt(scanner, "stadium_id (blank ok)"));
            setNullableInt(stmt, 5, prompt(scanner, "coach_id (blank ok)"));
            setRequiredInt(stmt, 6, prompt(scanner, "club_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readClubs(Connection conn, Scanner scanner) {
        String id = prompt(scanner, "club_id (blank for all)");
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

    private static void updateLeague(Connection conn, Scanner scanner) {
        String sql = "UPDATE League SET league_name=?, season_name=? WHERE league_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "league_name"));
            stmt.setString(2, prompt(scanner, "season_name"));
            setRequiredInt(stmt, 3, prompt(scanner, "league_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readLeagues(Connection conn, Scanner scanner) {
        String id = prompt(scanner, "league_id (blank for all)");
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

    private static void createMatch(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO `Match` (home_team_id, away_team_id, match_date, home_score, away_score) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "home_team_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "away_team_id"));
            setRequiredDate(stmt, 3, prompt(scanner, "match_date (YYYY-MM-DD)"));
            setNullableInt(stmt, 4, prompt(scanner, "home_score (blank ok)"));
            setNullableInt(stmt, 5, prompt(scanner, "away_score (blank ok)"));
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
            setNullableInt(stmt, 4, prompt(scanner, "home_score (blank ok)"));
            setNullableInt(stmt, 5, prompt(scanner, "away_score (blank ok)"));
            setRequiredInt(stmt, 6, prompt(scanner, "match_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readMatches(Connection conn, Scanner scanner) {
        String id = prompt(scanner, "match_id (blank for all)");
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

    private static void updateMarketValue(Connection conn, Scanner scanner) {
        String sql = "UPDATE MarketValue SET market_value=? WHERE player_id=? AND market_value_date=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredDecimal(stmt, 1, prompt(scanner, "market_value (new)"));
            setRequiredInt(stmt, 2, prompt(scanner, "player_id"));
            setRequiredDate(stmt, 3, prompt(scanner, "market_value_date (YYYY-MM-DD)"));
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
        String playerId = prompt(scanner, "player_id (blank for all)");
        String sql = (playerId == null || playerId.isBlank())
            ? "SELECT player_id, market_value_date, market_value FROM MarketValue"
            : "SELECT player_id, market_value_date, market_value FROM MarketValue WHERE player_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (playerId != null && !playerId.isBlank()) {
                setRequiredInt(stmt, 1, playerId);
            }
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void createMatchPerformance(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO MatchPerformance (match_id, player_id, play_time, performance_rating) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "match_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "player_id"));
            setNullableInt(stmt, 3, prompt(scanner, "play_time (minutes, blank ok)"));
            setNullableDecimal(stmt, 4, prompt(scanner, "performance_rating (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void updateMatchPerformance(Connection conn, Scanner scanner) {
        String sql = "UPDATE MatchPerformance SET play_time=?, performance_rating=? WHERE match_id=? AND player_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setNullableInt(stmt, 1, prompt(scanner, "play_time (minutes, blank ok)"));
            setNullableDecimal(stmt, 2, prompt(scanner, "performance_rating (blank ok)"));
            setRequiredInt(stmt, 3, prompt(scanner, "match_id"));
            setRequiredInt(stmt, 4, prompt(scanner, "player_id"));
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
        String matchId = prompt(scanner, "match_id (blank for all)");
        String playerId = prompt(scanner, "player_id (blank for all)");
        StringBuilder sql = new StringBuilder("SELECT match_id, player_id, play_time, performance_rating FROM MatchPerformance");
        List<String> conditions = new ArrayList<>();
        if (matchId != null && !matchId.isBlank()) {
            conditions.add("match_id = ?");
        }
        if (playerId != null && !playerId.isBlank()) {
            conditions.add("player_id = ?");
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (matchId != null && !matchId.isBlank()) {
                setRequiredInt(stmt, idx++, matchId);
            }
            if (playerId != null && !playerId.isBlank()) {
                setRequiredInt(stmt, idx, playerId);
            }
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void createCoach(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO Coach (first_name, last_name, dob, nationality, club_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prompt(scanner, "first_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            setNullableDate(stmt, 3, prompt(scanner, "dob (YYYY-MM-DD or blank)"));
            setNullableString(stmt, 4, prompt(scanner, "nationality (3-letter code, blank ok)"));
            setNullableInt(stmt, 5, prompt(scanner, "club_id (blank ok)"));
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
            setNullableDate(stmt, 3, prompt(scanner, "dob (YYYY-MM-DD or blank)"));
            setNullableString(stmt, 4, prompt(scanner, "nationality (3-letter code, blank ok)"));
            setNullableInt(stmt, 5, prompt(scanner, "club_id (blank ok)"));
            setRequiredInt(stmt, 6, prompt(scanner, "coach_id"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void readCoaches(Connection conn, Scanner scanner) {
        String id = prompt(scanner, "coach_id (blank for all)");
        String sql = (id == null || id.isBlank())
            ? "SELECT coach_id, first_name, last_name, dob, nationality, club_id FROM Coach"
            : "SELECT coach_id, first_name, last_name, dob, nationality, club_id FROM Coach WHERE coach_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id != null && !id.isBlank()) {
                setRequiredInt(stmt, 1, id);
            }
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void createSeasonPerformance(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO SeasonPerformance (player_id, league_id, appearance_count, goal_count, assist_count, play_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setRequiredInt(stmt, 1, prompt(scanner, "player_id"));
            setRequiredInt(stmt, 2, prompt(scanner, "league_id"));
            setNullableInt(stmt, 3, prompt(scanner, "appearance_count (blank ok)"));
            setNullableInt(stmt, 4, prompt(scanner, "goal_count (blank ok)"));
            setNullableInt(stmt, 5, prompt(scanner, "assist_count (blank ok)"));
            setNullableInt(stmt, 6, prompt(scanner, "play_time in minutes (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void updateSeasonPerformance(Connection conn, Scanner scanner) {
        String sql = "UPDATE SeasonPerformance SET appearance_count=?, goal_count=?, assist_count=?, play_time=? WHERE player_id=? AND league_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setNullableInt(stmt, 1, prompt(scanner, "appearance_count (blank ok)"));
            setNullableInt(stmt, 2, prompt(scanner, "goal_count (blank ok)"));
            setNullableInt(stmt, 3, prompt(scanner, "assist_count (blank ok)"));
            setNullableInt(stmt, 4, prompt(scanner, "play_time in minutes (blank ok)"));
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

    private static void readSeasonPerformances(Connection conn, Scanner scanner) {
        String playerId = prompt(scanner, "player_id (blank for all)");
        String sql = (playerId == null || playerId.isBlank())
            ? "SELECT player_id, league_id, appearance_count, goal_count, assist_count, play_time FROM SeasonPerformance"
            : "SELECT player_id, league_id, appearance_count, goal_count, assist_count, play_time FROM SeasonPerformance WHERE player_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (playerId != null && !playerId.isBlank()) {
                setRequiredInt(stmt, 1, playerId);
            }
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
            setNullableDecimal(stmt, 4, prompt(scanner, "transfer_fee (blank ok)"));
            executeUpdate(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void runReadOnlyQuery(Connection conn, Scanner scanner) {
        System.out.println("Read-only query helper. Example: SELECT * FROM Player;");
        String sql = prompt(scanner, "SQL (SELECT only)");
        if (sql == null || !sql.trim().toLowerCase().startsWith("select")) {
            System.out.println("Only SELECT statements are allowed.");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            executeQuery(stmt);
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
        if (value == null || value.isBlank()) {
            stmt.setNull(idx, Types.VARCHAR);
        } else {
            stmt.setString(idx, value);
        }
    }

    private static void setNullableInt(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(idx, Types.INTEGER);
        } else {
            try {
                stmt.setInt(idx, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new SQLException("Invalid integer value: " + value);
            }
        }
    }

    private static void setRequiredInt(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            throw new SQLException("Required integer value is missing.");
        }
        try {
            stmt.setInt(idx, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new SQLException("Invalid integer value: " + value);
        }
    }

    private static void setNullableDecimal(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(idx, Types.DECIMAL);
        } else {
            try {
                stmt.setBigDecimal(idx, new java.math.BigDecimal(value));
            } catch (NumberFormatException e) {
                throw new SQLException("Invalid decimal value: " + value);
            }
        }
    }

    private static void setRequiredDecimal(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            throw new SQLException("Required decimal value is missing.");
        }
        try {
            stmt.setBigDecimal(idx, new java.math.BigDecimal(value));
        } catch (NumberFormatException e) {
            throw new SQLException("Invalid decimal value: " + value);
        }
    }

    private static void setNullableDate(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(idx, Types.DATE);
        } else {
            try {
                stmt.setDate(idx, java.sql.Date.valueOf(value));
            } catch (IllegalArgumentException e) {
                throw new SQLException("Invalid date value (expected YYYY-MM-DD): " + value);
            }
        }
    }

    private static void setRequiredDate(PreparedStatement stmt, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            throw new SQLException("Required date value is missing.");
        }
        try {
            stmt.setDate(idx, java.sql.Date.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid date value (expected YYYY-MM-DD): " + value);
        }
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        StringBuilder header = new StringBuilder();
        for (int i = 1; i <= cols; i++) {
            if (i > 1) {
                header.append(" | ");
            }
            header.append(meta.getColumnLabel(i));
        }

        int rowCount = 0;
        StringBuilder rows = new StringBuilder();
        while (rs.next()) {
            rowCount++;
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) {
                    row.append(" | ");
                }
                String value = rs.getString(i);
                row.append(value == null ? "NULL" : value);
            }
            rows.append(row).append("\n");
        }

        // =============================================
        // ONLY PRINT HEADER AND ROWS IF RESULTS EXIST
        // =============================================
        if (rowCount == 0) {
            System.out.println("No data found for the given input. The record may exist but have no associated data, or the input may be incorrect.");
        } else {
            System.out.println(header);
            System.out.print(rows);
        }
    }



    private static void printSqlError(SQLException e) {
        System.out.println("Database error: " + e.getMessage());
        System.out.println("SQLState: " + e.getSQLState());
    }

    private static String prompt(Scanner scanner, String label) {
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

    private enum Role {
        ADMIN,
        USER
    }

    // ============================================================
    // USER VIEW METHODS
    // ============================================================

    private static void viewPlayersInLeague(Connection conn, Scanner scanner) {
//        show sample first
        String listSql = "SELECT l.league_name, l.season_name, COUNT(DISTINCT sp.player_id) AS player_count " +
                "FROM League l JOIN SeasonPerformance sp ON l.league_id = sp.league_id " +
                "GROUP BY l.league_id, l.league_name, l.season_name " +
                "ORDER BY player_count DESC LIMIT 20";
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
        // =============================================
        // SHOW SAMPLE PLAYER NAMES BEFORE PROMPTING
        // =============================================
        String listSql = "SELECT first_name, last_name FROM Player ORDER BY last_name LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nSample Players:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        // =============================================
        // PROMPT USER TO ENTER PLAYER NAME
        // =============================================
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
                // =============================================
                // FALLBACK: CHECK IF PLAYER EXISTS IN DB
                // =============================================
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
        // =============================================
        // SHOW AVAILABLE LEAGUES BEFORE PROMPTING USER
        // =============================================
        String listSql = "SELECT l.league_name, l.season_name, COUNT(DISTINCT sp.player_id) AS player_count FROM League l JOIN SeasonPerformance sp ON l.league_id = sp.league_id GROUP BY l.league_id, l.league_name, l.season_name ORDER BY player_count DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Leagues (sample):");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        // =============================================
        // PROMPT USER TO ENTER LEAGUE, SEASON, LIMIT
        // =============================================
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
        // =============================================
        // SHOW AVAILABLE LEAGUES BEFORE PROMPTING USER
        // =============================================
        String listSql = "SELECT l.league_name, l.season_name, COUNT(DISTINCT sp.player_id) AS player_count FROM League l JOIN SeasonPerformance sp ON l.league_id = sp.league_id GROUP BY l.league_id, l.league_name, l.season_name ORDER BY player_count DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Leagues (sample):");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        // =============================================
        // PROMPT USER TO ENTER LEAGUE AND SEASON
        // =============================================
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_teams_in_league(?, ?)")) {
            stmt.setString(1, prompt(scanner, "\nleague_name"));
            stmt.setString(2, prompt(scanner, "season_name (e.g. 22/23)"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewMatchResults(Connection conn, Scanner scanner) {
        // =============================================
        // SHOW ONLY LEAGUES THAT HAVE MATCH DATA
        // =============================================
        String listSql = "SELECT DISTINCT l.league_name, l.season_name, COUNT(m.match_id) AS match_count FROM League l JOIN `Match` m ON l.league_id = m.league_id GROUP BY l.league_id, l.league_name, l.season_name ORDER BY match_count DESC";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nLeagues with match data:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        // =============================================
        // PROMPT USER TO ENTER LEAGUE AND SEASON
        // =============================================
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_match_results(?, ?)")) {
            stmt.setString(1, prompt(scanner, "\nleague_name"));
            stmt.setString(2, prompt(scanner, "season_name (e.g. 25/26)"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewCoachStats(Connection conn, Scanner scanner) {
        // =============================================
        // SHOW ALL COACH NAMES BEFORE PROMPTING
        // =============================================
        String listSql = "SELECT first_name, last_name FROM Coach ORDER BY last_name";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Coaches:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        // =============================================
        // PROMPT USER TO ENTER COACH NAME
        // =============================================
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_coach_stats(?, ?)")) {
            stmt.setString(1, prompt(scanner, "\nfirst_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewStadiumStats(Connection conn, Scanner scanner) {
        // =============================================
        // SHOW ALL STADIUM NAMES BEFORE PROMPTING
        // =============================================
        String listSql = "SELECT stadium_name, city FROM Stadium ORDER BY stadium_name";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nAvailable Stadiums:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        // =============================================
        // PROMPT USER TO ENTER STADIUM NAME
        // =============================================
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_stadium_stats(?)")) {
            stmt.setString(1, prompt(scanner, "\nstadium_name"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewPlayerMarketValue(Connection conn, Scanner scanner) {
        // =============================================
        // SHOW SAMPLE PLAYER NAMES BEFORE PROMPTING
        // =============================================
        String listSql = "SELECT p.first_name, p.last_name FROM Player p JOIN MarketValue mv ON p.player_id = mv.player_id GROUP BY p.player_id ORDER BY COUNT(mv.market_value_date) DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nPlayers with market value data (top 20):");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        // =============================================
        // PROMPT USER TO ENTER PLAYER NAME
        // =============================================
        try (PreparedStatement stmt = conn.prepareStatement("CALL sp_player_market_value(?, ?)")) {
            stmt.setString(1, prompt(scanner, "\nfirst_name"));
            stmt.setString(2, prompt(scanner, "last_name"));
            executeQuery(stmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
    }

    private static void viewPlayerTransfers(Connection conn, Scanner scanner) {
        // =============================================
        // SHOW PLAYERS WITH MOST TRANSFERS
        // =============================================
        String listSql = "SELECT p.first_name, p.last_name, COUNT(t.transfer_id) AS transfer_count FROM Player p JOIN Transfer t ON p.player_id = t.player_id GROUP BY p.player_id ORDER BY transfer_count DESC LIMIT 20";
        try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
            System.out.println("\nPlayers with most transfers:");
            executeQuery(listStmt);
        } catch (SQLException e) {
            printSqlError(e);
        }
        // =============================================
        // PROMPT USER TO ENTER PLAYER NAME
        // =============================================
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
                // =============================================
                // FALLBACK: CHECK IF PLAYER EXISTS IN DB
                // =============================================
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

    private static Role parseRole(String roleInput) {
        if (roleInput == null) {
            return null;
        }
        String normalized = roleInput.trim().toLowerCase();
        if ("admin".equals(normalized)) {
            return Role.ADMIN;
        }
        if ("user".equals(normalized)) {
            return Role.USER;
        }
        return null;
    }
}

//
//& "C:\Users\Owner\.jdks\openjdk-26\bin\javac.exe" -cp "C:\Users\Owner\Desktop\mysql-connector-j-9.6.0\mysql-connector-j-9.6.0.jar;." AdminCli.java
//
//& "C:\Users\Owner\.jdks\openjdk-26\bin\java.exe" -cp "C:\Users\Owner\Desktop\mysql-connector-j-9.6.0\mysql-connector-j-9.6.0.jar;." AdminCli