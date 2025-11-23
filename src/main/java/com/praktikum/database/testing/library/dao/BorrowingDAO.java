package com.praktikum.database.testing.library.dao;

import com.praktikum.database.testing.library.config.DatabaseConfig;
import com.praktikum.database.testing.library.model.Borrowing;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowingDAO {

    public Borrowing create(Borrowing borrowing) throws SQLException {
        String sql = "INSERT INTO borrowings (user_id, book_id, due_date, status, notes) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "RETURNING borrowing_id, borrow_date, created_at, updated_at";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, borrowing.getUserId());
            pstmt.setInt(2, borrowing.getBookId());
            pstmt.setTimestamp(3, borrowing.getDueDate());
            pstmt.setString(4, borrowing.getStatus() != null ? borrowing.getStatus() : "borrowed");
            pstmt.setString(5, borrowing.getNotes());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                borrowing.setBorrowingId(rs.getInt("borrowing_id"));
                borrowing.setBorrowDate(rs.getTimestamp("borrow_date"));
                borrowing.setCreatedAt(rs.getTimestamp("created_at"));
                borrowing.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            return borrowing;
        }
    }

    public Optional<Borrowing> findById(Integer borrowingId) throws SQLException {
        String sql = "SELECT * FROM borrowings WHERE borrowing_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, borrowingId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToBorrowing(rs));
            }
            return Optional.empty();
        }
    }

    public List<Borrowing> findByUserId(Integer userId) throws SQLException {
        String sql = "SELECT * FROM borrowings WHERE user_id = ? ORDER BY borrow_date DESC";
        List<Borrowing> borrowings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                borrowings.add(mapResultSetToBorrowing(rs));
            }
        }
        return borrowings;
    }

    public List<Borrowing> findByBookId(Integer bookId) throws SQLException {
        String sql = "SELECT * FROM borrowings WHERE book_id = ? ORDER BY borrow_date DESC";
        List<Borrowing> borrowings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                borrowings.add(mapResultSetToBorrowing(rs));
            }
        }
        return borrowings;
    }

    public List<Borrowing> findActiveBorrowings() throws SQLException {
        String sql = "SELECT * FROM borrowings WHERE return_date IS NULL ORDER BY borrow_date DESC";
        List<Borrowing> borrowings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                borrowings.add(mapResultSetToBorrowing(rs));
            }
        }
        return borrowings;
    }

    public List<Borrowing> findOverdueBorrowings() throws SQLException {
        String sql = "SELECT * FROM borrowings WHERE return_date IS NULL AND due_date < CURRENT_TIMESTAMP " +
                "ORDER BY due_date ASC";
        List<Borrowing> borrowings = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                borrowings.add(mapResultSetToBorrowing(rs));
            }
        }
        return borrowings;
    }

    public boolean returnBook(Integer borrowingId, Timestamp returnDate) throws SQLException {
        String sql = "UPDATE borrowings SET return_date = ?, status = 'returned', updated_at = CURRENT_TIMESTAMP " +
                "WHERE borrowing_id = ? AND return_date IS NULL";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, returnDate);
            pstmt.setInt(2, borrowingId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(Integer borrowingId, String status) throws SQLException {
        String sql = "UPDATE borrowings SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE borrowing_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, borrowingId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateFineAmount(Integer borrowingId, Double fineAmount) throws SQLException {
        String sql = "UPDATE borrowings SET fine_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE borrowing_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, fineAmount);
            pstmt.setInt(2, borrowingId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(Integer borrowingId) throws SQLException {
        String sql = "DELETE FROM borrowings WHERE borrowing_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, borrowingId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrowings";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public int countActiveBorrowingsByUser(Integer userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrowings WHERE user_id = ? AND return_date IS NULL";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private Borrowing mapResultSetToBorrowing(ResultSet rs) throws SQLException {
        return Borrowing.builder()
                .borrowingId(rs.getInt("borrowing_id"))
                .userId(rs.getInt("user_id"))
                .bookId(rs.getInt("book_id"))
                .borrowDate(rs.getTimestamp("borrow_date"))
                .dueDate(rs.getTimestamp("due_date"))
                .returnDate(rs.getTimestamp("return_date"))
                .status(rs.getString("status"))
                .fineAmount(rs.getBigDecimal("fine_amount"))
                .finePaid(rs.getBoolean("fine_paid"))
                .notes(rs.getString("notes"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }
}