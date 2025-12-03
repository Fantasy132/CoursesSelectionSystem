package com.zjsu.ybz.course.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false, name = "student_id")
    private String studentId;
    
    @Column(nullable = false)
    private String name;
    
    @Column
    private String major;
    
    @Column
    private Integer grade;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Student() {
        this.createdAt = LocalDateTime.now();
    }

    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id = id;
    }

    public String getStudentId(){
        return studentId;
    }
    public void setStudentId(String studentId){
        this.studentId = studentId;
    }

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }

    public String getMajor(){
        return major;
    }
    public void setMajor(String major){
        this.major = major;
    }

    public Integer getGrade(){
        return grade;
    }
    public void setGrade(Integer grade){
        this.grade = grade;
    }

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }
}




















