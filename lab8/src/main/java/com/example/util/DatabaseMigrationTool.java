package com.example.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseMigrationTool {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseMigrationTool(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public void executeMigration() {
        System.out.println("Démarrage de la migration...");

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("migration_v2.sql");
            if (inputStream == null) throw new RuntimeException("migration_v2.sql introuvable");

            String script = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));

            // ⚠️ Simpliste: OK si script sans procédures DELIMITER
            String[] statements = script.split(";");
            try (Statement st = connection.createStatement()) {
                for (String s : statements) {
                    String sql = s.trim();
                    if (sql.isEmpty()) continue;
                    if (sql.startsWith("--")) continue;
                    System.out.println("Exécution: " + sql);
                    st.execute(sql);
                }
            }

            System.out.println("Migration terminée avec succès !");
        } catch (Exception e) {
            System.err.println("Erreur migration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DatabaseMigrationTool tool = new DatabaseMigrationTool(
                "jdbc:mysql://localhost:3306/reservation_salles",
                "root",
                "password"
        );
        tool.executeMigration();
    }
}