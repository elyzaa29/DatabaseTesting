package com.praktikum.database.testing.library.service;

import com.praktikum.database.testing.library.dao.BookDAO;
import com.praktikum.database.testing.library.dao.BorrowingDAO;
import com.praktikum.database.testing.library.dao.UserDAO;
import com.praktikum.database.testing.library.model.Book;
import com.praktikum.database.testing.library.model.Borrowing;
import com.praktikum.database.testing.library.model.User;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BorrowingService {
    private static final Logger logger = Logger.getLogger(BorrowingService.class.getName());

    private final UserDAO userDAO;
    private final BookDAO bookDAO;
    private final BorrowingDAO borrowingDAO;

    public BorrowingService() {
        this.userDAO = new UserDAO();
        this.bookDAO = new BookDAO();
        this.borrowingDAO = new BorrowingDAO();
    }

    public BorrowingService(UserDAO userDAO, BookDAO bookDAO, BorrowingDAO borrowingDAO) {
        this.userDAO = userDAO;
        this.bookDAO = bookDAO;
        this.borrowingDAO = borrowingDAO;
    }

    public Borrowing borrowBook(Integer userId, Integer bookId, int borrowDays) throws SQLException {
        logger.info("Memproses peminjaman buku - User: " + userId + ", Book: " + bookId);

        // Validasi user exists dan active
        Optional<User> user = userDAO.findById(userId);
        if (user.isEmpty()) {
            logger.warning("User tidak ditemukan dengan ID: " + userId);
            throw new IllegalArgumentException("User tidak ditemukan dengan ID: " + userId);
        }

        if (!"active".equals(user.get().getStatus())) {
            logger.warning("User account tidak active. Status: " + user.get().getStatus());
            throw new IllegalStateException("User account tidak active. Status: " + user.get().getStatus());
        }

        // Validasi book exists
        Optional<Book> book = bookDAO.findById(bookId);
        if (book.isEmpty()) {
            logger.warning("Buku tidak ditemukan dengan ID: " + bookId);
            throw new IllegalArgumentException("Buku tidak ditemukan dengan ID: " + bookId);
        }

        // Validasi book available
        if (book.get().getAvailableCopies() <= 0) {
            logger.warning("Tidak ada kopi yang tersedia untuk buku ini");
            throw new IllegalStateException("Tidak ada kopi yang tersedia untuk buku ini");
        }

        // Validasi batas peminjaman
        int activeBorrowings = borrowingDAO.countActiveBorrowingsByUser(userId);
        if (activeBorrowings >= 5) {
            logger.warning("User sudah mencapai batas peminjaman: " + activeBorrowings + " buku");
            throw new IllegalStateException("User sudah mencapai batas peminjaman: " + activeBorrowings + " buku");
        }

        // Decrease available copies
        boolean decreased = bookDAO.decreaseAvailableCopies(bookId);
        if (!decreased) {
            logger.severe("Gagal mengurangi available copies untuk buku ID: " + bookId);
            throw new IllegalStateException("Gagal mengurangi available copies");
        }

        // Create borrowing record
        Timestamp dueDate = Timestamp.valueOf(LocalDateTime.now().plusDays(borrowDays));
        Borrowing borrowing = Borrowing.builder()
                .userId(userId)
                .bookId(bookId)
                .dueDate(dueDate)
                .status("borrowed")
                .notes("Dipinjam via BorrowingService - " + borrowDays + " hari")
                .build();

        Borrowing createdBorrowing = borrowingDAO.create(borrowing);
        logger.info("Peminjaman berhasil - Borrowing ID: " + createdBorrowing.getBorrowingId());

        return createdBorrowing;
    }

    public boolean returnBook(Integer borrowingId) throws SQLException {
        logger.info("Memproses pengembalian buku - Borrowing ID: " + borrowingId);

        // Validasi borrowing exists
        Optional<Borrowing> borrowing = borrowingDAO.findById(borrowingId);
        if (borrowing.isEmpty()) {
            logger.warning("Borrowing record tidak ditemukan dengan ID: " + borrowingId);
            throw new IllegalArgumentException("Borrowing record tidak ditemukan dengan ID: " + borrowingId);
        }

        // Validasi buku belum dikembalikan
        if (borrowing.get().getReturnDate() != null) {
            logger.warning("Buku sudah dikembalikan sebelumnya");
            throw new IllegalStateException("Buku sudah dikembalikan");
        }

        // Update return date
        Timestamp returnDate = new Timestamp(System.currentTimeMillis());
        boolean updated = borrowingDAO.returnBook(borrowingId, returnDate);
        if (!updated) {
            logger.severe("Gagal update return date untuk borrowing ID: " + borrowingId);
            throw new IllegalStateException("Gagal update return date");
        }

        // Increase available copies
        Integer bookId = borrowing.get().getBookId();
        boolean increased = bookDAO.increaseAvailableCopies(bookId);
        if (!increased) {
            logger.severe("Gagal menambah available copies untuk buku ID: " + bookId);
            throw new IllegalStateException("Gagal menambah available copies");
        }

        logger.info("Pengembalian berhasil - Book ID: " + bookId);
        return true;
    }

    public boolean canUserBorrowBook(Integer userId, Integer bookId) throws SQLException {
        // Check user exists dan active
        Optional<User> user = userDAO.findById(userId);
        if (user.isEmpty() || !"active".equals(user.get().getStatus())) {
            return false;
        }

        // Check book exists dan available
        Optional<Book> book = bookDAO.findById(bookId);
        if (book.isEmpty() || book.get().getAvailableCopies() <= 0) {
            return false;
        }

        // Check user tidak melebihi batas peminjaman
        int activeBorrowings = borrowingDAO.countActiveBorrowingsByUser(userId);
        return activeBorrowings < 5;
    }

    public double calculateFine(Integer borrowingId) throws SQLException {
        Optional<Borrowing> borrowing = borrowingDAO.findById(borrowingId);
        if (borrowing.isEmpty()) {
            throw new IllegalArgumentException("Borrowing record tidak ditemukan");
        }

        Borrowing borrow = borrowing.get();

        // Jika sudah dikembalikan, return existing fine amount
        if (borrow.getReturnDate() != null) {
            return borrow.getFineAmount() != null ? borrow.getFineAmount().doubleValue() : 0.0;
        }

        // Jika belum overdue, tidak ada denda
        if (borrow.getDueDate().after(new Timestamp(System.currentTimeMillis()))) {
            return 0.0;
        }

        // Hitung denda: 5000 per hari keterlambatan
        long overdueDays = (System.currentTimeMillis() - borrow.getDueDate().getTime()) / (1000 * 60 * 60 * 24);
        double fine = overdueDays * 5000.0;

        logger.info("Denda dihitung - Borrowing ID: " + borrowingId + ", Denda: " + fine);
        return fine;
    }

    public List<Borrowing> getUserActiveBorrowings(Integer userId) throws SQLException {
        return borrowingDAO.findByUserId(userId).stream()
                .filter(borrowing -> borrowing.getReturnDate() == null)
                .collect(Collectors.toList());
    }

    public List<Borrowing> getUserBorrowingHistory(Integer userId) throws SQLException {
        return borrowingDAO.findByUserId(userId);
    }

    public void updateOverdueStatus() throws SQLException {
        logger.info("Memperbarui status overdue borrowings...");

        List<Borrowing> overdueBorrowings = borrowingDAO.findOverdueBorrowings();

        for (Borrowing borrowing : overdueBorrowings) {
            if (!"overdue".equals(borrowing.getStatus())) {
                borrowingDAO.updateStatus(borrowing.getBorrowingId(), "overdue");
            }

            // Calculate dan update fine amount
            double fine = calculateFine(borrowing.getBorrowingId());
            borrowingDAO.updateFineAmount(borrowing.getBorrowingId(), fine);

            logger.info("Updated to overdue - Borrowing ID: " + borrowing.getBorrowingId() + ", Fine: " + fine);
        }

        logger.info("Overdue status update completed - Total: " + overdueBorrowings.size());
    }
}