package com.zjsu.ybz.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ScheduleSlot {
    @Column(name = "schedule_day_of_week")
    private String dayOfWeek;
    
    @Column(name = "schedule_start_time")
    private String startTime;
    
    @Column(name = "schedule_end_time")
    private String endTime;
    
    @Column(name = "schedule_expected_attendance")
    private Integer expectedAttendance;

    public String getDayOfWeek() {
        return dayOfWeek;
    }
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getExpectedAttendance() {
        return expectedAttendance;
    }
    public void setExpectedAttendance(Integer expectedAttendance) {
        this.expectedAttendance = expectedAttendance;
    }
}
