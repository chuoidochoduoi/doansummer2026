package vn.edu.fpt.cares.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;

@Component
public class FixLabLimitsRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public FixLabLimitsRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            File sqlFile = new File("sync_limits.sql");
            if (sqlFile.exists()) {
                String sql = new String(Files.readAllBytes(Paths.get(sqlFile.getAbsolutePath())), "UTF-8");
                String[] statements = sql.split(";");
                for (String statement : statements) {
                    if (!statement.trim().isEmpty()) {
                        jdbcTemplate.update(statement.trim());
                    }
                }
                System.out.println("========== SYNCED LAB LIMITS TO DATABASE ==========");
                // Optional: Delete the file after successful run so it doesn't run every time
                // sqlFile.delete(); 
            }
        } catch (Exception e) {
            System.err.println("========== FAILED TO SYNC LAB LIMITS: " + e.getMessage() + " ==========");
        }
    }
}
