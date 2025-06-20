package com.teachhelper.controller.dev;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.service.dev.DevDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dev")
@Tag(name = "开发工具", description = "开发阶段专用的工具接口")
@Profile({"dev", "test"}) // 只在开发和测试环境启用
public class DevController {
    
    @Autowired
    private DevDataService devDataService;
    
    @PostMapping("/generate-sample-data")
    @Operation(summary = "生成示例数据", description = "生成开发和测试用的示例数据，包括用户、考试、题目、答案等")
    @PreAuthorize("hasRole('ADMIN')") // 只有管理员可以执行
    public ResponseEntity<String> generateSampleData() {
        try {
            devDataService.generateSampleDataForce();
            return ResponseEntity.ok("示例数据生成成功！包含：\n" +
                "- 13个用户（1个管理员，4个教师，8个学生）\n" +
                "- 8个学生信息记录（涵盖计算机科学、软件工程、网络工程专业）\n" +
                "- 6个考试（Java基础、数据库原理、操作系统、算法数据结构、计算机网络、软件工程）\n" +
                "- 17个题目（包含论述题、简答题、编程题、案例分析、选择题、判断题等多种题型）\n" +
                "- 完整的评分标准体系（每个题目包含详细的评分准则）\n" +
                "- 丰富的学生答案示例（包含已评估和未评估答案，涵盖手动、AI自动、AI辅助等评估类型）\n" +
                "- 题目选项配置（为选择题和判断题提供具体选项内容）\n" +
                "- 7个AI配置（涵盖OpenAI、DeepSeek、Claude、通义千问、文心一言、混元、自定义等主流AI提供商）\n" +
                "- 丰富的AI使用统计数据（包含不同使用场景、成本统计、成功率等指标）");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("生成示例数据失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/init-data")
    @Operation(summary = "初始化数据", description = "开发阶段无需认证的数据初始化接口")
    public ResponseEntity<String> initializeData() {
        try {
            devDataService.generateSampleData();
            return ResponseEntity.ok("初始化数据成功！默认用户：\n" +
                "管理员: admin/password\n" +
                "教师1: teacher1/password\n" +
                "教师2: teacher2/password\n" +
                "教师3: teacher3/password\n" +
                "教师4: teacher4/password\n" +
                "学生1: student1/password\n" +
                "学生2: student2/password\n" +
                "学生3: student3/password\n" +
                "学生4: student4/password\n" +
                "学生5: student5/password\n" +
                "学生6: student6/password\n" +
                "学生7: student7/password\n" +
                "学生8: student8/password\n\n" +
                "数据详情：\n" +
                "• 13个用户账户（完整的角色权限体系）\n" +
                "• 8个学生档案（3个专业，不同班级分布）\n" +
                "• 6门考试科目（涵盖计算机专业核心课程）\n" +
                "• 17道题目（7种题型，难度分级）\n" +
                "• 完整评分标准体系\n" +
                "• 丰富的答题示例数据\n" +
                "• 7个AI配置示例（支持主流AI提供商）\n" +
                "• AI使用统计数据（包含成本、性能指标）");
        } catch (Exception e) {
            if (e.getMessage().contains("已存在数据")) {
                return ResponseEntity.ok("数据库中已有数据，无需重复初始化。默认用户：\n" +
                    "管理员: admin/password\n" +
                    "教师1-4: teacher[1-4]/password\n" +
                    "学生1-8: student[1-8]/password\n\n" +
                    "当前数据包含完整的教学管理系统示例数据。");
            }
            return ResponseEntity.internalServerError()
                .body("初始化数据失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/data-statistics")
    @Operation(summary = "获取数据统计", description = "获取当前数据库中各类数据的统计信息")
    public ResponseEntity<String> getDataStatistics() {
        try {
            String dynamicStats = devDataService.getDataStatistics();
            String staticInfo = """
                
                🎯 数据特点：
                  • 专业分布：计算机科学、软件工程、网络工程
                  • 题型多样：论述、简答、编程、案例分析、选择、判断
                  • 评估完整：包含分数、反馈、评估者信息
                  • AI集成：完整的AI配置和使用数据
                
                💡 使用说明：
                  • 所有用户密码均为：password
                  • 数据已包含完整的关联关系
                  • 适用于开发测试和功能演示
                  • AI配置为演示用途，请替换为真实API密钥
                """;
            
            return ResponseEntity.ok(dynamicStats + staticInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("获取数据统计失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/clear-all-data")
    @Operation(summary = "清空所有数据", description = "清空数据库中的所有数据，用于重新开始")
    @PreAuthorize("hasRole('ADMIN')") // 只有管理员可以执行
    public ResponseEntity<String> clearAllData() {
        try {
            devDataService.clearAllData();
            return ResponseEntity.ok("✅ 所有数据已清空！\n" +
                "包括：用户、学生、考试、题目、答案、AI配置、试卷模板、知识库等所有数据\n" +
                "现在可以重新生成全新的示例数据。");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("清空数据失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/fix-database-constraints")
    @Operation(summary = "修复数据库约束问题", description = "修复外键约束等数据库问题")
    public ResponseEntity<String> fixDatabaseConstraints() {
        try {
            // 先清空所有数据，然后重新生成
            devDataService.clearAllData();
            devDataService.generateSampleDataForce();
            
            return ResponseEntity.ok("✅ 数据库约束问题已修复！\n" +
                "已重新生成完整的示例数据，所有外键关系正确建立。\n\n" +
                "包含：\n" +
                "- 13个用户（1个管理员，4个教师，8个学生）\n" +
                "- 8个学生档案（涵盖3个专业）\n" +
                "- 6门考试科目（Java、数据库、操作系统、算法、网络、软件工程）\n" +
                "- 17道题目（7种题型）\n" +
                "- 完整的题目库体系\n" +
                "- 丰富的学生答案数据\n" +
                "- 7个AI配置\n" +
                "- AI使用统计数据\n" +
                "- 试卷生成模板和历史记录\n" +
                "- 知识库和评分标准数据");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("修复数据库约束失败：" + e.getMessage());
        }
    }
}
