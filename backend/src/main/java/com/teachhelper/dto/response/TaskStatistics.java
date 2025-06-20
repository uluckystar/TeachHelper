package com.teachhelper.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 任务统计响应DTO
 */
@Schema(description = "任务统计信息")
public class TaskStatistics {

    @Schema(description = "总任务数", example = "50")
    private int total;

    @Schema(description = "运行中任务数", example = "3")
    private int running;

    @Schema(description = "待处理任务数", example = "5")
    private int pending;

    @Schema(description = "已完成任务数", example = "40")
    private int completed;

    @Schema(description = "失败任务数", example = "2")
    private int failed;

    @Schema(description = "已暂停任务数", example = "0")
    private int paused;

    @Schema(description = "已取消任务数", example = "0")
    private int cancelled;

    // Constructors
    public TaskStatistics() {}

    public TaskStatistics(int total, int running, int pending, int completed, int failed, int paused, int cancelled) {
        this.total = total;
        this.running = running;
        this.pending = pending;
        this.completed = completed;
        this.failed = failed;
        this.paused = paused;
        this.cancelled = cancelled;
    }

    // Getters and Setters
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getRunning() {
        return running;
    }

    public void setRunning(int running) {
        this.running = running;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getPaused() {
        return paused;
    }

    public void setPaused(int paused) {
        this.paused = paused;
    }

    public int getCancelled() {
        return cancelled;
    }

    public void setCancelled(int cancelled) {
        this.cancelled = cancelled;
    }
}
