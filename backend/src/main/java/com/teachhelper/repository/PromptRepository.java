package com.teachhelper.repository;

import com.teachhelper.entity.Prompt;
import com.teachhelper.enums.PromptName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 提示词Repository
 */
@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {
    
    /**
     * 根据名称查找激活的提示词
     */
    @Query("SELECT p FROM Prompt p WHERE p.name = :name AND p.isActive = true")
    Optional<Prompt> findActivePromptByName(@Param("name") PromptName name);
    
    /**
     * 查找所有激活的提示词
     */
    @Query("SELECT p FROM Prompt p WHERE p.isActive = true ORDER BY p.name")
    List<Prompt> findAllActivePrompts();
    
    /**
     * 根据名称查找所有版本的提示词
     */
    @Query("SELECT p FROM Prompt p WHERE p.name = :name ORDER BY p.version DESC")
    List<Prompt> findAllVersionsByName(@Param("name") PromptName name);
    
    /**
     * 查找使用次数最多的提示词
     */
    @Query("SELECT p FROM Prompt p WHERE p.isActive = true ORDER BY p.usageCount DESC")
    List<Prompt> findMostUsedPrompts();
    
    /**
     * 根据版本查找提示词
     */
    @Query("SELECT p FROM Prompt p WHERE p.name = :name AND p.version = :version")
    Optional<Prompt> findByNameAndVersion(@Param("name") PromptName name, @Param("version") String version);
}