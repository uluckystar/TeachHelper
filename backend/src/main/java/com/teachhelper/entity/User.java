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
}
