/*
 * TEMPORARILY DISABLED - DevAnswerDataService
 * This service needs to be updated to use User entities instead of Student entities
 * during the migration from Student-based to User-based architecture.
 * 
 * TODO: Update this service to use User entities and restore full functionality
 */

package com.teachhelper.service.dev;

import org.springframework.stereotype.Service;

@Service
public class DevAnswerDataService {
    
    // All methods temporarily disabled during User migration
    public void generateExamAnswersData(Long examId) {
        throw new UnsupportedOperationException("DevAnswerDataService is temporarily disabled during User migration");
    }
    
    public void clearExamAnswersData(Long examId) {
        throw new UnsupportedOperationException("DevAnswerDataService is temporarily disabled during User migration");
    }
    
    public void generateClassroomExamAnswersData(Long examId, Long classroomId) {
        throw new UnsupportedOperationException("DevAnswerDataService is temporarily disabled during User migration");
    }
    
    public void clearAllAnswersData() {
        throw new UnsupportedOperationException("DevAnswerDataService is temporarily disabled during User migration");
    }
    
    public void createStudentAnswers() {
        throw new UnsupportedOperationException("DevAnswerDataService is temporarily disabled during User migration");
    }
}
