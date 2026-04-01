package com.example.fatblur.models;

public class SpecialDay {
    public String specialDayId;
    public String note;
    public boolean isSpecial;
    public boolean isRepeat;
    public long updatedAt;
    public String createdBy;

    public SpecialDay() {

    }

    public SpecialDay(boolean isSpecial, boolean isRepeat, String note, long updatedAt, String createdBy) {
        this.isSpecial = isSpecial;
        this.isRepeat = isRepeat;
        this.note = note;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
    }
}