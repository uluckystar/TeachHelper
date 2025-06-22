package com.teachhelper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Classroom;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    
    /**
     * 根据班级代码查找班级
     */
    Optional<Classroom> findByClassCode(String classCode);
    
    /**
     * 查找指定教师创建的所有班级
     */
    List<Classroom> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);
    
    /**
     * 查找所有班级（管理员用）
     */
    List<Classroom> findAllByOrderByCreatedAtDesc();
    
    /**
     * 查找学生所在的所有班级
     */
    @Query("SELECT c FROM Classroom c JOIN c.students s WHERE s.id = :studentId ORDER BY c.createdAt DESC")
    List<Classroom> findByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 检查班级代码是否已存在
     */
    boolean existsByClassCode(String classCode);
    
    /**
     * 根据班级名称模糊搜索（仅限教师自己创建的班级）
     */
    @Query("SELECT c FROM Classroom c WHERE c.createdBy.id = :createdById AND c.name LIKE %:name%")
    List<Classroom> findByCreatedByIdAndNameContaining(@Param("createdById") Long createdById, @Param("name") String name);
    
    /**
     * 统计教师创建的班级数量
     */
    @Query("SELECT COUNT(c) FROM Classroom c WHERE c.createdBy.id = :createdById")
    long countByCreatedById(@Param("createdById") Long createdById);
}
