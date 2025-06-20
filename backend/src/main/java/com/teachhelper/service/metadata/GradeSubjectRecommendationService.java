package com.teachhelper.service.metadata;

import com.teachhelper.entity.SubjectGradeMapping;
import com.teachhelper.entity.GradeLevel;
import com.teachhelper.repository.SubjectGradeMappingRepository;
import com.teachhelper.repository.GradeLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 年级学科推荐服务
 * 基于数据库动态推荐学科，支持优先级排序
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GradeSubjectRecommendationService {
    
    private final SubjectService subjectService;
    private final SubjectGradeMappingRepository subjectGradeMappingRepository;
    private final GradeLevelRepository gradeLevelRepository;
    
    /**
     * 根据年级获取推荐学科列表（已排序）
     * 
     * @param gradeLevel 年级名称
     * @return 推荐的学科列表，按优先级排序
     */
    public List<String> getRecommendedSubjectsByGrade(String gradeLevel) {
        log.info("Getting recommended subjects for grade: {}", gradeLevel);
        
        try {
            // 首先尝试从数据库获取推荐学科
            List<SubjectGradeMapping> mappings = subjectGradeMappingRepository
                .findRecommendedSubjectsByGradeLevelName(gradeLevel);
            
            if (!mappings.isEmpty()) {
                // 从数据库获取推荐学科
                List<String> recommendedSubjects = mappings.stream()
                    .map(mapping -> mapping.getSubject().getName())
                    .collect(Collectors.toList());
                
                // 获取所有可用学科
                List<String> allSubjects = subjectService.getAllSubjectNames();
                
                // 智能排序：推荐的学科排在前面，其他学科排在后面
                List<String> sortedSubjects = new ArrayList<>(recommendedSubjects);
                
                // 添加其他学科（按字母顺序）
                List<String> otherSubjects = allSubjects.stream()
                    .filter(subject -> !recommendedSubjects.contains(subject))
                    .sorted()
                    .collect(Collectors.toList());
                
                sortedSubjects.addAll(otherSubjects);
                
                log.info("Found {} recommended subjects for grade {} from database", 
                    recommendedSubjects.size(), gradeLevel);
                
                return sortedSubjects;
            } else {
                // 如果数据库中没有配置，尝试根据年级类别获取推荐
                return getRecommendedSubjectsByGradeCategory(gradeLevel);
            }
            
        } catch (Exception e) {
            log.error("Error getting recommended subjects for grade: {}", gradeLevel, e);
            // 发生错误时返回所有学科
            return subjectService.getAllSubjectNames();
        }
    }
    
    /**
     * 根据年级类别获取推荐学科（回退方案）
     */
    private List<String> getRecommendedSubjectsByGradeCategory(String gradeLevel) {
        log.info("Trying to get recommended subjects by grade category for: {}", gradeLevel);
        
        try {
            // 查找年级信息
            Optional<GradeLevel> gradeLevelEntity = gradeLevelRepository.findByNameAndIsActiveTrue(gradeLevel);
            
            if (gradeLevelEntity.isPresent()) {
                String category = gradeLevelEntity.get().getCategory();
                
                if (category != null) {
                    // 根据类别获取推荐学科
                    List<SubjectGradeMapping> mappings = subjectGradeMappingRepository
                        .findRecommendedSubjectsByCategory(category);
                    
                    if (!mappings.isEmpty()) {
                        List<String> recommendedSubjects = mappings.stream()
                            .map(mapping -> mapping.getSubject().getName())
                            .distinct()
                            .collect(Collectors.toList());
                        
                        // 获取所有学科并排序
                        List<String> allSubjects = subjectService.getAllSubjectNames();
                        List<String> sortedSubjects = new ArrayList<>(recommendedSubjects);
                        
                        List<String> otherSubjects = allSubjects.stream()
                            .filter(subject -> !recommendedSubjects.contains(subject))
                            .sorted()
                            .collect(Collectors.toList());
                        
                        sortedSubjects.addAll(otherSubjects);
                        
                        log.info("Found {} recommended subjects for category {} from database", 
                            recommendedSubjects.size(), category);
                        
                        return sortedSubjects;
                    }
                }
            }
            
            // 最后的回退方案：返回所有学科
            log.warn("No recommendations found for grade: {}, returning all subjects", gradeLevel);
            return subjectService.getAllSubjectNames();
            
        } catch (Exception e) {
            log.error("Error getting recommended subjects by category for grade: {}", gradeLevel, e);
            return subjectService.getAllSubjectNames();
        }
    }
    
    /**
     * 获取年级类别
     * 
     * @param gradeLevel 年级名称
     * @return 年级类别
     */
    public String getGradeCategory(String gradeLevel) {
        try {
            Optional<GradeLevel> gradeLevelEntity = gradeLevelRepository.findByNameAndIsActiveTrue(gradeLevel);
            if (gradeLevelEntity.isPresent()) {
                return gradeLevelEntity.get().getCategory();
            }
            return "其他";
        } catch (Exception e) {
            log.error("Error getting category for grade: {}", gradeLevel, e);
            return "其他";
        }
    }
    
    /**
     * 获取某个类别的所有年级
     * 
     * @param category 类别名称
     * @return 该类别下的年级列表
     */
    public List<String> getGradesByCategory(String category) {
        try {
            return gradeLevelRepository.findByCategoryAndIsActiveTrueOrderBySortOrderAscNameAsc(category)
                .stream()
                .map(GradeLevel::getName)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting grades by category: {}", category, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取学科推荐度评分
     * 
     * @param subject 学科名称
     * @param gradeLevel 年级名称
     * @return 推荐度评分（1-10，10为最推荐）
     */
    public int getSubjectRecommendationScore(String subject, String gradeLevel) {
        try {
            List<SubjectGradeMapping> mappings = subjectGradeMappingRepository
                .findRecommendedSubjectsByGradeLevelName(gradeLevel);
            
            for (int i = 0; i < mappings.size(); i++) {
                if (subject.equals(mappings.get(i).getSubject().getName())) {
                    // 根据在推荐列表中的位置计算评分，越靠前评分越高
                    return Math.max(10 - i, 6);
                }
            }
            
            // 不在推荐列表中，较低推荐度
            return 3;
            
        } catch (Exception e) {
            log.error("Error getting recommendation score for subject {} and grade {}", subject, gradeLevel, e);
            return 5; // 默认中等推荐度
        }
    }
}
