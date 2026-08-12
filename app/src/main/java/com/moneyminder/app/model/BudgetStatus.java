package com.moneyminder.app.model;

public class BudgetStatus {

    public enum State { OK, WARNING, OVER }

    public final Category category;
    public final double spent;
    public final double limit;
    public final float  pct;
    public final State  state;

    public BudgetStatus(Category category, double spent, double limit) {
        this.category = category;
        this.spent    = spent;
        this.limit    = limit;
        this.pct      = (limit > 0) ? (float)(spent / limit) : 0f;
        if      (pct >= 1f)   this.state = State.OVER;
        else if (pct >= 0.8f) this.state = State.WARNING;
        else                  this.state = State.OK;
    }
}
