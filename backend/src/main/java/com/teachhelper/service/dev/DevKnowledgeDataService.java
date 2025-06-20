package com.teachhelper.service.dev;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.DifficultyLevel;
import com.teachhelper.entity.KnowledgeBase;
import com.teachhelper.entity.KnowledgeBaseFavorite;
import com.teachhelper.entity.KnowledgeDocument;
import com.teachhelper.entity.KnowledgePoint;
import com.teachhelper.entity.ProcessingStatus;
import com.teachhelper.entity.User;
import com.teachhelper.repository.KnowledgeBaseFavoriteRepository;
import com.teachhelper.repository.KnowledgeBaseRepository;
import com.teachhelper.repository.KnowledgeDocumentRepository;
import com.teachhelper.repository.KnowledgePointRepository;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.service.knowledge.VectorStoreService;

import lombok.extern.slf4j.Slf4j;

/**
 * 开发环境知识库数据服务
 * 负责创建知识库、文档、知识点和收藏数据
 */
@Service
@Slf4j
public class DevKnowledgeDataService {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    
    @Autowired
    private KnowledgePointRepository knowledgePointRepository;
    
    @Autowired
    private KnowledgeBaseFavoriteRepository knowledgeBaseFavoriteRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VectorStoreService vectorStoreService;

    /**
     * 创建知识库数据
     */
    @Transactional
    public void createKnowledgeBases() {
        log.info("🚀 开始创建知识库开发数据...");
        
        List<User> teachers = userRepository.findAll().stream()
            .filter(u -> u.getUsername().startsWith("teacher"))
            .toList();
            
        if (teachers.isEmpty()) {
            log.warn("⚠️ 未找到教师用户，将使用系统默认用户创建知识库");
            teachers = userRepository.findAll().stream()
                .filter(u -> u.getUsername().startsWith("admin"))
                .toList();
        }
        
        log.info("📊 找到 {} 个教师用户", teachers.size());

        List<KnowledgeBase> knowledgeBases = new ArrayList<>();
        
        // 创建各学科的知识库
        knowledgeBases.add(createKnowledgeBase("计算机科学基础", "包含数据结构、算法、操作系统等基础知识", 
                                              "计算机科学", "本科", teachers.get(0).getId()));
        knowledgeBases.add(createKnowledgeBase("Java编程技术", "Java语言基础、面向对象编程、设计模式", 
                                              "软件工程", "本科", teachers.get(0).getId()));
        knowledgeBases.add(createKnowledgeBase("数据库原理与应用", "关系数据库理论、SQL语言、数据库设计", 
                                              "计算机科学", "本科", teachers.get(1 % teachers.size()).getId()));
        knowledgeBases.add(createKnowledgeBase("机器学习基础", "监督学习、无监督学习、神经网络基础", 
                                              "人工智能", "研究生", teachers.get(1 % teachers.size()).getId()));
        knowledgeBases.add(createKnowledgeBase("网络安全技术", "密码学、网络攻防、系统安全", 
                                              "网络安全", "本科", teachers.get(2 % teachers.size()).getId()));
        knowledgeBases.add(createKnowledgeBase("软件工程方法", "需求分析、系统设计、测试方法", 
                                              "软件工程", "本科", teachers.get(2 % teachers.size()).getId()));
        knowledgeBases.add(createKnowledgeBase("高等数学", "微积分、线性代数、概率统计", 
                                              "数学", "本科", teachers.get(3 % teachers.size()).getId()));
        knowledgeBases.add(createKnowledgeBase("物理学基础", "力学、电磁学、量子物理基础", 
                                              "物理", "本科", teachers.get(3 % teachers.size()).getId()));

        knowledgeBaseRepository.saveAll(knowledgeBases);
        log.info("✅ 成功创建 {} 个知识库", knowledgeBases.size());
        
        // 创建知识文档
        createKnowledgeDocuments(knowledgeBases);
        
        // 创建知识点
        createKnowledgePoints(knowledgeBases);
        
        // 创建收藏数据
        createKnowledgeFavorites(knowledgeBases);
        
        log.info("🎉 知识库数据创建完成！");
    }

    /**
     * 创建知识库
     */
    private KnowledgeBase createKnowledgeBase(String name, String description, String subject, 
                                            String gradeLevel, Long createdBy) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        kb.setSubject(subject);
        kb.setGradeLevel(gradeLevel);
        kb.setCreatedBy(createdBy);
        kb.setIsActive(true);
        return kb;
    }

    /**
     * 创建知识文档
     */
    private void createKnowledgeDocuments(List<KnowledgeBase> knowledgeBases) {
        List<KnowledgeDocument> documents = new ArrayList<>();
        
        for (KnowledgeBase kb : knowledgeBases) {
            switch (kb.getName()) {
                case "计算机科学基础":
                    documents.add(createDocument(kb.getId(), "数据结构教程", "data_structures.pdf", 
                                               "PDF", 2048000L, "/docs/data_structures.pdf", 
                                               "详细介绍了线性表、栈、队列、树、图等数据结构的实现和应用", ProcessingStatus.COMPLETED));
                    documents.add(createDocument(kb.getId(), "算法设计与分析", "algorithms.pdf", 
                                               "PDF", 3145600L, "/docs/algorithms.pdf", 
                                               "包含排序、搜索、动态规划等经典算法的设计思路和复杂度分析", ProcessingStatus.COMPLETED));
                    documents.add(createDocument(kb.getId(), "操作系统概念", "os_concepts.pdf", 
                                               "PDF", 4194304L, "/docs/os_concepts.pdf", 
                                               "操作系统的基本概念、进程管理、内存管理、文件系统", ProcessingStatus.COMPLETED));
                    break;
                    
                case "Java编程技术":
                    documents.add(createDocument(kb.getId(), "Java语言基础", "java_basics.pdf", 
                                               "PDF", 1572864L, "/docs/java_basics.pdf", 
                                               "Java语法、面向对象编程、异常处理、集合框架", ProcessingStatus.COMPLETED));
                    documents.add(createDocument(kb.getId(), "设计模式详解", "design_patterns.pdf", 
                                               "PDF", 2097152L, "/docs/design_patterns.pdf", 
                                               "23种设计模式的详细讲解和Java实现", ProcessingStatus.COMPLETED));
                    documents.add(createDocument(kb.getId(), "Spring框架指南", "spring_guide.pdf", 
                                               "PDF", 3670016L, "/docs/spring_guide.pdf", 
                                               "Spring IoC、AOP、MVC等核心概念和实践", ProcessingStatus.PROCESSING));
                    break;
                    
                case "数据库原理与应用":
                    documents.add(createDocument(kb.getId(), "关系数据库理论", "database_theory.pdf", 
                                               "PDF", 2621440L, "/docs/database_theory.pdf", 
                                               "关系模型、范式理论、查询优化", ProcessingStatus.COMPLETED));
                    documents.add(createDocument(kb.getId(), "SQL实践教程", "sql_tutorial.pdf", 
                                               "PDF", 1835008L, "/docs/sql_tutorial.pdf", 
                                               "SQL语法、高级查询、存储过程和触发器", ProcessingStatus.COMPLETED));
                    break;
                    
                case "机器学习基础":
                    documents.add(createDocument(kb.getId(), "机器学习导论", "ml_introduction.pdf", 
                                               "PDF", 5242880L, "/docs/ml_introduction.pdf", 
                                               "机器学习基本概念、监督学习、无监督学习", ProcessingStatus.COMPLETED));
                    documents.add(createDocument(kb.getId(), "深度学习基础", "deep_learning.pdf", 
                                               "PDF", 6291456L, "/docs/deep_learning.pdf", 
                                               "神经网络、反向传播、CNN、RNN基础", ProcessingStatus.PROCESSING));
                    break;
                    
                default:
                    // 为其他知识库创建基础文档
                    documents.add(createDocument(kb.getId(), kb.getName() + "教学大纲", 
                                               "syllabus.pdf", "PDF", 1048576L, 
                                               "/docs/" + kb.getName().toLowerCase() + "_syllabus.pdf", 
                                               kb.getDescription(), ProcessingStatus.COMPLETED));
                    break;
            }
        }
        
        log.info("📄 保存 {} 个知识文档到数据库", documents.size());
        knowledgeDocumentRepository.saveAll(documents);
        
        // 将已完成的文档添加到向量数据库
        List<KnowledgeDocument> completedDocuments = documents.stream()
            .filter(doc -> doc.getStatus() == ProcessingStatus.COMPLETED)
            .toList();
            
        if (!completedDocuments.isEmpty()) {
            log.info("🔍 开始向量化 {} 个已完成的文档...", completedDocuments.size());
            try {
                vectorStoreService.addDocumentsToVectorStore(completedDocuments);
                log.info("✅ 成功将 {} 个文档添加到向量数据库", completedDocuments.size());
            } catch (Exception e) {
                log.error("❌ 向量化文档失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 创建知识文档
     */
    private KnowledgeDocument createDocument(Long knowledgeBaseId, String title, String fileName, 
                                           String fileType, Long fileSize, String filePath, 
                                           String content, ProcessingStatus status) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setTitle(title);
        doc.setFileName(fileName);
        doc.setFileType(fileType);
        doc.setFileSize(fileSize);
        doc.setFilePath(filePath);
        doc.setContent(content);
        doc.setStatus(status);
        doc.setProcessingProgress(status == ProcessingStatus.COMPLETED ? 100.0 : 
                                 status == ProcessingStatus.PROCESSING ? 65.0 : 0.0);
        return doc;
    }

    /**
     * 创建知识点
     */
    private void createKnowledgePoints(List<KnowledgeBase> knowledgeBases) {
        log.info("🎯 开始创建知识点数据...");
        List<KnowledgePoint> knowledgePoints = new ArrayList<>();
        
        for (KnowledgeBase kb : knowledgeBases) {
            List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByKnowledgeBaseId(kb.getId());
            
            switch (kb.getName()) {
                case "计算机科学基础":
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), "线性表", 
                                                          "线性表是最基本的数据结构，包括顺序存储和链式存储两种实现方式。顺序存储支持随机访问，链式存储便于插入删除。", 
                                                          "[\"数据结构\",\"线性表\",\"顺序表\",\"链表\"]", 
                                                          DifficultyLevel.EASY, 
                                                          documents.isEmpty() ? null : documents.get(0).getId(), 1));
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), "栈和队列", 
                                                          "栈是后进先出(LIFO)的数据结构，队列是先进先出(FIFO)的数据结构。都是限制访问的线性表。", 
                                                          "[\"数据结构\",\"栈\",\"队列\",\"LIFO\",\"FIFO\"]", 
                                                          DifficultyLevel.MEDIUM, 
                                                          documents.isEmpty() ? null : documents.get(0).getId(), 2));
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), "二叉树", 
                                                          "二叉树是每个节点最多有两个子节点的树结构。完全二叉树、满二叉树、二叉搜索树是常见类型。", 
                                                          "[\"数据结构\",\"二叉树\",\"完全二叉树\",\"二叉搜索树\"]", 
                                                          DifficultyLevel.HARD, 
                                                          documents.isEmpty() ? null : documents.get(0).getId(), 3));
                    break;
                    
                case "Java编程技术":
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), "面向对象编程", 
                                                          "面向对象编程有三大特性：封装、继承、多态。封装隐藏内部实现，继承实现代码复用，多态提供灵活性。", 
                                                          "[\"面向对象\",\"封装\",\"继承\",\"多态\",\"Java\"]", 
                                                          DifficultyLevel.MEDIUM, 
                                                          documents.isEmpty() ? null : documents.get(0).getId(), 1));
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), "异常处理", 
                                                          "Java异常处理机制包括try-catch-finally语句块，checked异常和unchecked异常的区别。", 
                                                          "[\"异常处理\",\"try-catch\",\"finally\",\"checked异常\"]", 
                                                          DifficultyLevel.MEDIUM, 
                                                          documents.isEmpty() ? null : documents.get(0).getId(), 2));
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), "集合框架", 
                                                          "Java集合框架包括List、Set、Map等接口，ArrayList、LinkedList、HashMap等实现类。", 
                                                          "[\"集合框架\",\"List\",\"Set\",\"Map\",\"ArrayList\",\"HashMap\"]", 
                                                          DifficultyLevel.MEDIUM, 
                                                          documents.isEmpty() ? null : documents.get(0).getId(), 3));
                    break;
                    
                case "数据库原理与应用":
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), "关系模型", 
                                                          "关系模型是数据库的数学基础，包括关系、元组、属性等概念，以及关系代数运算。", 
                                                          "[\"关系模型\",\"关系代数\",\"元组\",\"属性\"]", 
                                                          DifficultyLevel.MEDIUM, 
                                                          documents.isEmpty() ? null : documents.get(0).getId(), 1));
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), "SQL查询", 
                                                          "SQL是结构化查询语言，包括SELECT、INSERT、UPDATE、DELETE等操作，支持连接查询、子查询等。", 
                                                          "[\"SQL\",\"SELECT\",\"JOIN\",\"子查询\",\"聚合函数\"]", 
                                                          DifficultyLevel.EASY, 
                                                          documents.size() > 1 ? documents.get(1).getId() : documents.get(0).getId(), 1));
                    break;
                    
                default:
                    // 为其他知识库创建基础知识点
                    knowledgePoints.add(createKnowledgePoint(kb.getId(), kb.getName() + "概述", 
                                                          kb.getDescription(), 
                                                          "[\"" + kb.getSubject() + "\",\"基础概念\"]", 
                                                          DifficultyLevel.EASY, 
                                                          documents.isEmpty() ? null : documents.get(0).getId(), 1));
                    break;
            }
        }
        
        log.info("💡 保存 {} 个知识点到数据库", knowledgePoints.size());
        knowledgePointRepository.saveAll(knowledgePoints);
        log.info("✅ 知识点创建完成");
    }

    /**
     * 创建知识点
     */
    private KnowledgePoint createKnowledgePoint(Long knowledgeBaseId, String title, String content, 
                                              String keywords, DifficultyLevel difficulty, 
                                              Long sourceDocumentId, Integer pageNumber) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgeBaseId(knowledgeBaseId);
        kp.setTitle(title);
        kp.setContent(content);
        kp.setKeywords(keywords);
        kp.setDifficultyLevel(difficulty);
        kp.setSourceDocumentId(sourceDocumentId);
        kp.setSourcePageNumber(pageNumber);
        kp.setVectorId("vec_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
        return kp;
    }

    /**
     * 创建知识库收藏数据
     */
    private void createKnowledgeFavorites(List<KnowledgeBase> knowledgeBases) {
        log.info("⭐ 开始创建知识库收藏数据...");
        List<User> allUsers = userRepository.findAll();
        List<KnowledgeBaseFavorite> favorites = new ArrayList<>();
        
        // 为部分用户创建收藏数据
        for (int i = 0; i < Math.min(knowledgeBases.size(), 5); i++) {
            for (int j = 0; j < Math.min(allUsers.size(), 3); j++) {
                if (Math.random() < 0.6) { // 60% 概率收藏
                    KnowledgeBaseFavorite favorite = new KnowledgeBaseFavorite();
                    favorite.setUserId(allUsers.get(j).getId());
                    favorite.setKnowledgeBaseId(knowledgeBases.get(i).getId());
                    favorites.add(favorite);
                }
            }
        }
        
        log.info("💾 保存 {} 个收藏记录到数据库", favorites.size());
        knowledgeBaseFavoriteRepository.saveAll(favorites);
        log.info("✅ 收藏数据创建完成");
    }

    /**
     * 清空知识库相关数据
     */
    @Transactional
    public void clearKnowledgeData() {
        log.info("🗑️ 开始清空知识库相关数据...");
        
        // 清空收藏记录
        log.info("删除收藏记录...");
        knowledgeBaseFavoriteRepository.deleteAll();
        
        // 清空知识点
        log.info("删除知识点...");
        knowledgePointRepository.deleteAll();
        
        // 获取所有文档用于清理向量存储
        List<KnowledgeDocument> allDocuments = knowledgeDocumentRepository.findAll();
        
        // 清空知识文档
        log.info("删除知识文档...");
        knowledgeDocumentRepository.deleteAll();
        
        // 清空知识库
        log.info("删除知识库...");
        knowledgeBaseRepository.deleteAll();
        
        // 清理向量存储中的文档数据
        if (!allDocuments.isEmpty()) {
            log.info("🔍 清理向量存储中的 {} 个文档...", allDocuments.size());
            try {
                for (KnowledgeDocument doc : allDocuments) {
                    vectorStoreService.removeDocumentFromVectorStore(doc.getId());
                }
                log.info("✅ 向量存储清理完成");
            } catch (Exception e) {
                log.warn("⚠️ 向量存储清理失败: {}", e.getMessage());
            }
        }
        
        log.info("🎉 知识库数据清理完成！");
    }
}
