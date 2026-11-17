package com.kexu;
import jakarta.persistence.*;
@Entity @Table(name="accounts") public class Account { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false,unique=true,length=50) public String username; @Column(nullable=false) public String password; @Column(nullable=false) public String role; }
