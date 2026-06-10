package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.persistence.DbConnection;
import java.sql.*;

public class VerifyDb {
    public static void main(String[] args) throws Exception {
        try (Connection c = DbConnection.get()) {
            String[][] queries = {
                {"sps_script", "SELECT COUNT(*) FROM sps_script"},
                {"sps_rule", "SELECT COUNT(*) FROM sps_rule"},
                {"sps_rule_step", "SELECT COUNT(*) FROM sps_rule_step"},
                {"sps_output_rule", "SELECT COUNT(*) FROM sps_output_rule"},
                {"sps_unsupported", "SELECT COUNT(*) FROM sps_unsupported_statement"},
            };
            for (String[] q : queries) {
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(q[1]);
                rs.next();
                System.out.printf("%-25s %d%n", q[0], rs.getInt(1));
                rs.close(); s.close();
            }

            System.out.println("\n--- Rule types ---");
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT rule_type, COUNT(*) FROM sps_rule GROUP BY rule_type");
            while (rs.next()) System.out.printf("  %-15s %d%n", rs.getString(1), rs.getInt(2));
            rs.close();

            System.out.println("\n--- Step types ---");
            rs = c.createStatement().executeQuery(
                "SELECT step_type, COUNT(*) FROM sps_rule_step GROUP BY step_type");
            while (rs.next()) System.out.printf("  %-15s %d%n", rs.getString(1), rs.getInt(2));
            rs.close();

            System.out.println("\n--- Scripts ---");
            rs = c.createStatement().executeQuery(
                "SELECT script_name, parse_status FROM sps_script ORDER BY id");
            while (rs.next()) System.out.printf("  %-10s %s%n", rs.getString(1), rs.getString(2));
            rs.close();
        }
    }
}
