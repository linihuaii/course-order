package com.kexu;
import jakarta.persistence.*;import jakarta.validation.constraints.*;
@Entity public class Course { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false) public Long ownerId; @NotBlank @Size(max=100) public String name; @Size(max=100) public String teacher; @Size(max=100) public String location; @Min(1) @Max(7) public int weekday; @Min(1) @Max(12) public int startPeriod; @Min(1) @Max(12) public int endPeriod; @Min(1) @Max(30) public int startWeek; @Min(1) @Max(30) public int endWeek; @Size(max=30) public String color; }
