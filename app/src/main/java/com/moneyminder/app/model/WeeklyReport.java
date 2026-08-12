package com.moneyminder.app.model;

import java.util.List;
import java.util.Map;

public class WeeklyReport {
    public final Map<Category, Double> byCategory;
    public final double total;
    public final double totalBudget;
    public final Category topCategory;
    public final List<String> tips;
    public final List<TipCard> tipCards;
    public final List<TipCard> storyCards;
    public final int streakWeeks;

    public WeeklyReport(Map<Category, Double> byCategory, double total,
                        double totalBudget, Category topCategory,
                        List<String> tips, List<TipCard> tipCards,
                        List<TipCard> storyCards, int streakWeeks) {
        this.byCategory   = byCategory;
        this.total        = total;
        this.totalBudget  = totalBudget;
        this.topCategory  = topCategory;
        this.tips         = tips;
        this.tipCards     = tipCards;
        this.storyCards   = storyCards;
        this.streakWeeks  = streakWeeks;
    }
}
