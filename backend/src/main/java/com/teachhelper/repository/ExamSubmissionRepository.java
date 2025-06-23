package com.teachhelper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.ExamSubmission;

@Repository
public interface ExamSubmissionRepository extends JpaRepository<ExamSubmission, Long> {
    
    /**
     * 检查学生是否已提交指定考试
     */
    boolean existsByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") Long studentId);
    
    /**
     * 检查学生是否已提交指定考试（通过studentNumber字符串）
     */
    @Query("SELECT COUNT(es) > 0 FROM ExamSubmission es WHERE es.exam.id = :examId AND es.student.studentNumber = :studentNumber")
    boolean existsByExamIdAndStudentNumber(@Param("examId") Long examId, @Param("studentNumber") String studentNumber);
    
    /**
     * 获取学生的考试提交记录
     */
    Optional<ExamSubmission> findByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") Long studentId);
    
    /**
     * 获取学生的考试提交记录（通过studentNumber字符串）
     */
    @Query("SELECT es FROM ExamSubmission es WHERE es.exam.id = :examId AND es.student.studentNumber = :studentNumber")
    Optional<ExamSubmission> findByExamIdAndStudentNumber(@Param("examId") Long examId, @Param("studentNumber") String studentNumber);
    
    /**
     * 获取指定考试的所有提交记录
     */
    List<ExamSubmission> findByExamId(@Param("examId") Long examId);
    
    /**
     * 获取指定学生的所有提交记录
     */
    List<ExamSubmission> findByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 统计指定考试的提交人数
     */
    @Query("SELECT COUNT(es) FROM ExamSubmission es WHERE es.exam.id = :examId")
    long countByExamId(@Param("examId") Long examId);
    
    /**
     * 统计自动提交的数量
     */
    @Query("SELECT COUNT(es) FROM ExamSubmission es WHERE es.exam.id = :examId AND es.autoSubmitted = true")
    long countAutoSubmittedByExamId(@Param("examId") Long examId);
}
