package com.kexu;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
interface Accounts extends JpaRepository<Account,Long> { Optional<Account> findByUsername(String username); }
interface Courses extends JpaRepository<Course,Long> { List<Course> findByOwnerId(Long ownerId); Optional<Course> findByIdAndOwnerId(Long id,Long ownerId); }
