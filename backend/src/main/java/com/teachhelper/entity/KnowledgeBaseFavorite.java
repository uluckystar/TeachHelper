package com.teachhelper.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 知识库收藏实体
 */
@Entity
@Table(name = "knowledge_base_favorites")
@Data
@EqualsAndHashCode(callSuper = false)
public class KnowledgeBaseFavorite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // 添加唯一约束，防止重复收藏
    @Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "knowledge_base_id"})
    })
    static class TableConstraints {
    }
}
