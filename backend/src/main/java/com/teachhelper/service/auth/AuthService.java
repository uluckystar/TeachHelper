package com.teachhelper.service.auth;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.dto.request.LoginRequest;
import com.teachhelper.dto.request.RegisterRequest;
import com.teachhelper.dto.response.LoginResponse;
import com.teachhelper.entity.Role;
import com.teachhelper.entity.User;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.security.JwtTokenProvider;

@Service
@Transactional
public class AuthService {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        User user = userRepository.findByUsername(loginRequest.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        return new LoginResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }
    
    public User register(RegisterRequest registerRequest) {
        // Check if username exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在！");
        }
        
        // Check if email exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被注册！");
        }
        
        // Validate roles - only allow TEACHER and STUDENT registration
        Set<Role> roles = registerRequest.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = Set.of(Role.STUDENT); // Default to STUDENT role
        } else {
            // Check if any invalid roles are being requested
            for (Role role : roles) {
                if (role == Role.ADMIN) {
                    throw new RuntimeException("不允许通过注册创建管理员账户！");
                }
                if (role != Role.TEACHER && role != Role.STUDENT) {
                    throw new RuntimeException("只允许注册教师或学生角色！");
                }
            }
        }
        
        // Create new user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRoles(roles);
        
        // Set default properties for new users
        user.setEnabled(true);
        user.setLlmApiCallLimitPerDay(100); // Default API call limit
        user.setLlmApiCallCountToday(0);
        
        return userRepository.save(user);
    }
    
    /**
     * 管理员专用注册方法
     * 只能由管理员调用，用于创建新的管理员账户
     */
    public User registerAdmin(RegisterRequest registerRequest) {
        // Check if username exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在！");
        }
        
        // Check if email exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被注册！");
        }
        
        // Create new admin user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRoles(Set.of(Role.ADMIN)); // Force admin role
        
        // Set default properties for new admin users
        user.setEnabled(true);
        user.setLlmApiCallLimitPerDay(500); // Higher limit for admins
        user.setLlmApiCallCountToday(0);
        
        return userRepository.save(user);
    }
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("当前用户不存在"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("用户ID: " + id + " 不存在"));
    }

    /**
     * 通过用户名获取用户ID
     * @param username 用户名
     * @return 用户ID，如果用户不存在则返回null
     */
    public Long getUserIdByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        
        return userRepository.findByUsername(username.trim())
            .map(User::getId)
            .orElse(null);
    }
}
