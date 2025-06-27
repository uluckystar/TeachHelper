package com.teachhelper.dto.request;

import java.util.List;
import java.util.Objects;

public class StudentAnswerImportData {
    
    // 学生基本信息
    private String studentName;        // 学生姓名
    private String studentNumber;      // 学号
    private String college;           // 学院
    private String major;             // 专业
    private String className;         // 班级
    
    // 考试信息
    private String examTitle;         // 考试标题
    private String subject;           // 科目
    
    // 答案列表
    private List<QuestionAnswer> answers;
    
    // 无参构造函数
    public StudentAnswerImportData() {}
    
    // 全参构造函数
    public StudentAnswerImportData(String studentName, String studentNumber, String college, String major, String className, String examTitle, String subject, List<QuestionAnswer> answers) {
        this.studentName = studentName;
        this.studentNumber = studentNumber;
        this.college = college;
        this.major = major;
        this.className = className;
        this.examTitle = examTitle;
        this.subject = subject;
        this.answers = answers;
    }
    
    // Getter方法
    public String getStudentName() {
        return studentName;
    }
    
    public String getStudentNumber() {
        return studentNumber;
    }
    
    public String getCollege() {
        return college;
    }
    
    public String getMajor() {
        return major;
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getExamTitle() {
        return examTitle;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public List<QuestionAnswer> getAnswers() {
        return answers;
    }
    
    // Setter方法
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
    
    public void setCollege(String college) {
        this.college = college;
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public void setAnswers(List<QuestionAnswer> answers) {
        this.answers = answers;
    }
    
    /**
     * 获取学生信息（兼容方法）
     */
    public StudentInfo getStudentInfo() {
        StudentInfo info = new StudentInfo();
        info.setStudentName(this.studentName);
        info.setStudentNumber(this.studentNumber);
        info.setCollege(this.college);
        info.setMajor(this.major);
        info.setClassName(this.className);
        return info;
    }
    
    /**
     * 获取问题答案列表（兼容方法）
     */
    public List<QuestionAnswer> getQuestions() {
        return this.answers;
    }
    
    // equals方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentAnswerImportData that = (StudentAnswerImportData) o;
        return Objects.equals(studentName, that.studentName) &&
               Objects.equals(studentNumber, that.studentNumber) &&
               Objects.equals(college, that.college) &&
               Objects.equals(major, that.major) &&
               Objects.equals(className, that.className) &&
               Objects.equals(examTitle, that.examTitle) &&
               Objects.equals(subject, that.subject) &&
               Objects.equals(answers, that.answers);
    }
    
    // hashCode方法
    @Override
    public int hashCode() {
        return Objects.hash(studentName, studentNumber, college, major, className, examTitle, subject, answers);
    }
    
    // toString方法
    @Override
    public String toString() {
        return "StudentAnswerImportData{" +
                "studentName='" + studentName + '\'' +
                ", studentNumber='" + studentNumber + '\'' +
                ", college='" + college + '\'' +
                ", major='" + major + '\'' +
                ", className='" + className + '\'' +
                ", examTitle='" + examTitle + '\'' +
                ", subject='" + subject + '\'' +
                ", answers=" + answers +
                '}';
    }
    
    public static class StudentInfo {
        private String studentName;
        private String studentNumber;
        private String college;
        private String major;
        private String className;
        
        // 无参构造函数
        public StudentInfo() {}
        
        // 全参构造函数
        public StudentInfo(String studentName, String studentNumber, String college, String major, String className) {
            this.studentName = studentName;
            this.studentNumber = studentNumber;
            this.college = college;
            this.major = major;
            this.className = className;
        }
        
        // Getter方法
        public String getStudentName() {
            return studentName;
        }
        
        public String getStudentNumber() {
            return studentNumber;
        }
        
        public String getCollege() {
            return college;
        }
        
        public String getMajor() {
            return major;
        }
        
        public String getClassName() {
            return className;
        }
        
        // Setter方法
        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }
        
        public void setStudentNumber(String studentNumber) {
            this.studentNumber = studentNumber;
        }
        
        public void setCollege(String college) {
            this.college = college;
        }
        
        public void setMajor(String major) {
            this.major = major;
        }
        
        public void setClassName(String className) {
            this.className = className;
        }
        
        // equals方法
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StudentInfo that = (StudentInfo) o;
            return Objects.equals(studentName, that.studentName) &&
                   Objects.equals(studentNumber, that.studentNumber) &&
                   Objects.equals(college, that.college) &&
                   Objects.equals(major, that.major) &&
                   Objects.equals(className, that.className);
        }
        
        // hashCode方法
        @Override
        public int hashCode() {
            return Objects.hash(studentName, studentNumber, college, major, className);
        }
        
        // toString方法
        @Override
        public String toString() {
            return "StudentInfo{" +
                    "studentName='" + studentName + '\'' +
                    ", studentNumber='" + studentNumber + '\'' +
                    ", college='" + college + '\'' +
                    ", major='" + major + '\'' +
                    ", className='" + className + '\'' +
                    '}';
        }
    }
    
    public static class QuestionAnswer {
        private Integer questionNumber;    // 题目序号
        private String questionContent;    // 题目内容
        private String answerContent;      // 答案内容
        private Long questionId;          // 题目ID（匹配时设置）
        private Double score;             // 分数（可选）
        private String sectionHeader;     // 段落标题（如"一、选择题"）
        
        // 无参构造函数
        public QuestionAnswer() {}
        
        // 全参构造函数
        public QuestionAnswer(Integer questionNumber, String questionContent, String answerContent, Long questionId, Double score, String sectionHeader) {
            this.questionNumber = questionNumber;
            this.questionContent = questionContent;
            this.answerContent = answerContent;
            this.questionId = questionId;
            this.score = score;
            this.sectionHeader = sectionHeader;
        }
        
        // Getter方法
        public Integer getQuestionNumber() {
            return questionNumber;
        }
        
        public String getQuestionContent() {
            return questionContent;
        }
        
        public String getAnswerContent() {
            return answerContent;
        }
        
        public Long getQuestionId() {
            return questionId;
        }
        
        public Double getScore() {
            return score;
        }
        
        public String getSectionHeader() {
            return sectionHeader;
        }
        
        // Setter方法
        public void setQuestionNumber(Integer questionNumber) {
            this.questionNumber = questionNumber;
        }
        
        public void setQuestionContent(String questionContent) {
            this.questionContent = questionContent;
        }
        
        public void setAnswerContent(String answerContent) {
            this.answerContent = answerContent;
        }
        
        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }
        
        public void setScore(Double score) {
            this.score = score;
        }
        
        public void setSectionHeader(String sectionHeader) {
            this.sectionHeader = sectionHeader;
        }
        
        // equals方法
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuestionAnswer that = (QuestionAnswer) o;
            return Objects.equals(questionNumber, that.questionNumber) &&
                   Objects.equals(questionContent, that.questionContent) &&
                   Objects.equals(answerContent, that.answerContent) &&
                   Objects.equals(questionId, that.questionId) &&
                   Objects.equals(score, that.score) &&
                   Objects.equals(sectionHeader, that.sectionHeader);
        }
        
        // hashCode方法
        @Override
        public int hashCode() {
            return Objects.hash(questionNumber, questionContent, answerContent, questionId, score, sectionHeader);
        }
        
        // toString方法
        @Override
        public String toString() {
            return "QuestionAnswer{" +
                    "questionNumber=" + questionNumber +
                    ", questionContent='" + questionContent + '\'' +
                    ", answerContent='" + answerContent + '\'' +
                    ", questionId=" + questionId +
                    ", score=" + score +
                    ", sectionHeader='" + sectionHeader + '\'' +
                    '}';
        }
    }
} 