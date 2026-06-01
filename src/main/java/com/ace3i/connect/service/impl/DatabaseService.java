package com.ace3i.connect.service.impl;

import com.ace3i.connect.service.IDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.StringJoiner;

@Service
public class DatabaseService implements IDatabaseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String[] TABLES = {
            "CNCFG", "CNUSE", "CNCLI", "CNINT", "CNMIS", "CNNOT", "CNAUD",
            "CNAPPVERSION", "CNFEATURE", "CNDEPLOYMENT", "CNDEPLOYMENT_FEATURE"
    };

    @Override
    public String exportDatabase() {
        StringBuilder sql = new StringBuilder();
        sql.append("-- ACE3i Connect Database Export\n");
        sql.append("SET FOREIGN_KEY_CHECKS = 0;\n\n");

        for (String table : TABLES) {
            sql.append("-- Dumping data for table ").append(table).append("\n");
            sql.append("TRUNCATE TABLE ").append(table).append(";\n");

            jdbcTemplate.query("SELECT * FROM " + table, rs -> {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                StringJoiner columns = new StringJoiner(", ");
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnName(i));
                }

                while (rs.next()) {
                    StringJoiner values = new StringJoiner(", ");
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        if (value == null) {
                            values.add("NULL");
                        } else {
                            int type = metaData.getColumnType(i);
                            if (type == Types.CHAR || type == Types.VARCHAR || type == Types.LONGVARCHAR || type == Types.CLOB) {
                                values.add("'" + value.toString().replace("'", "''") + "'");
                            } else if (type == Types.DATE) {
                                values.add("'" + value.toString() + "'");
                            } else if (type == Types.TIMESTAMP || type == Types.TIMESTAMP_WITH_TIMEZONE) {
                                values.add("'" + value.toString() + "'");
                            } else if (type == Types.BIT || type == Types.BOOLEAN) {
                                values.add(value.toString().equalsIgnoreCase("true") || value.toString().equals("1") ? "1" : "0");
                            } else {
                                values.add(value.toString());
                            }
                        }
                    }
                    sql.append("INSERT INTO ").append(table)
                            .append(" (").append(columns.toString()).append(") VALUES (")
                            .append(values.toString()).append(");\n");
                }
                return null;
            });
            sql.append("\n");
        }

        sql.append("SET FOREIGN_KEY_CHECKS = 1;\n");
        return sql.toString();
    }

    @Override
    @Transactional
    public void importDatabase(byte[] sqlData) {
        String script = new String(sqlData);
        // On sépare les instructions par point-virgule, en faisant attention aux retours à la ligne
        String[] statements = script.split(";\\s*\\n");

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
        for (String statement : statements) {
            String trimmedStatement = statement.trim();
            if (!trimmedStatement.isEmpty() && !trimmedStatement.startsWith("--")) {
                jdbcTemplate.execute(trimmedStatement);
            }
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
    }

    @Override
    @Transactional
    public void clearDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
        for (String table : TABLES) {
            // On évite de vider la table des utilisateurs pour ne pas bloquer l'accès à l'application
            if (!"CNUSE".equals(table)) {
                jdbcTemplate.execute("TRUNCATE TABLE " + table);
            }
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
    }
}
