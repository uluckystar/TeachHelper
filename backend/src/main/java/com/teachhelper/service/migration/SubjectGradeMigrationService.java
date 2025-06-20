package com.teachhelper.service.migration;

import com.teachhelper.entity.Subject;
import com.teachhelper.entity.GradeLevel;
import com.teachhelper.entity.SubjectGradeMapping;
import com.teachhelper.repository.SubjectRepository;
import com.teachhelper.repository.GradeLevelRepository;
import com.teachhelper.repository.SubjectGradeMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 学科年级关联数据迁移服务
 * 将硬编码的推荐关系迁移到数据库
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectGradeMigrationService {
    
    private final SubjectRepository subjectRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SubjectGradeMappingRepository subjectGradeMappingRepository;
    
    // 硬编码的推荐映射（用于数据迁移）
    private static final Map<String, List<String>> LEGACY_GRADE_SUBJECT_MAPPING;
    static {
        LEGACY_GRADE_SUBJECT_MAPPING = new HashMap<>();
        
        // 小学阶段
        LEGACY_GRADE_SUBJECT_MAPPING.put("小学", Arrays.asList(
            "语文", "数学", "英语", "美术", "音乐", "体育", "道德与法治", "科学", "信息技术", "综合实践"
        ));
        
        // 初中阶段  
        LEGACY_GRADE_SUBJECT_MAPPING.put("初中", Arrays.asList(
            "语文", "数学", "英语", "物理", "化学", "生物", "历史", "地理", "政治", "音乐", "美术", "体育", "信息技术"
        ));
        
        // 高中阶段
        LEGACY_GRADE_SUBJECT_MAPPING.put("高中", Arrays.asList(
            "语文", "数学", "英语", "物理", "化学", "生物", "历史", "地理", "政治", "音乐", "美术", "体育", "信息技术", "通用技术"
        ));
        
        // 大学阶段
        LEGACY_GRADE_SUBJECT_MAPPING.put("大学", Arrays.asList(
            "高等数学", "线性代数", "概率论", "计算机科学", "软件工程", "数据结构", "操作系统", "计算机网络", 
            "数据库原理", "编程语言", "机械工程", "电子工程", "经济学", "管理学", "哲学", "心理学"
        ));
        
        // 职业教育
        LEGACY_GRADE_SUBJECT_MAPPING.put("职业", Arrays.asList(
            "职业技能培训", "企业培训", "IT认证", "会计师考试", "教师资格证", "语言培训", "技能认证"
        ));
        
        // 学前教育
        LEGACY_GRADE_SUBJECT_MAPPING.put("学前", Arrays.asList(
            "语言", "数学启蒙", "科学探索", "艺术创作", "体能训练", "社会认知", "音乐游戏", "美术手工"
        ));
        
        // 成人教育
        LEGACY_GRADE_SUBJECT_MAPPING.put("成人", Arrays.asList(
            "语文", "数学", "英语", "政治", "历史", "地理", "职业技能", "计算机应用", "法律基础", "经济管理"
        ));
    }
    
    /**
     * 执行数据迁移
     */
    @Transactional
    public void migrateSubjectGradeMappings() {
        log.info("Starting subject-grade mapping migration...");
        
        try {
            // 清空现有的关联数据
            subjectGradeMappingRepository.deleteAll();
            log.info("Cleared existing subject-grade mappings");
            
            int totalMappings = 0;
            
            // 遍历每个类别
            for (Map.Entry<String, List<String>> entry : LEGACY_GRADE_SUBJECT_MAPPING.entrySet()) {
                String category = entry.getKey();
                List<String> subjectNames = entry.getValue();
                
                log.info("Processing category: {} with {} subjects", category, subjectNames.size());
                
                // 获取该类别下的所有年级
                List<GradeLevel> gradeLevels = gradeLevelRepository
                    .findByCategoryAndIsActiveTrueOrderBySortOrderAscNameAsc(category);
                
                if (gradeLevels.isEmpty()) {
                    log.warn("No grade levels found for category: {}", category);
                    continue;
                }
                
                // 为每个学科创建与年级的关联
                for (int i = 0; i < subjectNames.size(); i++) {
                    String subjectName = subjectNames.get(i);
                    
                    // 查找或创建学科
                    Optional<Subject> subjectOpt = subjectRepository.findByNameAndIsActiveTrue(subjectName);
                    Subject subject;
                    
                    if (subjectOpt.isPresent()) {
                        subject = subjectOpt.get();
                    } else {
                        // 创建新学科
                        subject = new Subject();
                        subject.setName(subjectName);
                        subject.setDescription("系统自动创建的学科");
                        subject.setSortOrder(i);
                        subject.setIsActive(true);
                        subject = subjectRepository.save(subject);
                        log.info("Created new subject: {}", subjectName);
                    }
                    
                    // 为该学科在所有相关年级创建关联
                    for (GradeLevel gradeLevel : gradeLevels) {
                        // 检查是否已存在关联
                        if (!subjectGradeMappingRepository.existsBySubjectIdAndGradeLevelId(
                                subject.getId(), gradeLevel.getId())) {
                            
                            SubjectGradeMapping mapping = new SubjectGradeMapping();
                            mapping.setSubjectId(subject.getId());
                            mapping.setGradeLevelId(gradeLevel.getId());
                            mapping.setPriority(i); // 按原始顺序设置优先级
                            mapping.setIsRecommended(true);
                            
                            subjectGradeMappingRepository.save(mapping);
                            totalMappings++;
                        }
                    }
                }
                
                log.info("Completed category: {} with {} grade levels", category, gradeLevels.size());
            }
            
            log.info("Subject-grade mapping migration completed successfully! Created {} mappings", totalMappings);
            
        } catch (Exception e) {
            log.error("Error during subject-grade mapping migration", e);
            throw new RuntimeException("数据迁移失败", e);
        }
    }
    
    /**
     * 执行硬编码数据迁移（返回迁移的记录数）
     */
    @Transactional
    public int migrateHardcodedData() {
        log.info("Starting hardcoded data migration...");
        
        try {
            int totalMappings = 0;
            
            // 遍历每个类别
            for (Map.Entry<String, List<String>> entry : LEGACY_GRADE_SUBJECT_MAPPING.entrySet()) {
                String category = entry.getKey();
                List<String> subjectNames = entry.getValue();
                
                log.info("Processing category: {} with {} subjects", category, subjectNames.size());
                
                // 获取该类别下的所有年级
                List<GradeLevel> gradeLevels = gradeLevelRepository
                    .findByCategoryAndIsActiveTrueOrderBySortOrderAscNameAsc(category);
                
                if (gradeLevels.isEmpty()) {
                    log.warn("No grade levels found for category: {}", category);
                    continue;
                }
                
                // 为每个学科创建与年级的关联
                for (int i = 0; i < subjectNames.size(); i++) {
                    String subjectName = subjectNames.get(i);
                    
                    // 查找或创建学科
                    Optional<Subject> subjectOpt = subjectRepository.findByNameAndIsActiveTrue(subjectName);
                    Subject subject;
                    
                    if (subjectOpt.isPresent()) {
                        subject = subjectOpt.get();
                    } else {
                        // 创建新学科
                        subject = new Subject();
                        subject.setName(subjectName);
                        subject.setDescription("系统自动创建的学科");
                        subject.setSortOrder(i);
                        subject.setIsActive(true);
                        subject = subjectRepository.save(subject);
                        log.info("Created new subject: {}", subjectName);
                    }
                    
                    // 为该学科在所有相关年级创建关联
                    for (GradeLevel gradeLevel : gradeLevels) {
                        // 检查是否已存在关联
                        if (!subjectGradeMappingRepository.existsBySubjectIdAndGradeLevelId(
                                subject.getId(), gradeLevel.getId())) {
                            
                            SubjectGradeMapping mapping = new SubjectGradeMapping();
                            mapping.setSubjectId(subject.getId());
                            mapping.setGradeLevelId(gradeLevel.getId());
                            mapping.setPriority(i); // 按原始顺序设置优先级
                            mapping.setIsRecommended(true);
                            
                            subjectGradeMappingRepository.save(mapping);
                            totalMappings++;
                        }
                    }
                }
                
                log.info("Completed category: {} with {} grade levels", category, gradeLevels.size());
            }
            
            log.info("Hardcoded data migration completed successfully! Created {} mappings", totalMappings);
            return totalMappings;
            
        } catch (Exception e) {
            log.error("Error during hardcoded data migration", e);
            throw new RuntimeException("数据迁移失败", e);
        }
    }
    
    /**
     * 检查是否需要迁移
     */
    @Transactional(readOnly = true)
    public boolean needsMigration() {
        long existingMappings = subjectGradeMappingRepository.count();
        log.info("Found {} existing subject-grade mappings", existingMappings);
        return existingMappings == 0;
    }
    
    /**
     * 检查是否存在映射数据
     */
    @Transactional(readOnly = true)
    public boolean hasExistingMappingData() {
        return subjectGradeMappingRepository.count() > 0;
    }
    
    /**
     * 获取映射数据数量
     */
    @Transactional(readOnly = true)
    public int getMappingDataCount() {
        return (int) subjectGradeMappingRepository.count();
    }
    
    /**
     * 清空所有映射数据
     */
    @Transactional
    public int clearAllMappingData() {
        log.info("Clearing all subject-grade mapping data...");
        try {
            int count = getMappingDataCount();
            subjectGradeMappingRepository.deleteAll();
            log.info("Cleared {} subject-grade mappings", count);
            return count;
        } catch (Exception e) {
            log.error("Error clearing mapping data", e);
            throw new RuntimeException("数据清空失败", e);
        }
    }

    /**
     * 获取迁移统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMigrationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalSubjects = subjectRepository.countByIsActiveTrue();
        long totalGradeLevels = gradeLevelRepository.countByIsActiveTrue();
        long totalMappings = subjectGradeMappingRepository.count();
        
        stats.put("totalSubjects", totalSubjects);
        stats.put("totalGradeLevels", totalGradeLevels);
        stats.put("totalMappings", totalMappings);
        stats.put("needsMigration", needsMigration());
        
        return stats;
    }
}
