package com.moneyminder.app.model;

public class TipCard {

    public enum Urgency { RED, YELLOW, GREEN, BLUE, PURPLE }

    public final Urgency urgency;
    public final String icon;
    public final String header;
    public final String message;
    
    public final String savingsTag;
    
    public final String actionLabel;
    
    public final String actionKey;

    public TipCard(Urgency urgency, String icon, String header, String message,
                    String savingsTag, String actionLabel, String actionKey) {
        this.urgency     = urgency;
        this.icon        = icon;
        this.header      = header;
        this.message     = message;
        this.savingsTag  = savingsTag;
        this.actionLabel = actionLabel;
        this.actionKey   = actionKey;
    }
}
