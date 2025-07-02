package com.teachhelper.service.answer;

import com.teachhelper.ai.AIEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.entity.UserAIConfig;
import com.teachhelper.service.ai.AIClient;
import com.teachhelper.service.ai.AIResponse;
import java.util.*;

public class StudentInfoLLMExtractor {
    /**
     * 批量用LLM解析文件名，返回学号和姓名
     * @param fileNames 文件名列表
     * @param aiService 当前用户配置的AI服务
     * @param userId 当前用户ID
     * @return 学号和姓名列表
     */
    public static List<MajorAssignmentAnswerImportService.StudentInfo> batchExtract(List<String> fileNames, AIEvaluationService aiService, Long userId) {
        // 拼prompt
        StringBuilder sb = new StringBuilder();
        sb.append("请帮我从以下文件名中提取学生学号和姓名，返回JSON数组，每个元素包含studentNumber和name字段。如果没有学号请用No_student_number：\n");
        for (String name : fileNames) {
            sb.append(name).append("\n");
        }
        String prompt = sb.toString();
        try {
            // 通过AIEvaluationService暴露的公有方法获取AI配置和AIClient
            UserAIConfig aiConfig = aiService.getUserDefaultAIConfigPublic(userId);
            AIClient aiClient = aiService.getAIClientPublic(aiConfig);
            AIResponse aiResponse = aiClient.chat(prompt, aiConfig);
            String aiResult = aiResponse.getContent();
            ObjectMapper mapper = new ObjectMapper();
            List<MajorAssignmentAnswerImportService.StudentInfo> result = new ArrayList<>();
            List<Map<String, Object>> list = mapper.readValue(aiResult, List.class);
            for (Map<String, Object> obj : list) {
                String sn = String.valueOf(obj.getOrDefault("studentNumber", "No_student_number"));
                String name = String.valueOf(obj.getOrDefault("name", "未知"));
                result.add(new MajorAssignmentAnswerImportService.StudentInfo(sn, name));
            }
            return result;
        } catch (Exception e) {
            // 兜底：全部No_student_number
            List<MajorAssignmentAnswerImportService.StudentInfo> result = new ArrayList<>();
            for (String name : fileNames) {
                result.add(new MajorAssignmentAnswerImportService.StudentInfo("No_student_number", name));
            }
            return result;
        }
    }
} 