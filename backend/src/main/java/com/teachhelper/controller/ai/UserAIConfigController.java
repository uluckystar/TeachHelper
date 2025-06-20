package com.teachhelper.controller.ai;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.dto.request.AIConfigTestRequest;
import com.teachhelper.dto.request.UserAIConfigRequest;
import com.teachhelper.dto.response.AIConfigTestResponse;
import com.teachhelper.dto.response.AIProviderInfo;
import com.teachhelper.dto.response.AIUsageStatsDTO;
import com.teachhelper.dto.response.UserAIConfigResponse;
import com.teachhelper.service.UserAIConfigService;
import com.teachhelper.service.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user-ai-config")
@Tag(name = "AI配置管理", description = "用户AI配置的创建、查询、修改、删除等操作")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
public class UserAIConfigController {
    
    @Autowired
    private UserAIConfigService userAIConfigService;
    
    @Autowired
    private AuthService authService;
    
    @GetMapping
    @Operation(summary = "获取用户AI配置列表", description = "获取当前用户的所有AI配置")
    public ResponseEntity<List<UserAIConfigResponse>> getConfigs() {
        Long userId = authService.getCurrentUser().getId();
        List<UserAIConfigResponse> configs = userAIConfigService.getUserAIConfigs(userId);
        return ResponseEntity.ok(configs);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取AI配置详情", description = "根据ID获取特定AI配置的详细信息")
    public ResponseEntity<UserAIConfigResponse> getConfig(@PathVariable Long id) {
        Long userId = authService.getCurrentUser().getId();
        UserAIConfigResponse config = userAIConfigService.getAIConfigById(userId, id);
        return ResponseEntity.ok(config);
    }
    
    @PostMapping
    @Operation(summary = "创建AI配置", description = "为当前用户创建新的AI配置")
    public ResponseEntity<UserAIConfigResponse> createConfig(@Valid @RequestBody UserAIConfigRequest request) {
        Long userId = authService.getCurrentUser().getId();
        UserAIConfigResponse config = userAIConfigService.saveAIConfig(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新AI配置", description = "更新指定的AI配置")
    public ResponseEntity<UserAIConfigResponse> updateConfig(@PathVariable Long id, 
                                                           @Valid @RequestBody UserAIConfigRequest request) {
        Long userId = authService.getCurrentUser().getId();
        UserAIConfigResponse config = userAIConfigService.updateAIConfig(userId, id, request);
        return ResponseEntity.ok(config);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除AI配置", description = "删除指定的AI配置")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        Long userId = authService.getCurrentUser().getId();
        userAIConfigService.deleteAIConfig(userId, id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/toggle")
    @Operation(summary = "激活/停用AI配置", description = "切换AI配置的激活状态")
    public ResponseEntity<UserAIConfigResponse> toggleConfig(@PathVariable Long id,
                                                           @RequestBody Map<String, Boolean> requestBody) {
        Long userId = authService.getCurrentUser().getId();
        Boolean isActive = requestBody.get("isActive");
        UserAIConfigResponse config = userAIConfigService.toggleAIConfig(userId, id, isActive);
        return ResponseEntity.ok(config);
    }
    
    @PostMapping("/{id}/test")
    @Operation(summary = "测试AI配置", description = "测试AI配置的连接性和功能")
    public ResponseEntity<AIConfigTestResponse> testConfig(@PathVariable Long id,
                                                         @Valid @RequestBody AIConfigTestRequest testRequest) {
        Long userId = authService.getCurrentUser().getId();
        AIConfigTestResponse response = userAIConfigService.testAIConfigAdvanced(userId, id, testRequest);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/stats")
    @Operation(summary = "获取AI配置使用统计", description = "获取指定AI配置的使用统计信息")
    public ResponseEntity<AIUsageStatsDTO> getUsageStats(@PathVariable Long id,
                                                        @RequestParam(required = false) LocalDateTime startTime,
                                                        @RequestParam(required = false) LocalDateTime endTime) {
        Long userId = authService.getCurrentUser().getId();
        AIUsageStatsDTO stats = userAIConfigService.getConfigUsageStats(userId, id, startTime, endTime);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/providers")
    @Operation(summary = "获取AI提供商信息", description = "获取支持的AI提供商列表和相关信息")
    public ResponseEntity<List<AIProviderInfo>> getProviders() {
        List<AIProviderInfo> providers = userAIConfigService.getAvailableProviders();
        return ResponseEntity.ok(providers);
    }
    
    @PostMapping("/{id}/reset-stats")
    @Operation(summary = "重置使用统计", description = "重置指定AI配置的使用统计")
    public ResponseEntity<Void> resetStats(@PathVariable Long id) {
        Long userId = authService.getCurrentUser().getId();
        userAIConfigService.resetConfigStats(userId, id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/import")
    @Operation(summary = "导入AI配置", description = "从配置数据导入AI配置")
    public ResponseEntity<UserAIConfigResponse> importConfig(@RequestBody Map<String, Object> configData) {
        Long userId = authService.getCurrentUser().getId();
        UserAIConfigResponse config = userAIConfigService.importConfig(userId, configData);
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }
    
    @GetMapping("/{id}/export")
    @Operation(summary = "导出AI配置", description = "导出指定AI配置的数据")
    public ResponseEntity<Map<String, Object>> exportConfig(@PathVariable Long id) {
        Long userId = authService.getCurrentUser().getId();
        Map<String, Object> configData = userAIConfigService.exportConfig(userId, id);
        return ResponseEntity.ok(configData);
    }
    
    @PostMapping("/{id}/set-default")
    @Operation(summary = "设置默认配置", description = "将指定配置设置为默认AI配置")
    public ResponseEntity<Void> setDefaultConfig(@PathVariable Long id) {
        Long userId = authService.getCurrentUser().getId();
        userAIConfigService.setDefaultAIConfig(userId, id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/stats/summary")
    @Operation(summary = "获取用户总体统计", description = "获取当前用户的AI使用总体统计")
    public ResponseEntity<Map<String, Object>> getUserStats(@RequestParam(required = false) LocalDateTime startTime,
                                                           @RequestParam(required = false) LocalDateTime endTime) {
        Long userId = authService.getCurrentUser().getId();
        Map<String, Object> stats = userAIConfigService.getUserTotalStats(userId);
        return ResponseEntity.ok(stats);
    }
}
