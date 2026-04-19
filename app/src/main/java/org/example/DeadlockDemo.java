package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeadlockDemo {

    // Simulated database URL (using H2 in-memory DB for reproducibility)
    private static final String DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    public void run() {
        setupDatabase();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Transfers from Account 1 -> Account 2
        executor.submit(() -> {
            try {
                performTransfer(1, 2, 100);
            } catch (Exception e) {
                System.out.println("Thread 1 Error: " + e.getMessage());
            }
        });

        // Thread 2: Transfers from Account 2 -> Account 1
        executor.submit(() -> {
            try {
                performTransfer(2, 1, 100);
            } catch (Exception e) {
                System.out.println("Thread 2 Error: " + e.getMessage());
            }
        });

        executor.shutdown();
    }

    /**
     * Simulates a transaction that locks rows in the order they are provided.
     * This is the source of the deadlock if called with swapped IDs concurrently.
     */
    public static void performTransfer(int fromAccountId, int toAccountId, double amount) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        conn.setAutoCommit(false); // Start transaction

        try {
            System.out.println(
                    Thread.currentThread().getName() + ": Starting transfer " + fromAccountId + " -> " + toAccountId);

            // STEP 1: Lock the 'FROM' account
            // SELECT FOR UPDATE ensures this row is locked until commit/rollback
            String lockFromSql = "SELECT balance FROM accounts WHERE id = ? FOR UPDATE";
            PreparedStatement psFrom = conn.prepareStatement(lockFromSql);
            psFrom.setInt(1, fromAccountId);
            psFrom.executeQuery();
            System.out.println(Thread.currentThread().getName() + ": Locked Account " + fromAccountId);

            // Simulate some processing time to increase chance of deadlock
            Thread.sleep(100);

            // STEP 2: Lock the 'TO' account
            String lockToSql = "SELECT balance FROM accounts WHERE id = ? FOR UPDATE";
            PreparedStatement psTo = conn.prepareStatement(lockToSql);
            psTo.setInt(1, toAccountId);
            psTo.executeQuery();
            System.out.println(Thread.currentThread().getName() + ": Locked Account " + toAccountId);

            // STEP 3: Perform updates
            String updateSql = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
            PreparedStatement psUpdateFrom = conn.prepareStatement(updateSql);
            psUpdateFrom.setDouble(1, amount);
            psUpdateFrom.setInt(2, fromAccountId);
            psUpdateFrom.executeUpdate();

            PreparedStatement psUpdateTo = conn
                    .prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id = ?");
            psUpdateTo.setDouble(1, amount);
            psUpdateTo.setInt(2, toAccountId);
            psUpdateTo.executeUpdate();

            conn.commit();
            System.out.println(Thread.currentThread().getName() + ": Transfer committed successfully.");

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println(
                            Thread.currentThread().getName() + ": Transaction rolled back due to error/deadlock.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    private static void setupDatabase() {
        try {
            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            // Create table
            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS accounts (id INT PRIMARY KEY, balance DOUBLE)");

            // Insert test data if empty
            var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM accounts");
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().executeUpdate("INSERT INTO accounts VALUES (1, 1000)");
                conn.createStatement().executeUpdate("INSERT INTO accounts VALUES (2, 1000)");
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
