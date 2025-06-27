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
import com.teachhelper.entity.Exam;
import com.teachhelper.entity.User;

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
    
    // 支持分页和筛选的查询方法
    @Query("SELECT sa FROM StudentAnswer sa " +
           "WHERE sa.question.exam.id = :examId " +
           "AND (:questionId IS NULL OR sa.question.id = :questionId) " +
           "AND (:evaluated IS NULL OR sa.isEvaluated = :evaluated) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(sa.student.realName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(sa.student.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(sa.student.studentNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<StudentAnswer> findByExamIdWithFilters(
        @Param("examId") Long examId,
        @Param("questionId") Long questionId,
        @Param("evaluated") Boolean evaluated,
        @Param("keyword") String keyword,
        Pageable pageable
    );
    
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

    // Updated methods to work with User entity directly
    boolean existsByStudentIdAndQuestionId(Long studentId, Long questionId);
    
    // 查找现有的学生答案（用于更新） - 现在使用User.id
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.student.id = :studentId AND sa.question.id = :questionId")
    StudentAnswer findByStudentIdAndQuestionId(@Param("studentId") Long studentId, @Param("questionId") Long questionId);
    
    // 通过学号查找答案（向后兼容）
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.student.studentNumber = :studentNumber AND sa.question.id = :questionId")
    StudentAnswer findByStudentNumberAndQuestionId(@Param("studentNumber") String studentNumber, @Param("questionId") Long questionId);
    
    @Query("SELECT COUNT(DISTINCT sa.student.id) FROM StudentAnswer sa WHERE sa.question.exam.id = :examId")
    long countDistinctStudentByQuestionExamId(@Param("examId") Long examId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.student.id = :studentId ORDER BY sa.createdAt")
    List<StudentAnswer> findByQuestionExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") Long studentId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.student.studentNumber = :studentNumber ORDER BY sa.createdAt")
    List<StudentAnswer> findByQuestionExamIdAndStudentNumber(@Param("examId") Long examId, @Param("studentNumber") String studentNumber);
    
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
    @Query("SELECT COUNT(sa) > 0 FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.student.studentNumber = :studentNumber")
    boolean existsByQuestionExamIdAndStudentNumber(@Param("examId") Long examId, @Param("studentNumber") String studentNumber);
    
    // 通过 users.username 查询学生答案 (使用原生SQL)
    @Query(value = "SELECT sa.* FROM student_answers sa " +
                   "JOIN students s ON sa.student_id = s.id " +
                   "JOIN users u ON s.student_id = u.id " +
                   "JOIN questions q ON sa.question_id = q.id " +
                   "WHERE q.exam_id = :examId AND u.username = :username " +
                   "ORDER BY sa.created_at", 
           nativeQuery = true)
    List<StudentAnswer> findByQuestionExamIdAndUsername(@Param("examId") Long examId, @Param("username") String username);
    
    // 按学生分组查询的方法
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId ORDER BY sa.student.id ASC, sa.question.id ASC")
    List<StudentAnswer> findByQuestionExamIdOrderByStudentIdAscQuestionIdAsc(@Param("examId") Long examId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND sa.student.id = :studentId ORDER BY sa.question.id ASC")
    List<StudentAnswer> findByQuestionExamIdAndStudentIdOrderByQuestionIdAsc(@Param("examId") Long examId, @Param("studentId") Long studentId);
    
    // 删除指定考试和学生的所有答案
    void deleteByExamAndStudent(Exam exam, User student);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.question.exam.id = :examId AND " +
           "(LOWER(sa.student.realName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(sa.student.studentNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY sa.student.id ASC, sa.question.id ASC")
    List<StudentAnswer> findByQuestionExamIdAndStudentNameContainingOrStudentStudentNumberContaining(
        @Param("examId") Long examId, 
        @Param("keyword") String keyword1, 
        @Param("keyword") String keyword2
    );
    
    // 分页查询学生试卷信息
    @Query(value = "SELECT DISTINCT " +
           "s.id as studentId, " +
           "s.real_name as studentName, " +
           "s.student_number as studentNumber, " +
           "s.email as studentEmail, " +
           "(SELECT COUNT(DISTINCT q.id) FROM questions q WHERE q.exam_id = :examId) as totalQuestions, " +
           "(SELECT COUNT(DISTINCT sa2.question_id) FROM student_answers sa2 WHERE sa2.student_id = s.id AND sa2.question_id IN (SELECT id FROM questions WHERE exam_id = :examId)) as answeredQuestions, " +
           "(SELECT COUNT(*) FROM student_answers sa3 WHERE sa3.student_id = s.id AND sa3.is_evaluated = true AND sa3.question_id IN (SELECT id FROM questions WHERE exam_id = :examId)) as evaluatedAnswers " +
           "FROM student_answers sa " +
           "JOIN users s ON sa.student_id = s.id " +
           "JOIN questions q ON sa.question_id = q.id " +
           "WHERE q.exam_id = :examId " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(s.real_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(s.student_number) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY s.id",
           countQuery = "SELECT COUNT(DISTINCT s.id) " +
                       "FROM student_answers sa " +
                       "JOIN users s ON sa.student_id = s.id " +
                       "JOIN questions q ON sa.question_id = q.id " +
                       "WHERE q.exam_id = :examId " +
                       "AND (:keyword IS NULL OR :keyword = '' OR " +
                       "     LOWER(s.real_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                       "     LOWER(s.student_number) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           nativeQuery = true)
    Page<Object[]> findStudentPapersPagedByExamId(@Param("examId") Long examId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 查询考试中所有提交了答案的学生（不分页）
     */
    @Query(value = "SELECT DISTINCT s.id, " +
                       "COALESCE(s.real_name, s.username) as student_name, " +
                       "s.student_number, " +
                       "s.email, " +
                       "s.real_name " +
                       "FROM student_answers sa " +
                       "JOIN users s ON sa.student_id = s.id " +
                       "JOIN questions q ON sa.question_id = q.id " +
                       "WHERE q.exam_id = :examId " +
                       "ORDER BY s.student_number ASC, s.real_name ASC",
           nativeQuery = true)
    List<Object[]> findAllStudentsByExamId(@Param("examId") Long examId);
}
