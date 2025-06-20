package com.teachhelper.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Task;
import com.teachhelper.entity.TaskStatus;

/**
 * 任务数据访问层
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 根据任务ID查找任务
     */
    Optional<Task> findByTaskId(String taskId);

    /**
     * 根据状态查找任务
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * 根据类型查找任务
     */
    List<Task> findByType(String type);

    /**
     * 根据创建者查找任务
     */
    List<Task> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);

    /**
     * 获取最近的任务
     */
    List<Task> findTop10ByOrderByCreatedAtDesc();

    /**
     * 根据条件分页查询任务
     */
    @Query("SELECT t FROM Task t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR t.createdAt <= :endDate)")
    Page<Task> findTasksWithFilters(
            @Param("status") TaskStatus status,
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 根据状态统计任务数量
     */
    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> countByStatus();

    /**
     * 获取运行中的任务
     */
    List<Task> findByStatusIn(List<TaskStatus> statuses);

    /**
     * 删除已完成和失败的任务
     */
    @Query("DELETE FROM Task t WHERE t.status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    int deleteCompletedTasks();

    /**
     * 根据任务ID删除
     */
    void deleteByTaskId(String taskId);

    /**
     * 检查任务是否存在
     */
    boolean existsByTaskId(String taskId);
}
