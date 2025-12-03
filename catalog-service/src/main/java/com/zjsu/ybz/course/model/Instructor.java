package com.zjsu.ybz.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Instructor {
    @Column(name = "instructor_id")
    private String id;
    
    @Column(name = "instructor_name")
    private String name;
    
    @Column(name = "instructor_email")
    private String email;

    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id = id;
    }

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }
}
