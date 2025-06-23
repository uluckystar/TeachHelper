package com.teachhelper.entity;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {
    
    @NotBlank
    @Size(min = 2, max = 50)
    @Column(unique = true, nullable = false)
    private String username;
    
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;
    
    @NotBlank
    @Size(min = 6)
    @Column(nullable = false)
    private String password;
    
    @Column(name = "organization_id")
    private String organizationId;
    
    @Column(name = "llm_api_call_limit_per_day")
    private Integer llmApiCallLimitPerDay = 100;
    
    @Column(name = "llm_api_call_count_today")
    private Integer llmApiCallCountToday = 0;
    
    @Column(name = "balance", precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "enabled")
    private boolean enabled = true;
    
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<Role> roles;
    
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Exam> exams;
    
    // 学生扩展字段
    @Column(name = "student_number", unique = true, length = 20)
    private String studentNumber; // 学号，用于前端显示
    
    @Column(name = "class_name", length = 100)
    private String className; // 班级名称
    
    @Column(name = "major", length = 100)
    private String major; // 专业
    
    // 教师扩展字段
    @Column(name = "employee_number", unique = true, length = 20)
    private String employeeNumber; // 工号
    
    @Column(name = "department", length = 100)
    private String department; // 所属部门
    
    @Column(name = "title", length = 50)
    private String title; // 职称（如教授、副教授、讲师等）
    
    // 通用扩展字段
    @Column(name = "real_name", length = 100)
    private String realName; // 真实姓名
    
    @Column(name = "phone", length = 20)
    private String phone; // 电话号码
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl; // 头像URL
    
    // Constructors
    public User() {}
    
    public User(String username, String email, String password, Set<Role> roles) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }
    
    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    public Integer getLlmApiCallLimitPerDay() {
        return llmApiCallLimitPerDay;
    }
    
    public void setLlmApiCallLimitPerDay(Integer llmApiCallLimitPerDay) {
        this.llmApiCallLimitPerDay = llmApiCallLimitPerDay;
    }
    
    public Integer getLlmApiCallCountToday() {
        return llmApiCallCountToday;
    }
    
    public void setLlmApiCallCountToday(Integer llmApiCallCountToday) {
        this.llmApiCallCountToday = llmApiCallCountToday;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Set<Role> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
    
    public Set<Exam> getExams() {
        return exams;
    }
    
    public void setExams(Set<Exam> exams) {
        this.exams = exams;
    }
    
    // 学生扩展字段的 getter 和 setter
    public String getStudentNumber() {
        return studentNumber;
    }
    
    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getMajor() {
        return major;
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
    
    // 教师扩展字段的 getter 和 setter
    public String getEmployeeNumber() {
        return employeeNumber;
    }
    
    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    // 通用扩展字段的 getter 和 setter
    public String getRealName() {
        return realName;
    }
    
    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    // 便捷方法：判断用户角色
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }
    
    public boolean isStudent() {
        return hasRole(Role.STUDENT);
    }
    
    public boolean isTeacher() {
        return hasRole(Role.TEACHER);
    }
    
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
    
    // 向后兼容方法，为了支持原来依赖Student实体的代码
    public String getName() {
        // 优先返回真实姓名，否则返回用户名
        return realName != null && !realName.trim().isEmpty() ? realName : username;
    }
    
    public String getStudentId() {
        // 优先返回学号，否则返回用户ID的字符串形式
        return studentNumber != null && !studentNumber.trim().isEmpty() ? studentNumber : String.valueOf(getId());
    }
}
