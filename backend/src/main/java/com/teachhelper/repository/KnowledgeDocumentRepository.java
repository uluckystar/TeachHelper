package com.teachhelper.repository;

import com.teachhelper.entity.KnowledgeDocument;
import com.teachhelper.entity.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    /**
     * 根据知识库ID查询文档
     */
    List<KnowledgeDocument> findByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 根据知识库ID和状态查询文档
     */
    List<KnowledgeDocument> findByKnowledgeBaseIdAndStatus(Long knowledgeBaseId, ProcessingStatus status);

    /**
     * 根据状态查询文档
     */
    List<KnowledgeDocument> findByStatus(ProcessingStatus status);

    /**
     * 统计知识库中各状态的文档数量
     */
    @Query("SELECT d.status, COUNT(d) FROM KnowledgeDocument d WHERE d.knowledgeBaseId = :knowledgeBaseId GROUP BY d.status")
    List<Object[]> countByKnowledgeBaseIdGroupByStatus(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
     * 根据文件类型查询文档
     */
    List<KnowledgeDocument> findByFileType(String fileType);

    /**
     * 查询处理失败的文档
     */
    List<KnowledgeDocument> findByStatusAndProcessingErrorIsNotNull(ProcessingStatus status);

    /**
     * 删除知识库下所有文档
     */
    void deleteByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 根据知识库ID查询文档，按创建时间倒序
     */
    List<KnowledgeDocument> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId);

    /**
     * 根据知识库ID分页查询文档，按创建时间倒序
     */
    List<KnowledgeDocument> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId, org.springframework.data.domain.Pageable pageable);

    /**
     * 统计知识库下的文档总数
     */
    Long countByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 根据知识库ID和状态统计文档数量
     */
    Long countByKnowledgeBaseIdAndStatus(Long knowledgeBaseId, ProcessingStatus status);
}
