package com.teachhelper.service.exam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.yml")
public class ExamServiceTest {

    @Test
    public void testSanitizeSheetName() throws Exception {
        ExamService examService = new ExamService();
        
        // 使用反射来测试私有方法
        Method sanitizeMethod = ExamService.class.getDeclaredMethod("sanitizeSheetName", String.class);
        sanitizeMethod.setAccessible(true);
        
        // 测试包含斜杠的工作表名称
        String result1 = (String) sanitizeMethod.invoke(examService, "学习通导入测试 - 2025/07/01 - 成绩单");
        assertEquals("学习通导入测试 - 2025_07_01 - 成绩单", result1);
        
        // 测试包含多种非法字符
        String result2 = (String) sanitizeMethod.invoke(examService, "测试/考试\\题目?*名称[1]:2");
        assertEquals("测试_考试_题目__名称_1__2", result2);
        
        // 测试空字符串
        String result3 = (String) sanitizeMethod.invoke(examService, "");
        assertEquals("Sheet", result3);
        
        // 测试null
        String result4 = (String) sanitizeMethod.invoke(examService, (String) null);
        assertEquals("Sheet", result4);
        
        // 测试超长名称（超过31个字符）
        String longName = "这是一个非常长的考试名称，用来测试Excel工作表名称长度限制功能";
        String result5 = (String) sanitizeMethod.invoke(examService, longName);
        assertTrue(result5.length() <= 31);
        
        // 测试正常名称
        String result6 = (String) sanitizeMethod.invoke(examService, "正常考试名称");
        assertEquals("正常考试名称", result6);
        
        System.out.println("✅ 所有Excel工作表名称清理测试通过！");
    }
} 