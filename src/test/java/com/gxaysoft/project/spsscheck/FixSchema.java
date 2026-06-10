package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.persistence.DbConnection;
import java.sql.*;

public class FixSchema {
    public static void main(String[] args) throws Exception {
        Connection c = DbConnection.get();
        try {
            c.createStatement().execute(
                "ALTER TABLE sps_rule ADD COLUMN warning_message TEXT AFTER java_preview");
            System.out.println("Column warning_message added.");
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate"))
                System.out.println("Column already exists.");
            else throw e;
        }
        c.close();
    }
}
