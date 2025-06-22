package com.teachhelper.controller.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.dto.request.RegisterRequest;
import com.teachhelper.entity.Role;
import com.teachhelper.entity.User;
import com.teachhelper.service.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理员用户管理控制器
 * 只有管理员才能访问，用于创建管理员账户等高级操作
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "管理员用户管理", description = "管理员专用的用户管理接口")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/create-admin")
    @Operation(summary = "创建管理员账户", description = "只有管理员可以创建新的管理员账户")
    public ResponseEntity<?> createAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // 强制设置为管理员角色
            registerRequest.setRoles(Set.of(Role.ADMIN));
            
            User user = authService.registerAdmin(registerRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "管理员账户创建成功");
            response.put("username", user.getUsername());
            response.put("roles", user.getRoles());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "创建失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "系统错误");
            errorResponse.put("message", "创建过程中发生错误，请稍后重试");
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/create-user")
    @Operation(summary = "创建用户账户", description = "管理员创建教师或学生账户")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // 验证角色只能是教师或学生
            Set<Role> roles = registerRequest.getRoles();
            if (roles == null || roles.isEmpty()) {
                registerRequest.setRoles(Set.of(Role.STUDENT));
            } else {
                for (Role role : roles) {
                    if (role == Role.ADMIN) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "权限错误");
                        errorResponse.put("message", "请使用专门的创建管理员接口");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    if (role != Role.TEACHER && role != Role.STUDENT) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "角色错误");
                        errorResponse.put("message", "只能创建教师或学生账户");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                }
            }
            
            User user = authService.register(registerRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "用户账户创建成功");
            response.put("username", user.getUsername());
            response.put("roles", user.getRoles());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "创建失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "系统错误");
            errorResponse.put("message", "创建过程中发生错误，请稍后重试");
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
