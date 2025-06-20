package com.teachhelper.exception;

/**
 * 评估相关异常
 */
public class EvaluationException extends RuntimeException {
    
    public EvaluationException(String message) {
        super(message);
    }
    
    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public EvaluationException(Throwable cause) {
        super(cause);
    }
}
