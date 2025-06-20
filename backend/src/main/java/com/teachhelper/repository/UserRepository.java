package com.teachhelper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Role;
import com.teachhelper.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    /**
     * 根据角色查找用户
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") Role role);
    
    @Modifying
    @Query("UPDATE User u SET u.llmApiCallCountToday = u.llmApiCallCountToday + 1 WHERE u.id = :userId")
    void incrementLlmApiCallCount(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE User u SET u.llmApiCallCountToday = 0")
    void resetDailyLlmApiCallCounts();
}
