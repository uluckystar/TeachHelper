package com.teachhelper.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Exam;
import com.teachhelper.entity.ExamStatus;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    
    List<Exam> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);
    
    Page<Exam> findByCreatedById(Long createdById, Pageable pageable);
    
    // 根据状态查询考试
    List<Exam> findByStatus(com.teachhelper.entity.ExamStatus status);
    
    @Query("SELECT e FROM Exam e WHERE e.createdBy.id = :userId AND e.title LIKE %:keyword%")
    List<Exam> findByCreatedByIdAndTitleContaining(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    @Query("SELECT COUNT(e) FROM Exam e WHERE e.createdBy.id = :userId")
    long countByCreatedById(@Param("userId") Long userId);
    
    // 查询考试详情，包含目标班级和学生信息（解决懒加载问题）
    @Query("SELECT DISTINCT e FROM Exam e " +
           "LEFT JOIN FETCH e.targetClassrooms tc " +
           "LEFT JOIN FETCH tc.students " +
           "WHERE e.id = :examId")
    Optional<Exam> findByIdWithClassroomsAndStudents(@Param("examId") Long examId);
    
    // 查询所有已发布的考试，供学生参与
    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' ORDER BY e.createdAt DESC")
    List<Exam> findAllPublishedExams();
    
    // 分页查询所有已发布的考试
    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED'")
    Page<Exam> findAllPublishedExams(Pageable pageable);
    
    // 查询所有已发布或已结束的考试，供学生查看历史考试
    @Query("SELECT e FROM Exam e WHERE (e.status = 'PUBLISHED' OR e.status = 'ENDED' OR e.status = 'EVALUATED') ORDER BY e.createdAt DESC")
    List<Exam> findAllStudentAccessibleExams();
    
    // 分页查询所有已发布或已结束的考试
    @Query("SELECT e FROM Exam e WHERE (e.status = 'PUBLISHED' OR e.status = 'ENDED' OR e.status = 'EVALUATED')")
    Page<Exam> findAllStudentAccessibleExams(Pageable pageable);
    
    // 查询指定班级的已发布考试（可参加）
    @Query("SELECT DISTINCT e FROM Exam e JOIN e.targetClassrooms c WHERE c.id = :classroomId AND e.status = 'PUBLISHED' ORDER BY e.createdAt DESC")
    List<Exam> findByTargetClassroomIdAndStatusPublished(@Param("classroomId") Long classroomId);
    
    // 分页查询指定班级的已发布考试
    @Query("SELECT DISTINCT e FROM Exam e JOIN e.targetClassrooms c WHERE c.id = :classroomId AND e.status = 'PUBLISHED'")
    Page<Exam> findByTargetClassroomIdAndStatusPublished(@Param("classroomId") Long classroomId, Pageable pageable);
    
    // 查询指定班级的所有可访问考试（已发布、已结束、已评估）
    @Query("SELECT DISTINCT e FROM Exam e JOIN e.targetClassrooms c WHERE c.id = :classroomId AND (e.status = 'PUBLISHED' OR e.status = 'ENDED' OR e.status = 'EVALUATED') ORDER BY e.createdAt DESC")
    List<Exam> findByTargetClassroomIdAndAccessible(@Param("classroomId") Long classroomId);
    
    // 查询学生可以参与的考试（学生所在班级的已发布考试）
    @Query("SELECT DISTINCT e FROM Exam e JOIN e.targetClassrooms c JOIN c.students s WHERE s.id = :studentId AND e.status = 'PUBLISHED' ORDER BY e.createdAt DESC")
    List<Exam> findAvailableExamsForStudent(@Param("studentId") Long studentId);
    
    // 分页查询学生可以参与的考试
    @Query("SELECT DISTINCT e FROM Exam e JOIN e.targetClassrooms c JOIN c.students s WHERE s.id = :studentId AND e.status = 'PUBLISHED'")
    Page<Exam> findAvailableExamsForStudent(@Param("studentId") Long studentId, Pageable pageable);
    
    // 查询学生所有可访问的考试（包括已发布、已结束、已评估）
    @Query("SELECT DISTINCT e FROM Exam e JOIN e.targetClassrooms c JOIN c.students s WHERE s.id = :studentId AND (e.status = 'PUBLISHED' OR e.status = 'ENDED' OR e.status = 'EVALUATED') ORDER BY e.createdAt DESC")
    List<Exam> findAllAccessibleExamsForStudent(@Param("studentId") Long studentId);
    
    // 分页查询学生所有可访问的考试
    @Query("SELECT DISTINCT e FROM Exam e JOIN e.targetClassrooms c JOIN c.students s WHERE s.id = :studentId AND (e.status = 'PUBLISHED' OR e.status = 'ENDED' OR e.status = 'EVALUATED')")
    Page<Exam> findAllAccessibleExamsForStudent(@Param("studentId") Long studentId, Pageable pageable);
    
    // ===== 定时任务相关方法 =====
    
    // 查找指定状态且结束时间早于指定时间的考试（用于自动结束过期考试）
    List<Exam> findByStatusAndEndTimeBefore(ExamStatus status, LocalDateTime endTime);
    
    // 查找指定状态且结束时间在指定时间范围内的考试（用于提醒即将结束的考试）
    List<Exam> findByStatusAndEndTimeBetween(ExamStatus status, LocalDateTime startTime, LocalDateTime endTime);
}