package com.teachhelper.config;

import com.teachhelper.security.CustomUserDetailsService;
import com.teachhelper.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/dev/init-data").permitAll() // 开发阶段数据初始化端点
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Migration endpoints (Admin only)
                .requestMatchers("/api/migration/**").hasRole("ADMIN")
                
                // Metadata endpoints
                .requestMatchers("/api/metadata/**").hasAnyRole("TEACHER", "ADMIN")
                
                // Dev endpoints (需要认证)
                .requestMatchers("/api/dev/**").hasRole("ADMIN")
                
                // Teacher and Admin can manage exams and questions, Students can view exams for taking
                .requestMatchers("/api/exams", "/api/exams/").hasAnyRole("TEACHER", "ADMIN", "STUDENT") // 学生可以查看考试列表
                .requestMatchers("/api/exams/student/**").hasRole("STUDENT") // 学生专用API路径，必须在其他规则之前
                .requestMatchers("/api/exams/available").hasRole("STUDENT") // 学生可以查看可参加的考试
                .requestMatchers("/api/exams/*/take").hasAnyRole("TEACHER", "ADMIN", "STUDENT") // 学生可以参加考试（如果需要专门的API）
                .requestMatchers("/api/exams/*/statistics").hasAnyRole("TEACHER", "ADMIN") // 统计信息仅限教师和管理员
                .requestMatchers("/api/exams/*/classrooms").hasAnyRole("TEACHER", "ADMIN") // 班级管理仅限教师和管理员
                .requestMatchers("/api/exams/*/publish").hasAnyRole("TEACHER", "ADMIN") // 发布考试仅限教师和管理员
                .requestMatchers("/api/exams/*/unpublish").hasAnyRole("TEACHER", "ADMIN") // 撤销发布仅限教师和管理员
                .requestMatchers("/api/exams/*/end").hasAnyRole("TEACHER", "ADMIN") // 结束考试仅限教师和管理员
                .requestMatchers("/api/exams/*").hasAnyRole("TEACHER", "ADMIN", "STUDENT") // 学生可以查看考试基本信息（用于参加考试）
                .requestMatchers("/api/exams/**").hasAnyRole("TEACHER", "ADMIN") // 其他考试管理功能仅限教师和管理员
                .requestMatchers("/api/questions/exam/*/take").hasAnyRole("TEACHER", "ADMIN", "STUDENT") // 学生可以在参加考试时获取题目
                .requestMatchers("/api/questions/**").hasAnyRole("TEACHER", "ADMIN") // 其他题目管理功能仅限教师和管理员
                
                // Evaluation endpoints
                .requestMatchers("/api/evaluations/**").hasAnyRole("TEACHER", "ADMIN")
                
                // Student answer endpoints
                .requestMatchers("/api/student-answers/**").hasAnyRole("TEACHER", "ADMIN", "STUDENT")
                
                // AI service endpoints
                .requestMatchers("/api/ai/**").hasAnyRole("TEACHER", "ADMIN")
                
                // User AI config endpoints
                .requestMatchers("/api/user-ai-config/**").hasAnyRole("TEACHER", "ADMIN")
                
                // All other requests need authentication
                .anyRequest().authenticated()
            );
        
        // Disable frame options for H2 console
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 开发环境允许前端域名
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",  // Vite 开发服务器默认端口
            "http://localhost:3000",  // 备用端口
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000"
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 预检请求缓存时间
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
