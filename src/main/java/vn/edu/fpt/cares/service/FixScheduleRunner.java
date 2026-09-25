package vn.edu.fpt.cares.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class FixScheduleRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public FixScheduleRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String sql = "UPDATE staff_schedule ss " +
                     "SET actual_start_time = sc.start_time::time, " +
                     "    actual_end_time = sc.end_time::time " +
                     "FROM shift_config sc " +
                     "WHERE ss.shift_id = sc.shift_id " +
                     "AND (ss.actual_start_time != sc.start_time::time OR ss.actual_end_time != sc.end_time::time)";
        try {
            int updated = jdbcTemplate.update(sql);
            System.out.println("========== SYNCED " + updated + " STAFF SCHEDULES ==========");
        } catch (Exception e) {
            System.err.println("========== FAILED TO SYNC STAFF SCHEDULES: " + e.getMessage() + " ==========");
        }
    }
}
