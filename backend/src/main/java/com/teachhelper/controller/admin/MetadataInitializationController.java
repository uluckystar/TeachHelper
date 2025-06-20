package com.teachhelper.controller.admin;

import com.teachhelper.service.metadata.MetadataInitializationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 元数据初始化控制器
 * 提供手动初始化和重新初始化功能
 */
@RestController
@RequestMapping("/api/admin/metadata")
@RequiredArgsConstructor
@Tag(name = "元数据初始化", description = "元数据初始化管理接口")
public class MetadataInitializationController {

    private final MetadataInitializationService initializationService;

    @PostMapping("/initialize")
    @Operation(summary = "手动初始化元数据", description = "手动触发学科和年级的初始化")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> initializeMetadata() {
        try {
            initializationService.initializeSubjects();
            initializationService.initializeGradeLevels();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "元数据初始化完成"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "初始化失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/reinitialize")
    @Operation(summary = "重新初始化元数据", description = "清除现有数据并重新初始化")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reinitializeMetadata() {
        try {
            initializationService.reinitialize();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "元数据重新初始化完成"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "重新初始化失败: " + e.getMessage()
            ));
        }
    }
}
