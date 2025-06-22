package com.teachhelper.validator;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.teachhelper.dto.request.RegisterRequest;
import com.teachhelper.entity.Role;

/**
 * 注册请求验证器
 * 确保注册请求的安全性和有效性
 */
@Component
public class RegisterRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RegisterRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegisterRequest request = (RegisterRequest) target;
        
        // 验证角色
        Set<Role> roles = request.getRoles();
        if (roles != null && !roles.isEmpty()) {
            for (Role role : roles) {
                if (role == Role.ADMIN) {
                    errors.rejectValue("roles", "invalid.role.admin", 
                        "不允许通过注册创建管理员账户");
                }
                if (role != Role.TEACHER && role != Role.STUDENT) {
                    errors.rejectValue("roles", "invalid.role.type", 
                        "只允许注册教师或学生角色");
                }
            }
            
            // 确保只有一个角色
            if (roles.size() > 1) {
                errors.rejectValue("roles", "invalid.role.multiple", 
                    "一个用户只能有一个角色");
            }
        }
        
        // 验证用户名格式
        String username = request.getUsername();
        if (username != null) {
            // 检查字符是否合法（中文、英文、数字、下划线）
            if (!username.matches("^[\\u4e00-\\u9fff\\u3400-\\u4dbfa-zA-Z0-9_]+$")) {
                errors.rejectValue("username", "invalid.username.format", 
                    "用户名只能包含中文、英文、数字和下划线");
                return; // 如果字符不合法，不继续验证长度
            }
            
            // 检查长度：中文字符2-50个，英文字符3-50个
            long chineseCharCount = username.chars()
                .filter(c -> (c >= 0x4e00 && c <= 0x9fff) || (c >= 0x3400 && c <= 0x4dbf))
                .count();
            int totalLength = username.length();
            
            if (totalLength > 50) {
                errors.rejectValue("username", "invalid.username.length", 
                    "用户名长度不能超过50个字符");
            } else if (chineseCharCount > 0) {
                // 如果包含中文字符，最少2个字符
                if (totalLength < 2) {
                    errors.rejectValue("username", "invalid.username.length", 
                        "包含中文的用户名至少需要2个字符");
                }
            } else {
                // 纯英文数字组合，最少3个字符
                if (totalLength < 3) {
                    errors.rejectValue("username", "invalid.username.length", 
                        "英文用户名至少需要3个字符");
                }
            }
            
            // 用户名不能是保留词（包括中英文）
            String[] reservedWords = {"admin", "root", "system", "administrator", "teacher", "student", 
                                    "管理员", "教师", "学生", "系统", "超级用户"};
            for (String reserved : reservedWords) {
                if (username.toLowerCase().equals(reserved.toLowerCase())) {
                    errors.rejectValue("username", "invalid.username.reserved", 
                        "用户名不能使用保留词");
                }
            }
        }
        
        // 验证密码强度
        String password = request.getPassword();
        if (password != null) {
            if (password.length() < 8) {
                errors.rejectValue("password", "invalid.password.length", 
                    "密码长度至少8个字符");
            }
            
            // 检查是否包含字母和数字
            boolean hasLetter = password.matches(".*[a-zA-Z].*");
            boolean hasDigit = password.matches(".*[0-9].*");
            
            if (!hasLetter || !hasDigit) {
                errors.rejectValue("password", "invalid.password.complexity", 
                    "密码必须包含字母和数字");
            }
            
            // 检查常见弱密码
            String[] weakPasswords = {"password", "123456", "12345678", "qwerty", "abc123"};
            for (String weak : weakPasswords) {
                if (password.toLowerCase().contains(weak)) {
                    errors.rejectValue("password", "invalid.password.weak", 
                        "密码过于简单，请使用更强的密码");
                }
            }
        }
        
        // 验证邮箱域名（可选的业务规则）
        String email = request.getEmail();
        if (email != null) {
            // 这里可以添加邮箱域名白名单验证
            // 例如：只允许教育机构邮箱注册教师账户
            if (roles != null && roles.contains(Role.TEACHER)) {
                if (!isEducationalEmail(email)) {
                    // 注释掉严格的教育邮箱检查，根据需要启用
                    // errors.rejectValue("email", "invalid.email.domain", 
                    //     "教师注册需要使用教育机构邮箱");
                }
            }
        }
    }
    
    /**
     * 检查是否为教育机构邮箱
     */
    private boolean isEducationalEmail(String email) {
        if (email == null) return false;
        
        String[] educationalDomains = {
            ".edu", ".edu.cn", ".ac.cn", ".university", ".college"
        };
        
        String lowerEmail = email.toLowerCase();
        for (String domain : educationalDomains) {
            if (lowerEmail.contains(domain)) {
                return true;
            }
        }
        
        return false;
    }
}
