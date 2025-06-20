package com.teachhelper.controller.evaluation;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EvaluationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "TEACHER")
    public void testBatchEvaluationWithEmptyList() throws Exception {
        List<Long> emptyAnswerIds = Arrays.asList();
        
        mockMvc.perform(post("/api/evaluations/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyAnswerIds)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Answer IDs list cannot be null or empty")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    public void testBatchEvaluationWithNullList() throws Exception {
        mockMvc.perform(post("/api/evaluations/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().is5xxServerError()); // Spring框架的标准错误处理
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    public void testBatchEvaluationForNonExistentQuestion() throws Exception {
        Long nonExistentQuestionId = 999999L;
        
        mockMvc.perform(post("/api/evaluations/batch/question/" + nonExistentQuestionId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Question with ID 999999 does not exist")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    public void testBatchEvaluationResultForNonExistentTask() throws Exception {
        String nonExistentTaskId = "non-existent-task-id";
        
        mockMvc.perform(get("/api/evaluations/result/" + nonExistentTaskId))
                .andExpect(status().isNotFound());
    }
}
