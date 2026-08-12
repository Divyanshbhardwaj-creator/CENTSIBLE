package com.moneyminder.app.model;

public class Transaction {
    public final long   id;
    public final double amount;
    public final Category category;
    public final String description;
    public final long   timestamp;

    public Transaction(long id, double amount, Category category,
                       String description, long timestamp) {
        this.id          = id;
        this.amount      = amount;
        this.category    = category;
        this.description = description;
        this.timestamp   = timestamp;
    }
}
