package com.teachhelper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.SystemRubric;

/**
 * 系统级评分标准仓储接口
 */
@Repository
public interface SystemRubricRepository extends JpaRepository<SystemRubric, Long> {
    
    /**
     * 查找所有评分标准，按创建时间倒序
     * @return 评分标准列表
     */
    @Query("SELECT r FROM SystemRubric r ORDER BY r.createdAt DESC")
    List<SystemRubric> findAllOrderByCreatedAtDesc();
    
    /**
     * 根据ID查找评分标准并预加载关联的criteria
     * @param id 评分标准ID
     * @return 评分标准
     */
    @Query("SELECT r FROM SystemRubric r " +
           "LEFT JOIN FETCH r.criteria " +
           "WHERE r.id = :id")
    Optional<SystemRubric> findByIdWithCriteria(@Param("id") Long id);
    
    /**
     * 查找激活状态的评分标准
     * @param isActive 是否激活
     * @return 评分标准列表
     */
    List<SystemRubric> findByIsActive(Boolean isActive);
    
    /**
     * 根据学科查找评分标准
     * @param subject 学科
     * @return 评分标准列表
     */
    List<SystemRubric> findBySubject(String subject);
    
    /**
     * 根据名称模糊查找评分标准
     * @param name 名称关键字
     * @return 评分标准列表
     */
    List<SystemRubric> findByNameContainingIgnoreCase(String name);
}
