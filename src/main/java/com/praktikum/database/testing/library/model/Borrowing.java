package com.praktikum.database.testing.library.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Borrowing {
    private Integer borrowingId;
    private Integer userId;
    private Integer bookId;
    private Timestamp borrowDate;
    private Timestamp dueDate;
    private Timestamp returnDate;
    private String status;
    private BigDecimal fineAmount;
    private Boolean finePaid;
    private String notes;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}