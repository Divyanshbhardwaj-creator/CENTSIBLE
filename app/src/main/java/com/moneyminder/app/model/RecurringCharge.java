package com.moneyminder.app.model;

public class RecurringCharge {
    public final String description;
    public final Category category;
    public final double monthlyAmount;
    public final double annualCost;
    public final int occurrences;

    public RecurringCharge(String description, Category category,
                            double monthlyAmount, double annualCost, int occurrences) {
        this.description   = description;
        this.category      = category;
        this.monthlyAmount = monthlyAmount;
        this.annualCost    = annualCost;
        this.occurrences   = occurrences;
    }
}
