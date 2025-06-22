package com.teachhelper.controller.auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.dto.request.LoginRequest;
import com.teachhelper.dto.request.RegisterRequest;
import com.teachhelper.dto.response.LoginResponse;
import com.teachhelper.entity.User;
import com.teachhelper.service.auth.AuthService;
import com.teachhelper.validator.RegisterRequestValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private RegisterRequestValidator registerRequestValidator;
    
    @InitBinder("registerRequest")
    public void initRegisterBinder(WebDataBinder binder) {
        binder.addValidators(registerRequestValidator);
    }
    
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码进行登录")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户账户，只允许注册教师和学生角色")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest, 
                                     BindingResult bindingResult) {
        // 检查验证错误
        if (bindingResult.hasErrors()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "验证失败");
            errorResponse.put("message", bindingResult.getAllErrors().get(0).getDefaultMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            // 服务层会进行角色验证和安全检查
            User user = authService.register(registerRequest);
            
            // 返回成功信息，不包含敏感数据
            Map<String, Object> response = new HashMap<>();
            response.put("message", "用户注册成功");
            response.put("username", user.getUsername());
            response.put("roles", user.getRoles());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // 返回详细的错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "注册失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            // 记录系统错误但不暴露给客户端
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "系统错误");
            errorResponse.put("message", "注册过程中发生错误，请稍后重试");
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public ResponseEntity<User> getCurrentUser() {
        User user = authService.getCurrentUser();
        // Remove sensitive information
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}
