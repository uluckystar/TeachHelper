package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.ScoreLevel;

/**
 * 评分级别仓储接口
 */
@Repository
public interface ScoreLevelRepository extends JpaRepository<ScoreLevel, Long> {
    
    /**
     * 根据评分标准条目ID查找评分级别
     * @param criterionId 条目ID
     * @return 评分级别列表
     */
    List<ScoreLevel> findByCriterionId(Long criterionId);
    
    /**
     * 删除指定条目的所有评分级别
     * @param criterionId 条目ID
     */
    void deleteByCriterionId(Long criterionId);
}
