package com.moneyminder.app.model;

public enum Category {
    FOOD("Food",           "🍕", 0xFF2FBFAA),
    ENTERTAINMENT("Entertainment", "🎬", 0xFFFF6B9D),
    TRANSPORT("Transport", "🚗", 0xFF4285F4),
    SHOPPING("Shopping",   "🛍️", 0xFF7B5EA7),
    SCHOOL("School",       "📚", 0xFFFF8C42),
    SUBSCRIPTIONS("Subscriptions", "📱", 0xFFFF6B5E),
    OTHER("Other",         "💼", 0xFF9498B3);

    public final String label;
    public final String emoji;
    public final int color;

    Category(String label, String emoji, int color) {
        this.label = label;
        this.emoji = emoji;
        this.color = color;
    }
}
