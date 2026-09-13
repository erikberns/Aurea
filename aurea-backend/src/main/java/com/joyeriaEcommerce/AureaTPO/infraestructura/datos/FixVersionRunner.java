package com.joyeriaEcommerce.AureaTPO.infraestructura.datos;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FixVersionRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public FixVersionRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE product ADD COLUMN IF NOT EXISTS version INT DEFAULT 0");
            jdbcTemplate.execute("UPDATE product SET version = 0 WHERE version IS NULL");
        } catch (Exception e) {}
        try {
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS version INT DEFAULT 0");
            jdbcTemplate.execute("UPDATE orders SET version = 0 WHERE version IS NULL");
        } catch (Exception e) {}
    }
}
