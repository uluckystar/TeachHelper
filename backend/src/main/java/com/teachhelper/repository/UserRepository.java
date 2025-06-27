package com.teachhelper.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.teachhelper.entity.Role;
import com.teachhelper.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByStudentNumber(String studentNumber);
    
    boolean existsByStudentNumber(String studentNumber);
    
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
    
    /**
     * 根据真实姓名模糊查询用户
     */
    List<User> findByRealNameContaining(String realName);
    
    /**
     * 根据学号模糊查询用户
     */
    List<User> findByStudentNumberContaining(String studentNumber);

    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findByIds(@Param("ids") Set<Long> ids);
    
    @Query("SELECT s FROM Classroom c JOIN c.students s WHERE c.id = :classroomId")
    List<User> findStudentsByClassroomId(@Param("classroomId") Long classroomId);

    @Query("SELECT DISTINCT s FROM Exam e JOIN e.targetClassrooms c JOIN c.students s WHERE e.id = :examId")
    List<User> findUsersByExamId(@Param("examId") Long examId);
}
