package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.RubricCriterion;

@Repository
public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, Long> {
    
    List<RubricCriterion> findByQuestionIdOrderByOrderIndex(Long questionId);
    
    List<RubricCriterion> findByQuestionId(Long questionId);
    
    void deleteByQuestionId(Long questionId);
}
