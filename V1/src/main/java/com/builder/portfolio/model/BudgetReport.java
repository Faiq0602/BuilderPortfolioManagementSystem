package com.builder.portfolio.model;

public class BudgetReport {
    private double planned;
    private double actual;
    private double variance;
    private String health;

    public double getPlanned() {
        return planned;
    }

    public void setPlanned(double planned) {
        this.planned = planned;
    }

    public double getActual() {
        return actual;
    }

    public void setActual(double actual) {
        this.actual = actual;
    }

    public double getVariance() {
        return variance;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }
}
