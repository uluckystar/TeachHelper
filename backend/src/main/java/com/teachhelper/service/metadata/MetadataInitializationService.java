package com.teachhelper.service.metadata;

import com.teachhelper.entity.Subject;
import com.teachhelper.entity.GradeLevel;
import com.teachhelper.repository.SubjectRepository;
import com.teachhelper.repository.GradeLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 元数据初始化服务
 * 在应用启动时初始化基础的学科和年级数据
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataInitializationService implements ApplicationRunner {

    private final SubjectRepository subjectRepository;
    private final GradeLevelRepository gradeLevelRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            initializeSubjects();
            initializeGradeLevels();
            log.info("Metadata initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize metadata", e);
        }
    }

    @Transactional
    public void initializeSubjects() {
        // 检查是否已有数据
        if (subjectRepository.count() > 0) {
            log.info("Subjects already exist, skipping initialization");
            return;
        }

        log.info("Initializing default subjects...");

        List<Subject> defaultSubjects = Arrays.asList(
            new Subject("语文", "中文语言文学科目", 1L),
            new Subject("数学", "数学科目", 1L),
            new Subject("英语", "英语语言科目", 1L),
            new Subject("物理", "物理科学科目", 1L),
            new Subject("化学", "化学科学科目", 1L),
            new Subject("生物", "生物科学科目", 1L),
            new Subject("历史", "历史人文科目", 1L),
            new Subject("地理", "地理科学科目", 1L),
            new Subject("政治", "思想政治科目", 1L),
            new Subject("信息技术", "计算机信息技术科目", 1L),
            new Subject("音乐", "音乐艺术科目", 1L),
            new Subject("美术", "美术艺术科目", 1L),
            new Subject("体育", "体育健康科目", 1L)
        );

        // 设置排序顺序
        for (int i = 0; i < defaultSubjects.size(); i++) {
            defaultSubjects.get(i).setSortOrder(i + 1);
        }

        subjectRepository.saveAll(defaultSubjects);
        log.info("Initialized {} default subjects", defaultSubjects.size());
    }

    @Transactional
    public void initializeGradeLevels() {
        // 检查是否已有数据
        if (gradeLevelRepository.count() > 0) {
            log.info("Grade levels already exist, skipping initialization");
            return;
        }

        log.info("Initializing default grade levels...");

        List<GradeLevel> defaultGradeLevels = Arrays.asList(
            // 小学
            new GradeLevel("一年级", "小学一年级", "小学", 1L),
            new GradeLevel("二年级", "小学二年级", "小学", 1L),
            new GradeLevel("三年级", "小学三年级", "小学", 1L),
            new GradeLevel("四年级", "小学四年级", "小学", 1L),
            new GradeLevel("五年级", "小学五年级", "小学", 1L),
            new GradeLevel("六年级", "小学六年级", "小学", 1L),
            
            // 初中
            new GradeLevel("七年级", "初中一年级", "初中", 1L),
            new GradeLevel("八年级", "初中二年级", "初中", 1L),
            new GradeLevel("九年级", "初中三年级", "初中", 1L),
            
            // 高中
            new GradeLevel("高一", "高中一年级", "高中", 1L),
            new GradeLevel("高二", "高中二年级", "高中", 1L),
            new GradeLevel("高三", "高中三年级", "高中", 1L),
            
            // 大学
            new GradeLevel("大一", "大学一年级", "大学", 1L),
            new GradeLevel("大二", "大学二年级", "大学", 1L),
            new GradeLevel("大三", "大学三年级", "大学", 1L),
            new GradeLevel("大四", "大学四年级", "大学", 1L)
        );

        // 设置排序顺序
        for (int i = 0; i < defaultGradeLevels.size(); i++) {
            defaultGradeLevels.get(i).setSortOrder(i + 1);
        }

        gradeLevelRepository.saveAll(defaultGradeLevels);
        log.info("Initialized {} default grade levels", defaultGradeLevels.size());
    }

    /**
     * 手动触发重新初始化（用于测试）
     */
    @Transactional
    public void reinitialize() {
        log.info("Reinitializing metadata...");
        subjectRepository.deleteAll();
        gradeLevelRepository.deleteAll();
        
        initializeSubjects();
        initializeGradeLevels();
        
        log.info("Metadata reinitialization completed");
    }
}
