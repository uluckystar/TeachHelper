package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class ImportResult {
    
    /**
     * 成功导入的答案数量
     */
    private int successCount;
    
    /**
     * 跳过的文件数量
     */
    private int skippedCount;
    
    /**
     * 失败的文件数量
     */
    private int failedCount;
    
    /**
     * 总文件数量
     */
    private int totalFiles;
    
    /**
     * 成功导入的学生列表
     */
    private List<String> successfulStudents = new ArrayList<>();
    
    /**
     * 跳过的文件列表（无法解析）
     */
    private List<SkippedFile> skippedFiles = new ArrayList<>();
    
    /**
     * 失败的文件列表（解析异常）
     */
    private List<FailedFile> failedFiles = new ArrayList<>();
    
    /**
     * 导入过程中的警告信息
     */
    private List<String> warnings = new ArrayList<>();
    
    /**
     * 错误信息列表（兼容字段）
     */
    private List<Object> errors = new ArrayList<>();
    
    /**
     * 时间字段
     */
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    /**
     * 更多字段
     */
    private boolean success = true;
    private String errorMessage;
    private int totalProcessed;
    private List<String> failedStudents = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();
    
    // 无参构造函数
    public ImportResult() {}
    
    // 全参构造函数
    public ImportResult(int successCount, int skippedCount, int failedCount, int totalFiles, List<String> successfulStudents, List<SkippedFile> skippedFiles, List<FailedFile> failedFiles, List<String> warnings, List<Object> errors) {
        this.successCount = successCount;
        this.skippedCount = skippedCount;
        this.failedCount = failedCount;
        this.totalFiles = totalFiles;
        this.successfulStudents = successfulStudents != null ? successfulStudents : new ArrayList<>();
        this.skippedFiles = skippedFiles != null ? skippedFiles : new ArrayList<>();
        this.failedFiles = failedFiles != null ? failedFiles : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    // Getter方法
    public int getSuccessCount() {
        return successCount;
    }
    
    public int getSkippedCount() {
        return skippedCount;
    }
    
    public int getFailedCount() {
        return failedCount;
    }
    
    public int getTotalFiles() {
        return totalFiles;
    }
    
    public List<String> getSuccessfulStudents() {
        return successfulStudents;
    }
    
    public List<SkippedFile> getSkippedFiles() {
        return skippedFiles;
    }
    
    public List<FailedFile> getFailedFiles() {
        return failedFiles;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public List<Object> getErrors() {
        return errors;
    }
    
    // Setter方法
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }
    
    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
    
    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public void setSuccessfulStudents(List<String> successfulStudents) {
        this.successfulStudents = successfulStudents;
    }
    
    public void setSkippedFiles(List<SkippedFile> skippedFiles) {
        this.skippedFiles = skippedFiles;
    }
    
    public void setFailedFiles(List<FailedFile> failedFiles) {
        this.failedFiles = failedFiles;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public void setErrors(List<Object> errors) {
        this.errors = errors;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getTotalProcessed() {
        return totalProcessed;
    }
    
    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }
    
    public List<String> getFailedStudents() {
        return failedStudents;
    }
    
    public void setFailedStudents(List<String> failedStudents) {
        this.failedStudents = failedStudents;
    }
    
    public List<String> getErrorMessages() {
        return errorMessages;
    }
    
    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
    
    // 为了向下兼容，添加setFailureCount方法（映射到setFailedCount）
    public void setFailureCount(int failureCount) {
        setFailedCount(failureCount);
    }
    
    public static class SkippedFile {
        private String fileName;
        private String reason;
        
        // 无参构造函数
        public SkippedFile() {}
        
        // 全参构造函数
        public SkippedFile(String fileName, String reason) {
            this.fileName = fileName;
            this.reason = reason;
        }
        
        // Getter方法
        public String getFileName() {
            return fileName;
        }
        
        public String getReason() {
            return reason;
        }
        
        // Setter方法
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        // equals方法
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SkippedFile that = (SkippedFile) o;
            return Objects.equals(fileName, that.fileName) &&
                   Objects.equals(reason, that.reason);
        }
        
        // hashCode方法
        @Override
        public int hashCode() {
            return Objects.hash(fileName, reason);
        }
        
        // toString方法
        @Override
        public String toString() {
            return "SkippedFile{" +
                    "fileName='" + fileName + '\'' +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
    
    public static class FailedFile {
        private String fileName;
        private String error;
        
        // 无参构造函数
        public FailedFile() {}
        
        // 全参构造函数
        public FailedFile(String fileName, String error) {
            this.fileName = fileName;
            this.error = error;
        }
        
        // Getter方法
        public String getFileName() {
            return fileName;
        }
        
        public String getError() {
            return error;
        }
        
        // Setter方法
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        // equals方法
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedFile that = (FailedFile) o;
            return Objects.equals(fileName, that.fileName) &&
                   Objects.equals(error, that.error);
        }
        
        // hashCode方法
        @Override
        public int hashCode() {
            return Objects.hash(fileName, error);
        }
        
        // toString方法
        @Override
        public String toString() {
            return "FailedFile{" +
                    "fileName='" + fileName + '\'' +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
    
    /**
     * 判断是否有需要用户注意的问题
     */
    public boolean hasIssues() {
        return skippedCount > 0 || failedCount > 0 || !warnings.isEmpty();
    }
    
    /**
     * 获取总体成功率
     */
    public double getSuccessRate() {
        if (totalFiles == 0) return 0.0;
        return (double) successCount / totalFiles * 100;
    }
    
    // equals方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportResult that = (ImportResult) o;
        return successCount == that.successCount &&
               skippedCount == that.skippedCount &&
               failedCount == that.failedCount &&
               totalFiles == that.totalFiles &&
               Objects.equals(successfulStudents, that.successfulStudents) &&
               Objects.equals(skippedFiles, that.skippedFiles) &&
               Objects.equals(failedFiles, that.failedFiles) &&
               Objects.equals(warnings, that.warnings) &&
               Objects.equals(errors, that.errors);
    }
    
    // hashCode方法
    @Override
    public int hashCode() {
        return Objects.hash(successCount, skippedCount, failedCount, totalFiles, successfulStudents, skippedFiles, failedFiles, warnings, errors);
    }
    
    // toString方法
    @Override
    public String toString() {
        return "ImportResult{" +
                "successCount=" + successCount +
                ", skippedCount=" + skippedCount +
                ", failedCount=" + failedCount +
                ", totalFiles=" + totalFiles +
                ", successfulStudents=" + successfulStudents +
                ", skippedFiles=" + skippedFiles +
                ", failedFiles=" + failedFiles +
                ", warnings=" + warnings +
                ", errors=" + errors +
                '}';
    }
} 