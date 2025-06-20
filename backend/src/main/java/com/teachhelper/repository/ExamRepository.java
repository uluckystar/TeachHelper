package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Exam;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    
    List<Exam> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);
    
    Page<Exam> findByCreatedById(Long createdById, Pageable pageable);
    
    @Query("SELECT e FROM Exam e WHERE e.createdBy.id = :userId AND e.title LIKE %:keyword%")
    List<Exam> findByCreatedByIdAndTitleContaining(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    @Query("SELECT COUNT(e) FROM Exam e WHERE e.createdBy.id = :userId")
    long countByCreatedById(@Param("userId") Long userId);
}