import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
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

            if (!adminLogin(scanner)) {
                System.out.println("Admin login failed. Exiting.");
                return;
            }

            adminMenu(conn, scanner);
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

    private static boolean adminLogin(Scanner scanner) {
        String adminUser = getConfig("ADMIN_USER", "");
        String adminPass = getConfig("ADMIN_PASS", "");

        System.out.println("Admin login");
        String username = prompt(scanner, "Username");
        String password = prompt(scanner, "Password");

        if (adminUser.isEmpty() || adminPass.isEmpty()) {
            System.out.println("Warning: ADMIN_USER/ADMIN_PASS not set. Allowing login for any credentials.");
            return true;
        }

        return adminUser.equals(username) && adminPass.equals(password);
    }

    private static void adminMenu(Connection conn, Scanner scanner) {
        System.out.println("Note: sp_admin_* procedure names are placeholders. Update them to match your MySQL routines.");
        while (true) {
            System.out.println("\nAdmin Menu");
            System.out.println("1. Create player (sp_admin_create_player)");
            System.out.println("2. Update player (sp_admin_update_player)");
            System.out.println("3. Delete player (sp_admin_delete_player)");
            System.out.println("4. Create club (sp_admin_create_club)");
            System.out.println("5. Update club (sp_admin_update_club)");
            System.out.println("6. Delete club (sp_admin_delete_club)");
            System.out.println("7. Create league (sp_admin_create_league)");
            System.out.println("8. Update league (sp_admin_update_league)");
            System.out.println("9. Delete league (sp_admin_delete_league)");
            System.out.println("10. Create match (sp_admin_create_match)");
            System.out.println("11. Update match (sp_admin_update_match)");
            System.out.println("12. Delete match (sp_admin_delete_match)");
            System.out.println("13. Create market value (sp_admin_create_market_value)");
            System.out.println("14. Update market value (sp_admin_update_market_value)");
            System.out.println("15. Delete market value (sp_admin_delete_market_value)");
            System.out.println("16. Create player match stat (sp_admin_create_player_match_stat)");
            System.out.println("17. Update player match stat (sp_admin_update_player_match_stat)");
            System.out.println("18. Delete player match stat (sp_admin_delete_player_match_stat)");
            System.out.println("19. Add player-team relation (sp_admin_create_player_team_relation)");
            System.out.println("20. Delete player-team relation (sp_admin_delete_player_team_relation)");
            System.out.println("21. Create user (sp_admin_create_user)");
            System.out.println("22. Delete user (sp_admin_delete_user)");
            System.out.println("23. Record transfer (sp_record_transfer)");
            System.out.println("24. Call custom procedure");
            System.out.println("25. Exit");

            String choice = prompt(scanner, "Select an option");
            switch (choice) {
                case "1":
                    callProcedure(conn, "sp_admin_create_player", promptPlayerParams(scanner, false));
                    break;
                case "2":
                    callProcedure(conn, "sp_admin_update_player", promptPlayerParams(scanner, true));
                    break;
                case "3":
                    callProcedure(conn, "sp_admin_delete_player", promptSingleId(scanner, "player_id"));
                    break;
                case "4":
                    callProcedure(conn, "sp_admin_create_club", promptClubParams(scanner, false));
                    break;
                case "5":
                    callProcedure(conn, "sp_admin_update_club", promptClubParams(scanner, true));
                    break;
                case "6":
                    callProcedure(conn, "sp_admin_delete_club", promptSingleId(scanner, "club_id"));
                    break;
                case "7":
                    callProcedure(conn, "sp_admin_create_league", promptLeagueParams(scanner, false));
                    break;
                case "8":
                    callProcedure(conn, "sp_admin_update_league", promptLeagueParams(scanner, true));
                    break;
                case "9":
                    callProcedure(conn, "sp_admin_delete_league", promptSingleId(scanner, "league_id"));
                    break;
                case "10":
                    callProcedure(conn, "sp_admin_create_match", promptMatchParams(scanner, false));
                    break;
                case "11":
                    callProcedure(conn, "sp_admin_update_match", promptMatchParams(scanner, true));
                    break;
                case "12":
                    callProcedure(conn, "sp_admin_delete_match", promptSingleId(scanner, "match_id"));
                    break;
                case "13":
                    callProcedure(conn, "sp_admin_create_market_value", promptMarketValueParams(scanner, false));
                    break;
                case "14":
                    callProcedure(conn, "sp_admin_update_market_value", promptMarketValueParams(scanner, true));
                    break;
                case "15":
                    callProcedure(conn, "sp_admin_delete_market_value", promptMarketValueDeleteParams(scanner));
                    break;
                case "16":
                    callProcedure(conn, "sp_admin_create_player_match_stat", promptPlayerMatchStatParams(scanner, false));
                    break;
                case "17":
                    callProcedure(conn, "sp_admin_update_player_match_stat", promptPlayerMatchStatParams(scanner, true));
                    break;
                case "18":
                    callProcedure(conn, "sp_admin_delete_player_match_stat", promptPlayerMatchStatDeleteParams(scanner));
                    break;
                case "19":
                    callProcedure(conn, "sp_admin_create_player_team_relation", promptPlayerTeamRelationParams(scanner));
                    break;
                case "20":
                    callProcedure(conn, "sp_admin_delete_player_team_relation", promptPlayerTeamRelationParams(scanner));
                    break;
                case "21":
                    callProcedure(conn, "sp_admin_create_user", promptUserParams(scanner, false));
                    break;
                case "22":
                    callProcedure(conn, "sp_admin_delete_user", promptSingleId(scanner, "user_id"));
                    break;
                case "23":
                    callProcedure(conn, "sp_record_transfer", promptTransferParams(scanner));
                    break;
                case "24":
                    callCustomProcedure(conn, scanner);
                    break;
                case "25":
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static List<String> promptPlayerParams(Scanner scanner, boolean includeId) {
        List<String> params = new ArrayList<>();
        if (includeId) {
            params.add(prompt(scanner, "player_id"));
        }
        params.add(prompt(scanner, "first_name"));
        params.add(prompt(scanner, "last_name"));
        params.add(prompt(scanner, "dob (YYYY-MM-DD or blank)"));
        params.add(prompt(scanner, "place_of_birth (blank ok)"));
        params.add(prompt(scanner, "height_cm (blank ok)"));
        params.add(prompt(scanner, "preferred_foot [Left|Right|Both]"));
        params.add(prompt(scanner, "position_id (blank ok)"));
        params.add(prompt(scanner, "country_abbr (3-letter code)"));
        params.add(prompt(scanner, "club_id (blank ok)"));
        return params;
    }

    private static List<String> promptClubParams(Scanner scanner, boolean includeId) {
        List<String> params = new ArrayList<>();
        if (includeId) {
            params.add(prompt(scanner, "club_id"));
        }
        params.add(prompt(scanner, "club_name"));
        params.add(prompt(scanner, "country_abbr"));
        params.add(prompt(scanner, "league_id"));
        params.add(prompt(scanner, "stadium_id (blank ok)"));
        params.add(prompt(scanner, "coach_id (blank ok)"));
        return params;
    }

    private static List<String> promptTransferParams(Scanner scanner) {
        List<String> params = new ArrayList<>();
        params.add(prompt(scanner, "player_id"));
        params.add(prompt(scanner, "new_club_id"));
        params.add(prompt(scanner, "transfer_date (YYYY-MM-DD)"));
        params.add(prompt(scanner, "transfer_fee"));
        return params;
    }

    private static List<String> promptLeagueParams(Scanner scanner, boolean includeId) {
        List<String> params = new ArrayList<>();
        if (includeId) {
            params.add(prompt(scanner, "league_id"));
        }
        params.add(prompt(scanner, "league_name"));
        params.add(prompt(scanner, "country_abbr"));
        return params;
    }

    private static List<String> promptMatchParams(Scanner scanner, boolean includeId) {
        List<String> params = new ArrayList<>();
        if (includeId) {
            params.add(prompt(scanner, "match_id"));
        }
        params.add(prompt(scanner, "home_team_id"));
        params.add(prompt(scanner, "away_team_id"));
        params.add(prompt(scanner, "match_date (YYYY-MM-DD)"));
        params.add(prompt(scanner, "home_score"));
        params.add(prompt(scanner, "away_score"));
        return params;
    }

    private static List<String> promptMarketValueParams(Scanner scanner, boolean includeId) {
        List<String> params = new ArrayList<>();
        params.add(prompt(scanner, "player_id"));
        params.add(prompt(scanner, "market_value_date (YYYY-MM-DD)"));
        if (includeId) {
            params.add(prompt(scanner, "market_value (new)"));
        } else {
            params.add(prompt(scanner, "market_value"));
        }
        return params;
    }

    private static List<String> promptMarketValueDeleteParams(Scanner scanner) {
        List<String> params = new ArrayList<>();
        params.add(prompt(scanner, "player_id"));
        params.add(prompt(scanner, "market_value_date (YYYY-MM-DD)"));
        return params;
    }

    private static List<String> promptPlayerMatchStatParams(Scanner scanner, boolean includeId) {
        List<String> params = new ArrayList<>();
        params.add(prompt(scanner, "match_id"));
        params.add(prompt(scanner, "player_id"));
        params.add(prompt(scanner, "minutes_played"));
        params.add(prompt(scanner, "goals"));
        params.add(prompt(scanner, "assists"));
        params.add(prompt(scanner, "yellow_cards"));
        params.add(prompt(scanner, "red_cards"));
        params.add(prompt(scanner, "shots"));
        params.add(prompt(scanner, "passes"));
        params.add(prompt(scanner, "tackles"));
        if (includeId) {
            params.add(prompt(scanner, "rating (new)"));
        } else {
            params.add(prompt(scanner, "rating"));
        }
        return params;
    }

    private static List<String> promptPlayerMatchStatDeleteParams(Scanner scanner) {
        List<String> params = new ArrayList<>();
        params.add(prompt(scanner, "match_id"));
        params.add(prompt(scanner, "player_id"));
        return params;
    }

    private static List<String> promptPlayerTeamRelationParams(Scanner scanner) {
        List<String> params = new ArrayList<>();
        params.add(prompt(scanner, "player_id"));
        params.add(prompt(scanner, "club_id"));
        params.add(prompt(scanner, "start_date (YYYY-MM-DD, blank ok)"));
        params.add(prompt(scanner, "end_date (YYYY-MM-DD, blank ok)"));
        return params;
    }

    private static List<String> promptUserParams(Scanner scanner, boolean includeId) {
        List<String> params = new ArrayList<>();
        if (includeId) {
            params.add(prompt(scanner, "user_id"));
        }
        params.add(prompt(scanner, "username"));
        params.add(prompt(scanner, "password"));
        params.add(prompt(scanner, "role (admin|user)"));
        return params;
    }

    private static List<String> promptSingleId(Scanner scanner, String label) {
        List<String> params = new ArrayList<>();
        params.add(prompt(scanner, label));
        return params;
    }

    private static void callCustomProcedure(Connection conn, Scanner scanner) {
        String procName = prompt(scanner, "Procedure name");
        String raw = prompt(scanner, "Params (comma-separated, empty for none)");
        List<String> params = splitParams(raw);
        callProcedure(conn, procName, params);
    }

    private static List<String> splitParams(String raw) {
        List<String> params = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return params;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            params.add(part.trim());
        }
        return params;
    }

    private static void callProcedure(Connection conn, String procName, List<String> params) {
        String callSql = buildCall(procName, params.size());
        try (CallableStatement stmt = conn.prepareCall(callSql)) {
            for (int i = 0; i < params.size(); i++) {
                String value = params.get(i);
                if (value == null || value.isBlank()) {
                    stmt.setNull(i + 1, Types.VARCHAR);
                } else {
                    stmt.setString(i + 1, value);
                }
            }

            boolean hasResult = stmt.execute();
            int updateCount = stmt.getUpdateCount();

            if (hasResult) {
                try (ResultSet rs = stmt.getResultSet()) {
                    printResultSet(rs);
                }
            } else {
                System.out.println("Procedure executed. Rows affected: " + updateCount);
            }

            while (stmt.getMoreResults() || stmt.getUpdateCount() != -1) {
                ResultSet rs = stmt.getResultSet();
                if (rs != null) {
                    printResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Procedure call failed: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
        }
    }

    private static String buildCall(String procName, int paramCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{CALL ").append(procName).append("(");
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        sb.append(")}");
        return sb.toString();
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
        System.out.println(header);

        while (rs.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) {
                    row.append(" | ");
                }
                String value = rs.getString(i);
                row.append(value == null ? "NULL" : value);
            }
            System.out.println(row);
        }
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
}
