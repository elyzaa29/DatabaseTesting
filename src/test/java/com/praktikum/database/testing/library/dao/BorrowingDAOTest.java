package com.praktikum.database.testing.library.dao;

// Import classes untuk testing
import com.github.javafaker.Faker;
import com.praktikum.database.testing.library.BaseDatabaseTest;
import com.praktikum.database.testing.library.model.Borrowing;
import com.praktikum.database.testing.library.model.User;
import com.praktikum.database.testing.library.model.Book;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Import static assertions
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite untuk BorrowingDAO
 * Menguji semua operasi CRUD pada entity Borrowing
 * Termasuk testing untuk borrowing workflows dan status management
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("BorrowingDAO CRUD Operations Test Suite")
public class BorrowingDAOTest extends BaseDatabaseTest {

    // Test dependencies
    private static UserDAO userDAO;
    private static BookDAO bookDAO;
    private static BorrowingDAO borrowingDAO;
    private static Faker faker;

    // Test data
    private static User testUser;
    private static Book testBook;
    private static Borrowing testBorrowing;

    @BeforeAll
    static void setUpAll() throws SQLException {
        logger.info("Starting BorrowingDAO CRUD Tests");

        // Initialize dependencies
        userDAO = new UserDAO();
        bookDAO = new BookDAO();
        borrowingDAO = new BorrowingDAO();
        faker = new Faker();

        // Create test user dan book untuk digunakan dalam tests
        setupTestData();
    }

    /**
     * Setup test data yang digunakan oleh semua tests
     */
    private static void setupTestData() throws SQLException {
        // Create test user
        testUser = User.builder()
                .username("borrowing_test_user_" + System.currentTimeMillis())
                .email("borrowing_test@" + System.currentTimeMillis() + ".com")
                .fullName("Borrowing Test User")
                .phone("081234567890")
                .role("member")
                .status("active")
                .build();
        testUser = userDAO.create(testUser);

        // Create test book
        testBook = Book.builder()
                .isbn("978borrowing" + System.currentTimeMillis())
                .title("Borrowing Test Book")
                .authorId(1)
                .publisherId(1)
                .categoryId(1)
                .publicationYear(2023)
                .pages(300)
                .totalCopies(5)
                .availableCopies(5)
                .build();
        testBook = bookDAO.create(testBook);

        logger.info("Test data created - User ID: " + testUser.getUserId() + ", Book ID: " + testBook.getBookId());
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        logger.info("BorrowingDAO CRUD Tests Completed");

        // Cleanup test data
        if (testBorrowing != null && testBorrowing.getBorrowingId() != null) {
            try {
                borrowingDAO.delete(testBorrowing.getBorrowingId());
            } catch (SQLException e) {
                logger.warning("Gagal cleanup borrowing: " + e.getMessage());
            }
        }

        if (testBook != null && testBook.getBookId() != null) {
            try {
                bookDAO.delete(testBook.getBookId());
            } catch (SQLException e) {
                logger.warning("Gagal cleanup book: " + e.getMessage());
            }
        }

        if (testUser != null && testUser.getUserId() != null) {
            try {
                userDAO.delete(testUser.getUserId());
            } catch (SQLException e) {
                logger.warning("Gagal cleanup user: " + e.getMessage());
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Setup borrowing data untuk setiap test
        testBorrowing = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(Timestamp.valueOf(LocalDateTime.now().plusDays(14)))
                .status("borrowed")
                .notes("Test borrowing for automated testing")
                .build();

        testBorrowing = borrowingDAO.create(testBorrowing);
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Cleanup borrowing data setelah setiap test
        if (testBorrowing != null && testBorrowing.getBorrowingId() != null) {
            try {
                borrowingDAO.delete(testBorrowing.getBorrowingId());
            } catch (SQLException e) {
                logger.warning("Gagal cleanup borrowing dalam tearDown: " + e.getMessage());
            }
        }
    }

    // =============================================
    // POSITIVE TEST CASES
    // =============================================

    @Test
    @Order(1)
    @DisplayName("TC201: Create borrowing dengan data valid - Should Success")
    void testCreateBorrowing_WithValidData_ShouldSuccess() throws SQLException {
        // ARRANGE - Data sudah di-setup di @BeforeEach

        // ASSERT - Verify borrowing created successfully
        assertThat(testBorrowing)
                .isNotNull()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getBorrowingId()).isNotNull().isPositive();
                    assertThat(borrowing.getUserId()).isEqualTo(testUser.getUserId());
                    assertThat(borrowing.getBookId()).isEqualTo(testBook.getBookId());
                    assertThat(borrowing.getStatus()).isEqualTo("borrowed");
                    assertThat(borrowing.getBorrowDate()).isNotNull();
                    assertThat(borrowing.getDueDate()).isNotNull();
                    assertThat(borrowing.getReturnDate()).isNull(); // Should be null for new borrowing
                    assertThat(borrowing.getCreatedAt()).isNotNull();
                    assertThat(borrowing.getUpdatedAt()).isNotNull();
                });

        logger.info("TC201 PASSED: Borrowing created dengan ID: " + testBorrowing.getBorrowingId());
    }

    @Test
    @Order(2)
    @DisplayName("TC202: Find borrowing by existing ID - Should Return Borrowing")
    void testFindBorrowingById_WithExistingId_ShouldReturnBorrowing() throws SQLException {
        // ACT
        Optional<Borrowing> foundBorrowing = borrowingDAO.findById(testBorrowing.getBorrowingId());

        // ASSERT
        assertThat(foundBorrowing)
                .isPresent()
                .get()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getBorrowingId()).isEqualTo(testBorrowing.getBorrowingId());
                    assertThat(borrowing.getUserId()).isEqualTo(testUser.getUserId());
                    assertThat(borrowing.getBookId()).isEqualTo(testBook.getBookId());
                });

        logger.info("TC202 PASSED: Borrowing found dengan ID: " + testBorrowing.getBorrowingId());
    }

    @Test
    @Order(3)
    @DisplayName("TC203: Find borrowings by user ID - Should Return List")
    void testFindBorrowingsByUserId_ShouldReturnList() throws SQLException {
        // ACT
        List<Borrowing> userBorrowings = borrowingDAO.findByUserId(testUser.getUserId());

        // ASSERT
        assertThat(userBorrowings)
                .isNotNull()
                .isNotEmpty()
                .anySatisfy(borrowing -> {
                    assertThat(borrowing.getUserId()).isEqualTo(testUser.getUserId());
                });

        logger.info("TC203 PASSED: Found " + userBorrowings.size() + " borrowings for user ID: " + testUser.getUserId());
    }

    @Test
    @Order(4)
    @DisplayName("TC204: Find borrowings by book ID - Should Return List")
    void testFindBorrowingsByBookId_ShouldReturnList() throws SQLException {
        // ACT
        List<Borrowing> bookBorrowings = borrowingDAO.findByBookId(testBook.getBookId());

        // ASSERT
        assertThat(bookBorrowings)
                .isNotNull()
                .isNotEmpty()
                .anySatisfy(borrowing -> {
                    assertThat(borrowing.getBookId()).isEqualTo(testBook.getBookId());
                });

        logger.info("TC204 PASSED: Found " + bookBorrowings.size() + " borrowings for book ID: " + testBook.getBookId());
    }

    @Test
    @Order(5)
    @DisplayName("TC205: Find active borrowings - Should Return Only Active")
    void testFindActiveBorrowings_ShouldReturnOnlyActive() throws SQLException {
        // ACT
        List<Borrowing> activeBorrowings = borrowingDAO.findActiveBorrowings();

        // ASSERT
        assertThat(activeBorrowings)
                .isNotNull()
                .allSatisfy(borrowing -> {
                    assertThat(borrowing.getReturnDate()).isNull(); // Active borrowings should not have return date
                });

        logger.info("TC205 PASSED: Found " + activeBorrowings.size() + " active borrowings");
    }

    @Test
    @Order(6)
    @DisplayName("TC206: Find overdue borrowings - Should Return Only Overdue")
    void testFindOverdueBorrowings_ShouldReturnOnlyOverdue() throws SQLException {
        // ARRANGE - Create an overdue borrowing
        Borrowing overdueBorrowing = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .borrowDate(Timestamp.valueOf(LocalDateTime.now().minusDays(10)))
                .dueDate(Timestamp.valueOf(LocalDateTime.now().minusDays(3))) // Overdue by 3 days
                .status("borrowed")
                .build();

        Borrowing createdOverdue = borrowingDAO.create(overdueBorrowing);

        // ACT
        List<Borrowing> overdueBorrowings = borrowingDAO.findOverdueBorrowings();

        // ASSERT
        assertThat(overdueBorrowings)
                .isNotNull()
                .isNotEmpty();

        logger.info("TC206 PASSED: Found " + overdueBorrowings.size() + " overdue borrowings");

        // CLEANUP
        borrowingDAO.delete(createdOverdue.getBorrowingId());
    }

    @Test
    @Order(7)
    @DisplayName("TC207: Return book - Should Update Return Date and Status")
    void testReturnBook_ShouldUpdateReturnDateAndStatus() throws SQLException {
        // ARRANGE
        Timestamp returnDate = new Timestamp(System.currentTimeMillis());

        // ACT
        boolean returned = borrowingDAO.returnBook(testBorrowing.getBorrowingId(), returnDate);

        // ASSERT
        assertThat(returned).isTrue();

        // VERIFY - Borrowing should be updated
        Optional<Borrowing> returnedBorrowing = borrowingDAO.findById(testBorrowing.getBorrowingId());
        assertThat(returnedBorrowing)
                .isPresent()
                .get()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getReturnDate()).isEqualTo(returnDate);
                    assertThat(borrowing.getStatus()).isEqualTo("returned");
                });

        logger.info("TC207 PASSED: Book returned successfully - Return date: " + returnDate);
    }

    @Test
    @Order(8)
    @DisplayName("TC208: Update borrowing status - Should Success")
    void testUpdateBorrowingStatus_ShouldSuccess() throws SQLException {
        // ARRANGE
        String newStatus = "overdue";

        // ACT
        boolean updated = borrowingDAO.updateStatus(testBorrowing.getBorrowingId(), newStatus);

        // ASSERT
        assertThat(updated).isTrue();

        // VERIFY
        Optional<Borrowing> updatedBorrowing = borrowingDAO.findById(testBorrowing.getBorrowingId());
        assertThat(updatedBorrowing)
                .isPresent()
                .get()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getStatus()).isEqualTo(newStatus);
                });

        logger.info("TC208 PASSED: Borrowing status updated to: " + newStatus);
    }

    @Test
    @Order(9)
    @DisplayName("TC209: Update fine amount - Should Success")
    void testUpdateFineAmount_ShouldSuccess() throws SQLException {
        // ARRANGE
        Double fineAmount = 15000.0;

        // ACT
        boolean updated = borrowingDAO.updateFineAmount(testBorrowing.getBorrowingId(), fineAmount);

        // ASSERT
        assertThat(updated).isTrue();

        // VERIFY
        Optional<Borrowing> updatedBorrowing = borrowingDAO.findById(testBorrowing.getBorrowingId());
        assertThat(updatedBorrowing)
                .isPresent()
                .get()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getFineAmount().doubleValue()).isEqualTo(fineAmount);
                });

        logger.info("TC209 PASSED: Fine amount updated to: " + fineAmount);
    }

    @Test
    @Order(10)
    @DisplayName("TC210: Delete existing borrowing - Should Success")
    void testDeleteBorrowing_WithExistingBorrowing_ShouldSuccess() throws SQLException {
        // ARRANGE - Create a new borrowing untuk di-delete
        Borrowing borrowingToDelete = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(Timestamp.valueOf(LocalDateTime.now().plusDays(7)))
                .status("borrowed")
                .build();

        borrowingToDelete = borrowingDAO.create(borrowingToDelete);
        int borrowingIdToDelete = borrowingToDelete.getBorrowingId();

        // ACT
        boolean deleted = borrowingDAO.delete(borrowingIdToDelete);

        // ASSERT
        assertThat(deleted).isTrue();

        // VERIFY - Borrowing should not exist anymore
        Optional<Borrowing> deletedBorrowing = borrowingDAO.findById(borrowingIdToDelete);
        assertThat(deletedBorrowing).isEmpty();

        logger.info("TC210 PASSED: Borrowing deleted successfully - ID: " + borrowingIdToDelete);
    }

    @Test
    @Order(11)
    @DisplayName("TC211: Count all borrowings - Should Return Correct Count")
    void testCountAllBorrowings_ShouldReturnCorrectCount() throws SQLException {
        // ACT
        int borrowingCount = borrowingDAO.countAll();

        // ASSERT
        assertThat(borrowingCount).isGreaterThanOrEqualTo(1);

        logger.info("TC211 PASSED: Total borrowings count: " + borrowingCount);
    }

    @Test
    @Order(12)
    @DisplayName("TC212: Count active borrowings by user - Should Return Correct Count")
    void testCountActiveBorrowingsByUser_ShouldReturnCorrectCount() throws SQLException {
        // ACT
        int activeBorrowings = borrowingDAO.countActiveBorrowingsByUser(testUser.getUserId());

        // ASSERT
        assertThat(activeBorrowings).isGreaterThanOrEqualTo(1);

        logger.info("TC212 PASSED: Active borrowings for user: " + activeBorrowings);
    }

    // =============================================
    // NEGATIVE TEST CASES
    // =============================================

    @Test
    @Order(20)
    @DisplayName("TC220: Find borrowing dengan non-existent ID - Should Return Empty")
    void testFindBorrowingById_WithNonExistentId_ShouldReturnEmpty() throws SQLException {
        // ACT
        Optional<Borrowing> foundBorrowing = borrowingDAO.findById(999999);

        // ASSERT
        assertThat(foundBorrowing).isEmpty();

        logger.info("TC220 PASSED: Non-existent borrowing handled correctly");
    }

    @Test
    @Order(21)
    @DisplayName("TC221: Delete non-existent borrowing - Should Return False")
    void testDeleteBorrowing_WithNonExistentBorrowing_ShouldReturnFalse() throws SQLException {
        // ACT
        boolean deleted = borrowingDAO.delete(999999);

        // ASSERT
        assertThat(deleted).isFalse();

        logger.info("TC221 PASSED: Non-existent borrowing delete handled correctly");
    }

    @Test
    @Order(22)
    @DisplayName("TC222: Return already returned book - Should Return False")
    void testReturnBook_AlreadyReturned_ShouldReturnFalse() throws SQLException {
        // ARRANGE - Return the book first
        Timestamp returnDate = new Timestamp(System.currentTimeMillis());
        borrowingDAO.returnBook(testBorrowing.getBorrowingId(), returnDate);

        // ACT - Try to return again
        boolean returnedAgain = borrowingDAO.returnBook(testBorrowing.getBorrowingId(), returnDate);

        // ASSERT
        assertThat(returnedAgain).isFalse();

        logger.info("TC222 PASSED: Already returned book cannot be returned again");
    }

    @Test
    @Order(23)
    @DisplayName("TC223: Update status dengan non-existent borrowing - Should Return False")
    void testUpdateStatus_WithNonExistentBorrowing_ShouldReturnFalse() throws SQLException {
        // ACT
        boolean updated = borrowingDAO.updateStatus(999999, "overdue");

        // ASSERT
        assertThat(updated).isFalse();

        logger.info("TC223 PASSED: Non-existent borrowing status update handled correctly");
    }

    @Test
    @Order(24)
    @DisplayName("TC224: Update fine amount dengan non-existent borrowing - Should Return False")
    void testUpdateFineAmount_WithNonExistentBorrowing_ShouldReturnFalse() throws SQLException {
        // ACT
        boolean updated = borrowingDAO.updateFineAmount(999999, 5000.0);

        // ASSERT
        assertThat(updated).isFalse();

        logger.info("TC224 PASSED: Non-existent borrowing fine update handled correctly");
    }

    // =============================================
    // BOUNDARY TEST CASES
    // =============================================

    @Test
    @Order(30)
    @DisplayName("TC230: Create borrowing dengan different status values - Should Success")
    void testCreateBorrowing_WithDifferentStatus_ShouldSuccess() throws SQLException {
        // Test semua status yang valid
        String[] validStatuses = {"borrowed", "returned", "overdue", "lost"};

        for (String status : validStatuses) {
            // ARRANGE
            Borrowing borrowing = Borrowing.builder()
                    .userId(testUser.getUserId())
                    .bookId(testBook.getBookId())
                    .dueDate(Timestamp.valueOf(LocalDateTime.now().plusDays(14)))
                    .status(status)
                    .build();

            // ACT
            Borrowing created = borrowingDAO.create(borrowing);

            // ASSERT
            assertThat(created.getStatus()).isEqualTo(status);

            // CLEANUP
            borrowingDAO.delete(created.getBorrowingId());

            logger.info("Status '" + status + "' accepted successfully");
        }

        logger.info("TC230 PASSED: All valid statuses accepted");
    }

    @Test
    @Order(31)
    @DisplayName("TC231: Create borrowing dengan notes panjang - Should Success")
    void testCreateBorrowing_WithLongNotes_ShouldSuccess() throws SQLException {
        // ARRANGE - Notes dengan panjang yang reasonable
        String longNotes = "This is a very long note for testing purposes. " +
                "It should be stored correctly in the database without any issues. " +
                "The note contains important information about the borrowing.";

        Borrowing borrowing = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(Timestamp.valueOf(LocalDateTime.now().plusDays(14)))
                .status("borrowed")
                .notes(longNotes)
                .build();

        // ACT
        Borrowing created = borrowingDAO.create(borrowing);

        // ASSERT
        assertThat(created.getNotes()).isEqualTo(longNotes);

        logger.info("TC231 PASSED: Long notes accepted successfully");

        // CLEANUP
        borrowingDAO.delete(created.getBorrowingId());
    }

    // =============================================
    // DATA CONSISTENCY TEST CASES
    // =============================================

    @Test
    @Order(40)
    @DisplayName("TC240: Data consistency after multiple operations")
    void testDataConsistency_AfterMultipleOperations() throws SQLException {
        // ARRANGE
        int initialCount = borrowingDAO.countAll();

        // ACT - Perform multiple operations
        Borrowing borrowing1 = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(Timestamp.valueOf(LocalDateTime.now().plusDays(7)))
                .status("borrowed")
                .build();

        Borrowing borrowing2 = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(Timestamp.valueOf(LocalDateTime.now().plusDays(14)))
                .status("borrowed")
                .build();

        Borrowing created1 = borrowingDAO.create(borrowing1);
        Borrowing created2 = borrowingDAO.create(borrowing2);

        boolean deleted1 = borrowingDAO.delete(created1.getBorrowingId());

        // ASSERT
        assertThat(deleted1).isTrue();

        int finalCount = borrowingDAO.countAll();
        assertThat(finalCount).isEqualTo(initialCount + 1); // +2 created, -1 deleted = +1 net

        // Verify borrowing2 still exists
        Optional<Borrowing> foundBorrowing2 = borrowingDAO.findById(created2.getBorrowingId());
        assertThat(foundBorrowing2).isPresent();

        // Verify borrowing1 deleted
        Optional<Borrowing> foundBorrowing1 = borrowingDAO.findById(created1.getBorrowingId());
        assertThat(foundBorrowing1).isEmpty();

        logger.info("TC240 PASSED: Data consistency maintained after multiple operations");
        logger.info("Initial count: " + initialCount + ", Final count: " + finalCount);

        // CLEANUP
        borrowingDAO.delete(created2.getBorrowingId());
    }

    @Test
    @Order(41)
    @DisplayName("TC241: Auto-update trigger - updated_at should change on update")
    void testAutoUpdateTrigger_UpdatedAtShouldChangeOnUpdate() throws SQLException, InterruptedException {
        // ARRANGE - Get original updated_at
        Optional<Borrowing> beforeUpdate = borrowingDAO.findById(testBorrowing.getBorrowingId());
        Timestamp originalUpdatedAt = beforeUpdate.get().getUpdatedAt();

        // Tunggu 2 detik untuk memastikan timestamp berbeda
        pause(2000);

        // ACT - Update borrowing status
        borrowingDAO.updateStatus(testBorrowing.getBorrowingId(), "overdue");

        // ASSERT - Verify updated_at changed
        Optional<Borrowing> afterUpdate = borrowingDAO.findById(testBorrowing.getBorrowingId());
        assertThat(afterUpdate)
                .isPresent()
                .get()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getUpdatedAt())
                            .isAfter(originalUpdatedAt); // updated_at harus lebih baru
                });

        logger.info("TC241 PASSED: Trigger updated_at working correctly for borrowings");
        logger.info("Before: " + originalUpdatedAt);
        logger.info("After: " + afterUpdate.get().getUpdatedAt());
    }

    // =============================================
    // PERFORMANCE TEST CASES
    // =============================================

    @Test
    @Order(50)
    @DisplayName("TC250: Find borrowing by ID - Performance Test")
    void testFindBorrowingById_Performance() throws SQLException {
        // ARRANGE
        int iterations = 10;
        long totalTime = 0;

        // ACT & MEASURE
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            borrowingDAO.findById(testBorrowing.getBorrowingId());
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        long averageTimeNano = totalTime / iterations;
        long averageTimeMs = averageTimeNano / 1_000_000;

        // ASSERT - Average time harus reasonable
        assertThat(averageTimeMs).isLessThan(100);

        logger.info("TC250 PASSED: Average query time: " + averageTimeMs + " ms");
    }

    @Test
    @Order(51)
    @DisplayName("TC251: Find borrowings by user ID - Performance Test")
    void testFindBorrowingsByUserId_Performance() throws SQLException {
        // ARRANGE
        int iterations = 5;
        long totalTime = 0;

        // ACT & MEASURE
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            borrowingDAO.findByUserId(testUser.getUserId());
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        long averageTimeNano = totalTime / iterations;
        long averageTimeMs = averageTimeNano / 1_000_000;

        // ASSERT
        assertThat(averageTimeMs).isLessThan(200);

        logger.info("TC251 PASSED: Average find by user ID time: " + averageTimeMs + " ms");
    }
}