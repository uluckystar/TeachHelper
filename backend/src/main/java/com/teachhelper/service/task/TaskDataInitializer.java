package com.teachhelper.service.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 任务管理系统测试数据初始化
 * 已禁用示例数据创建，用于真实测试
 */
@Component
@Profile("disabled") // 禁用示例数据创建
public class TaskDataInitializer implements ApplicationRunner {

    @Autowired
    private TaskService taskService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 已禁用示例数据创建，系统准备进行真实测试
        System.out.println("TaskDataInitializer 已禁用 - 系统准备进行真实测试");
    }
}
