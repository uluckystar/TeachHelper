package com.teachhelper.controller.evaluation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class EvaluationControllerSimpleTest {

    @Test
    public void contextLoads() {
        // 简单的上下文加载测试
        System.out.println("Spring context loads successfully");
    }
}
