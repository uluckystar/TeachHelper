package com.teachhelper.controller.migration;

import com.teachhelper.service.migration.SubjectGradeMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学科年级关联数据迁移控制器
 */
@RestController
@RequestMapping("/api/migration/subject-grade")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "数据迁移", description = "学科年级关联数据迁移相关接口")
@PreAuthorize("hasRole('ADMIN')")
public class SubjectGradeMigrationController {

    private final SubjectGradeMigrationService migrationService;

    @PostMapping("/migrate")
    @Operation(summary = "迁移硬编码的学科年级关联数据到数据库")
    public ResponseEntity<Map<String, Object>> migrateSubjectGradeData() {
        try {
            log.info("开始迁移学科年级关联数据...");
            int migratedCount = migrationService.migrateHardcodedData();
            String message = String.format("数据迁移完成，共迁移 %d 条关联记录", migratedCount);
            log.info(message);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "migratedCount", migratedCount
            ));
        } catch (Exception e) {
            log.error("数据迁移失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "数据迁移失败: " + e.getMessage(),
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "检查迁移状态")
    public ResponseEntity<Map<String, Object>> checkMigrationStatus() {
        try {
            boolean hasData = migrationService.hasExistingMappingData();
            if (hasData) {
                int count = migrationService.getMappingDataCount();
                return ResponseEntity.ok(Map.of(
                    "hasData", true,
                    "message", String.format("数据库中已存在 %d 条学科年级关联记录", count),
                    "count", count
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "hasData", false,
                    "message", "数据库中暂无学科年级关联记录，可以执行迁移操作",
                    "count", 0
                ));
            }
        } catch (Exception e) {
            log.error("检查迁移状态失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "检查状态失败: " + e.getMessage(),
                "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空学科年级关联数据")
    public ResponseEntity<Map<String, Object>> clearMappingData() {
        try {
            log.info("开始清空学科年级关联数据...");
            int deletedCount = migrationService.clearAllMappingData();
            String message = String.format("数据清空完成，共删除 %d 条关联记录", deletedCount);
            log.info(message);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            log.error("数据清空失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "数据清空失败: " + e.getMessage(),
                "error", e.getMessage()
            ));
        }
    }
}
