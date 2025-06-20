package com.teachhelper.event;

import org.springframework.context.ApplicationEvent;

import com.teachhelper.dto.response.TaskResponse;

/**
 * 任务创建事件
 * 当新任务被创建时触发此事件
 */
public class TaskCreatedEvent extends ApplicationEvent {
    
    private final TaskResponse task;
    
    public TaskCreatedEvent(Object source, TaskResponse task) {
        super(source);
        this.task = task;
    }
    
    public TaskResponse getTask() {
        return task;
    }
}
