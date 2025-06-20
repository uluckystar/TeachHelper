package com.teachhelper.repository;

import com.teachhelper.entity.SubjectGradeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学科年级关联Repository
 */
@Repository
public interface SubjectGradeMappingRepository extends JpaRepository<SubjectGradeMapping, Long> {
    
    /**
     * 根据年级ID获取推荐学科（按优先级排序）
     */
    @Query("SELECT sgm FROM SubjectGradeMapping sgm " +
           "JOIN sgm.subject s " +
           "WHERE sgm.gradeLevelId = :gradeLevelId " +
           "AND sgm.isRecommended = true " +
           "AND s.isActive = true " +
           "ORDER BY sgm.priority ASC, s.name ASC")
    List<SubjectGradeMapping> findRecommendedSubjectsByGradeLevelId(@Param("gradeLevelId") Long gradeLevelId);
    
    /**
     * 根据年级名称获取推荐学科（按优先级排序）
     */
    @Query("SELECT sgm FROM SubjectGradeMapping sgm " +
           "JOIN sgm.subject s " +
           "JOIN sgm.gradeLevel gl " +
           "WHERE gl.name = :gradeLevelName " +
           "AND sgm.isRecommended = true " +
           "AND s.isActive = true " +
           "AND gl.isActive = true " +
           "ORDER BY sgm.priority ASC, s.name ASC")
    List<SubjectGradeMapping> findRecommendedSubjectsByGradeLevelName(@Param("gradeLevelName") String gradeLevelName);
    
    /**
     * 根据学科ID获取关联的年级
     */
    @Query("SELECT sgm FROM SubjectGradeMapping sgm " +
           "JOIN sgm.gradeLevel gl " +
           "WHERE sgm.subjectId = :subjectId " +
           "AND gl.isActive = true " +
           "ORDER BY gl.sortOrder ASC, gl.name ASC")
    List<SubjectGradeMapping> findGradeLevelsBySubjectId(@Param("subjectId") Long subjectId);
    
    /**
     * 根据学科ID和年级ID查找关联
     */
    Optional<SubjectGradeMapping> findBySubjectIdAndGradeLevelId(Long subjectId, Long gradeLevelId);
    
    /**
     * 批量删除学科的所有年级关联
     */
    void deleteBySubjectId(Long subjectId);
    
    /**
     * 批量删除年级的所有学科关联
     */
    void deleteByGradeLevelId(Long gradeLevelId);
    
    /**
     * 检查学科和年级是否已关联
     */
    boolean existsBySubjectIdAndGradeLevelId(Long subjectId, Long gradeLevelId);
    
    /**
     * 获取年级类别下的推荐学科
     */
    @Query("SELECT sgm FROM SubjectGradeMapping sgm " +
           "JOIN sgm.subject s " +
           "JOIN sgm.gradeLevel gl " +
           "WHERE gl.category = :category " +
           "AND sgm.isRecommended = true " +
           "AND s.isActive = true " +
           "AND gl.isActive = true " +
           "ORDER BY sgm.priority ASC, s.name ASC")
    List<SubjectGradeMapping> findRecommendedSubjectsByCategory(@Param("category") String category);
}
