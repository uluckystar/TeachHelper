package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.SystemRubricCriterion;

/**
 * 系统级评分标准条目仓储接口
 */
@Repository
public interface SystemRubricCriterionRepository extends JpaRepository<SystemRubricCriterion, Long> {
    
    /**
     * 根据评分标准ID查找条目
     * @param systemRubricId 评分标准ID
     * @return 条目列表
     */
    List<SystemRubricCriterion> findBySystemRubricId(Long systemRubricId);
    
    /**
     * 删除指定评分标准的所有条目
     * @param systemRubricId 评分标准ID
     */
    void deleteBySystemRubricId(Long systemRubricId);
}
