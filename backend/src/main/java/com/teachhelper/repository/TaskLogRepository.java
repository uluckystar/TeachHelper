package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Task;
import com.teachhelper.entity.TaskLog;
import com.teachhelper.entity.TaskLog.LogLevel;

/**
 * 任务日志数据访问层
 */
@Repository
public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {

    /**
     * 根据任务查找日志
     */
    List<TaskLog> findByTaskOrderByCreatedAtDesc(Task task);

    /**
     * 根据任务ID查找日志
     */
    @Query("SELECT tl FROM TaskLog tl WHERE tl.task.taskId = :taskId ORDER BY tl.createdAt DESC")
    List<TaskLog> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") String taskId);

    /**
     * 根据任务和日志级别查找日志
     */
    List<TaskLog> findByTaskAndLevelOrderByCreatedAtDesc(Task task, LogLevel level);

    /**
     * 分页查询任务日志
     */
    Page<TaskLog> findByTaskOrderByCreatedAtDesc(Task task, Pageable pageable);

    /**
     * 根据任务ID分页查询日志
     */
    @Query("SELECT tl FROM TaskLog tl WHERE tl.task.taskId = :taskId ORDER BY tl.createdAt DESC")
    Page<TaskLog> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") String taskId, Pageable pageable);

    /**
     * 删除指定任务的所有日志
     */
    void deleteByTask(Task task);

    /**
     * 根据任务ID删除日志
     */
    @Query("DELETE FROM TaskLog tl WHERE tl.task.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") String taskId);
}
