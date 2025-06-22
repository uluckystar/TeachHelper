package com.teachhelper.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    Optional<Student> findByStudentId(String studentId);
    
    Optional<Student> findByEmail(String email);
    
    boolean existsByStudentId(String studentId);
    
    boolean existsByEmail(String email);
}
