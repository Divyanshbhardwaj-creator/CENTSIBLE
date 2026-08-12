package com.moneyminder.app.model;

public class SavingsGoal {
    public final long id;
    public String name;
    public double target;
    public double saved;

    public SavingsGoal(long id, String name, double target, double saved) {
        this.id     = id;
        this.name   = name;
        this.target = target;
        this.saved  = saved;
    }

    public double progress() {
        return target > 0 ? saved / target : 0.0;
    }

    public boolean isComplete() {
        return target > 0 && saved >= target;
    }
}
