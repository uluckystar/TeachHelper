package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.EvaluationType;
import com.teachhelper.entity.StudentAnswer;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    
    List<StudentAnswer> findByQuestionIdOrderByCreatedAt(Long questionId);
    
    List<StudentAnswer> findByStudentIdOrderByCreatedAt(Long studentId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId")
    List<StudentAnswer> findByExamId(@Param("examId") Long examId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.isEvaluated = :isEvaluated")
    List<StudentAnswer> findByExamIdAndEvaluated(@Param("examId") Long examId, @Param("isEvaluated") boolean isEvaluated);
    
    @Query(value = "SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId", 
           countQuery = "SELECT count(sa) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId")
    Page<StudentAnswer> findByExamId(@Param("examId") Long examId, Pageable pageable);
    
    @Query("SELECT COUNT(sa) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.isEvaluated = false")
    long countUnevaluatedByExamId(@Param("examId") Long examId);
    
    @Query("SELECT COUNT(sa) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.evaluationType = :evaluationType")
    long countByExamIdAndEvaluationType(@Param("examId") Long examId, @Param("evaluationType") EvaluationType evaluationType);
    
    boolean existsByQuestionIdAndStudentId(Long questionId, Long studentId);
    
    // 缺失的方法补充
    List<StudentAnswer> findByQuestionId(Long questionId);
    
    List<StudentAnswer> findByStudentId(Long studentId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId")
    List<StudentAnswer> findByQuestionExamId(@Param("examId") Long examId);
    
    List<StudentAnswer> findByIsEvaluatedFalse();
    
    List<StudentAnswer> findByIsEvaluated(boolean isEvaluated);
    
    List<StudentAnswer> findByQuestionIdAndIsEvaluatedFalse(Long questionId);
    
    List<StudentAnswer> findByQuestionIdAndIsEvaluated(Long questionId, boolean isEvaluated);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.isEvaluated = :isEvaluated")
    List<StudentAnswer> findByQuestionExamIdAndIsEvaluated(@Param("examId") Long examId, @Param("isEvaluated") boolean isEvaluated);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.question.id = :questionId")
    List<StudentAnswer> findByQuestionExamIdAndQuestionId(@Param("examId") Long examId, @Param("questionId") Long questionId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.question.id = :questionId AND sa.isEvaluated = :isEvaluated")
    List<StudentAnswer> findByQuestionExamIdAndQuestionIdAndIsEvaluated(@Param("examId") Long examId, @Param("questionId") Long questionId, @Param("isEvaluated") boolean isEvaluated);
    
    long countByQuestionId(Long questionId);
    
    long countByQuestionIdAndIsEvaluatedTrue(Long questionId);
    
    @Query("SELECT AVG(sa.score) FROM StudentAnswer sa WHERE sa.question.id = :questionId AND sa.isEvaluated = true")
    double findAverageScoreByQuestionId(@Param("questionId") Long questionId);
    
    // Exam-level methods
    @Query("SELECT COUNT(sa) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId")
    long countByQuestionExamId(@Param("examId") Long examId);
    
    @Query("SELECT COUNT(sa) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.isEvaluated = true")
    long countByQuestionExamIdAndIsEvaluatedTrue(@Param("examId") Long examId);
    
    @Query("SELECT AVG(sa.score) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.isEvaluated = true")
    double findAverageScoreByExamId(@Param("examId") Long examId);

    @Query("SELECT MAX(sa.score) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.isEvaluated = true")
    Double findMaxScoreByExamId(@Param("examId") Long examId);

    @Query("SELECT MIN(sa.score) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.isEvaluated = true")
    Double findMinScoreByExamId(@Param("examId") Long examId);

    boolean existsByStudentStudentIdAndQuestionId(String studentId, Long questionId);
    
    // 查找现有的学生答案（用于更新）
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.student.studentId = :studentId AND sa.question.id = :questionId")
    StudentAnswer findByStudentStudentIdAndQuestionId(@Param("studentId") String studentId, @Param("questionId") Long questionId);
    
    @Query("SELECT COUNT(DISTINCT sa.student.id) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId")
    long countDistinctStudentByQuestionExamId(@Param("examId") Long examId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.student.id = :studentId ORDER BY sa.createdAt")
    List<StudentAnswer> findByQuestionExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") Long studentId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.student.studentId = :studentId ORDER BY sa.createdAt")
    List<StudentAnswer> findByQuestionExamIdAndStudentStudentId(@Param("examId") Long examId, @Param("studentId") String studentId);
    
    // 带FETCH JOIN的查询方法，用于批量评估时避免LazyInitializationException
    @Query("SELECT sa FROM StudentAnswer sa " +
           "JOIN FETCH sa.question q " +
           "LEFT JOIN FETCH q.rubricCriteria " +
           "JOIN FETCH sa.student s " +
           "WHERE sa.id IN :answerIds")
    List<StudentAnswer> findByIdInWithFetch(@Param("answerIds") List<Long> answerIds);
    
    @Query("SELECT sa FROM StudentAnswer sa " +
           "JOIN FETCH sa.question q " +
           "LEFT JOIN FETCH q.rubricCriteria " +
           "JOIN FETCH sa.student s " +
           "WHERE sa.id = :answerId")
    StudentAnswer findByIdWithFetch(@Param("answerId") Long answerId);
    
    // 检查学生是否已提交指定考试的方法
    @Query("SELECT COUNT(sa) > 0 FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.student.studentId = :studentId")
    boolean existsByQuestionExamIdAndStudentStudentId(@Param("examId") Long examId, @Param("studentId") String studentId);
    
    // 通过 users.username 查询学生答案 (使用原生SQL)
    @Query(value = "SELECT sa.* FROM student_answers sa " +
                   "JOIN students s ON sa.student_id = s.id " +
                   "JOIN users u ON s.student_id = u.id " +
                   "JOIN questions q ON sa.question_id = q.id " +
                   "WHERE q.exam_id = :examId AND u.username = :username " +
                   "ORDER BY sa.created_at", 
           nativeQuery = true)
    List<StudentAnswer> findByQuestionExamIdAndUsername(@Param("examId") Long examId, @Param("username") String username);
}
