package com.teachhelper.repository;

import com.teachhelper.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    
    List<KnowledgeBase> findByCreatedByAndIsActiveTrue(Long createdBy);
    
    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.createdBy = :userId AND kb.isActive = true ORDER BY kb.updatedAt DESC")
    Page<KnowledgeBase> findActiveByUserOrderByUpdatedDesc(@Param("userId") Long userId, Pageable pageable);
    
    boolean existsByNameAndCreatedBy(String name, Long createdBy);
    
    Optional<KnowledgeBase> findByIdAndCreatedByAndIsActiveTrue(Long id, Long createdBy);
    
    /**
     * 获取所有不同的科目
     */
    @Query("SELECT DISTINCT kb.subject FROM KnowledgeBase kb WHERE kb.isActive = true AND kb.subject IS NOT NULL ORDER BY kb.subject")
    List<String> findDistinctSubjects();
    
    /**
     * 获取所有不同的年级
     */
    @Query("SELECT DISTINCT kb.gradeLevel FROM KnowledgeBase kb WHERE kb.isActive = true AND kb.gradeLevel IS NOT NULL ORDER BY kb.gradeLevel")
    List<String> findDistinctGradeLevels();
    
    /**
     * 统计指定学科的知识库数量
     */
    long countBySubjectAndIsActiveTrue(String subject);
    
    /**
     * 统计指定年级的知识库数量
     */
    long countByGradeLevelAndIsActiveTrue(String gradeLevel);

    /**
     * 根据条件搜索知识库（支持动态排序）
     */
    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.createdBy = :userId AND kb.isActive = true " +
           "AND (:subject IS NULL OR kb.subject = :subject) " +
           "AND (:gradeLevel IS NULL OR kb.gradeLevel = :gradeLevel) " +
           "AND (:name IS NULL OR LOWER(kb.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<KnowledgeBase> findByConditions(@Param("userId") Long userId, 
                                       @Param("subject") String subject,
                                       @Param("gradeLevel") String gradeLevel,
                                       @Param("name") String name,
                                       Pageable pageable);

    /**
     * 根据条件搜索知识库（支持文档数量排序）
     */
    @Query(value = "SELECT kb, " +
           "(SELECT COUNT(d) FROM KnowledgeDocument d WHERE d.knowledgeBaseId = kb.id) as docCount " +
           "FROM KnowledgeBase kb WHERE kb.createdBy = :userId AND kb.isActive = true " +
           "AND (:subject IS NULL OR kb.subject = :subject) " +
           "AND (:gradeLevel IS NULL OR kb.gradeLevel = :gradeLevel) " +
           "AND (:name IS NULL OR LOWER(kb.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "ORDER BY " +
           "CASE WHEN :direction = 'desc' THEN " +
           "(SELECT COUNT(d2) FROM KnowledgeDocument d2 WHERE d2.knowledgeBaseId = kb.id) " +
           "END DESC, " +
           "CASE WHEN :direction = 'asc' THEN " +
           "(SELECT COUNT(d3) FROM KnowledgeDocument d3 WHERE d3.knowledgeBaseId = kb.id) " +
           "END ASC",
           countQuery = "SELECT COUNT(kb) FROM KnowledgeBase kb WHERE kb.createdBy = :userId AND kb.isActive = true " +
           "AND (:subject IS NULL OR kb.subject = :subject) " +
           "AND (:gradeLevel IS NULL OR kb.gradeLevel = :gradeLevel) " +
           "AND (:name IS NULL OR LOWER(kb.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Object[]> findByConditionsOrderByDocumentCount(@Param("userId") Long userId, 
                                                       @Param("subject") String subject,
                                                       @Param("gradeLevel") String gradeLevel,
                                                       @Param("name") String name,
                                                       @Param("direction") String direction,
                                                       Pageable pageable);

    /**
     * 根据条件搜索知识库（支持知识点数量排序）
     */
    @Query(value = "SELECT kb, " +
           "(SELECT COUNT(kp) FROM KnowledgePoint kp WHERE kp.knowledgeBaseId = kb.id) as kpCount " +
           "FROM KnowledgeBase kb WHERE kb.createdBy = :userId AND kb.isActive = true " +
           "AND (:subject IS NULL OR kb.subject = :subject) " +
           "AND (:gradeLevel IS NULL OR kb.gradeLevel = :gradeLevel) " +
           "AND (:name IS NULL OR LOWER(kb.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "ORDER BY " +
           "CASE WHEN :direction = 'desc' THEN " +
           "(SELECT COUNT(kp2) FROM KnowledgePoint kp2 WHERE kp2.knowledgeBaseId = kb.id) " +
           "END DESC, " +
           "CASE WHEN :direction = 'asc' THEN " +
           "(SELECT COUNT(kp3) FROM KnowledgePoint kp3 WHERE kp3.knowledgeBaseId = kb.id) " +
           "END ASC",
           countQuery = "SELECT COUNT(kb) FROM KnowledgeBase kb WHERE kb.createdBy = :userId AND kb.isActive = true " +
           "AND (:subject IS NULL OR kb.subject = :subject) " +
           "AND (:gradeLevel IS NULL OR kb.gradeLevel = :gradeLevel) " +
           "AND (:name IS NULL OR LOWER(kb.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Object[]> findByConditionsOrderByKnowledgePointCount(@Param("userId") Long userId, 
                                                             @Param("subject") String subject,
                                                             @Param("gradeLevel") String gradeLevel,
                                                             @Param("name") String name,
                                                             @Param("direction") String direction,
                                                             Pageable pageable);
}
