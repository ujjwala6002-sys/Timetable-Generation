package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"school_day", "period_number"}))
public class TimeSlot {

    public enum Day { MON, TUE, WED, THU, FRI }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Day is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "school_day", nullable = false)
    private Day day;

    @Min(1)
    @Max(6)
    @Column(name = "period_number", nullable = false)
    private int period;

    protected TimeSlot() {
    }

    public TimeSlot(Day day, int period) {
        this.day = day;
        this.period = period;
    }

    public Long getId() { return id; }
    public Day getDay() { return day; }
    public void setDay(Day day) { this.day = day; }
    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }
}
