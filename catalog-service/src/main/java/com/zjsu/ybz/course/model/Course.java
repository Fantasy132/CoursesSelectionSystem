package com.zjsu.ybz.course.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String title;
    
    @Embedded
    private Instructor instructor;
    
    @Embedded
    private ScheduleSlot schedule;
    
    @Column(nullable = false)
    private Integer capacity;
    
    @Column(nullable = false)
    private Integer enrolled = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Course(){
        this.createdAt = LocalDateTime.now();
    }

    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id = id;
    }

    public String getCode(){
        return code;
    }
    public void setCode(String code){
        this.code = code;
    }

    public String getTitle(){
        return title;
    }
    public void setTitle(String title){
        this.title = title;
    }

    public Instructor getInstructor(){
        return instructor;
    }
    public void setInstructor(Instructor instructor){
        this.instructor = instructor;
    }

    public ScheduleSlot getSchedule(){
        return schedule;
    }
    public void setSchedule(ScheduleSlot schedule){
        this.schedule = schedule;
    }

    public Integer getCapacity(){
        return capacity;
    }
    public void setCapacity(Integer capacity){
        this.capacity = capacity;
    }

    public Integer getEnrolled(){
        return enrolled;
    }
    public void setEnrolled(Integer enrolled){
        this.enrolled = enrolled;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }
    
}























