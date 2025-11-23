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
public class Book {
    private Integer bookId;
    private String isbn;
    private String title;
    private Integer authorId;
    private Integer publisherId;
    private Integer categoryId;
    private Integer publicationYear;
    private Integer pages;
    private String language;
    private String description;
    private Integer totalCopies;
    private Integer availableCopies;
    private BigDecimal price;
    private String location;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}