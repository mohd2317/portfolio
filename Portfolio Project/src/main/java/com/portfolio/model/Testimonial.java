package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A client/colleague testimonial. */
@Entity
@Table(name = "testimonial")
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, length = 160)
    private String role;

    @Column(nullable = false, length = 1000)
    private String quote;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Testimonial() { }

    public Long getId() { return id; }
    public String getAuthor() { return author; }
    public String getRole() { return role; }
    public String getQuote() { return quote; }
    public int getDisplayOrder() { return displayOrder; }
}
