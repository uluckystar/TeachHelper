package com.teachhelper.service.dev;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.Exam;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionBank;
import com.teachhelper.entity.QuestionOption;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.entity.RubricCriterion;
import com.teachhelper.entity.User;
import com.teachhelper.repository.ExamRepository;
import com.teachhelper.repository.QuestionBankRepository;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.RubricCriterionRepository;
import com.teachhelper.repository.UserRepository;

/**
 * 开发环境题目数据服务
 * 负责创建题目、选项和评分标准的示例数据
 */
@Service
public class DevQuestionDataService {
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionBankRepository questionBankRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RubricCriterionRepository rubricCriterionRepository;
    
    /**
     * 创建题目和评分标准数据
     */
    @Transactional
    public void createQuestionsAndRubrics() {
        List<Exam> exams = examRepository.findAll();
        if (exams.isEmpty()) {
            throw new RuntimeException("请先创建考试数据");
        }
        
        List<User> teachers = userRepository.findAll().stream()
            .filter(u -> u.getUsername().startsWith("teacher"))
            .toList();
        if (teachers.isEmpty()) {
            throw new RuntimeException("请先创建教师用户数据");
        }
        
        // 创建题目库
        List<QuestionBank> questionBanks = createQuestionBanks(teachers);
        
        // 创建题目
        List<Question> questions = createQuestions(exams, questionBanks);
        
        // 创建评分标准
        createRubricCriteria(questions);
        
        // 为选择题创建选项
        createQuestionOptions(questions);
    }
    
    /**
     * 清空题目相关数据
     */
    @Transactional
    public void clearQuestionData() {
        rubricCriterionRepository.deleteAll();
        questionRepository.deleteAll();
        questionBankRepository.deleteAll();
    }
    
    /**
     * 创建题目库数据
     */
    private List<QuestionBank> createQuestionBanks(List<User> teachers) {
        List<QuestionBank> questionBanks = new ArrayList<>();
        
        // 创建题目库 - 使用构造后设置方式
        questionBanks.add(createQuestionBank("Java编程题库", "Java基础编程相关题目", 
            "Java编程", "本科", teachers.get(0).getId()));
        questionBanks.add(createQuestionBank("数据库系统题库", "数据库原理与应用相关题目", 
            "数据库", "本科", teachers.get(1 % teachers.size()).getId()));
        questionBanks.add(createQuestionBank("计算机网络题库", "计算机网络技术相关题目", 
            "计算机网络", "本科", teachers.get(2 % teachers.size()).getId()));
        questionBanks.add(createQuestionBank("算法与数据结构题库", "算法设计与数据结构相关题目", 
            "算法", "本科", teachers.get(3 % teachers.size()).getId()));
        questionBanks.add(createQuestionBank("操作系统题库", "操作系统原理相关题目", 
            "操作系统", "本科", teachers.get(0).getId()));
        questionBanks.add(createQuestionBank("软件工程题库", "软件工程方法与实践相关题目", 
            "软件工程", "本科", teachers.get(1 % teachers.size()).getId()));
        questionBanks.add(createQuestionBank("Web开发技术题库", "Web前端与后端开发相关题目", 
            "Web开发", "本科", teachers.get(2 % teachers.size()).getId()));
        
        return questionBankRepository.saveAll(questionBanks);
    }
    
    /**
     * 创建单个题目库的辅助方法
     */
    private QuestionBank createQuestionBank(String name, String description, String subject, 
                                          String gradeLevel, Long createdBy) {
        QuestionBank bank = new QuestionBank();
        // 使用反射来设置私有字段
        try {
            setField(bank, "name", name);
            setField(bank, "description", description);
            setField(bank, "subject", subject);
            setField(bank, "gradeLevel", gradeLevel);
            setField(bank, "createdBy", createdBy);
            setField(bank, "isPublic", true);
            setField(bank, "isActive", true);
        } catch (Exception e) {
            throw new RuntimeException("创建题目库失败: " + e.getMessage(), e);
        }
        return bank;
    }
    
    /**
     * 使用反射设置字段值
     */
    private void setField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
    
    /**
     * 创建题目数据
     */
    private List<Question> createQuestions(List<Exam> exams, List<QuestionBank> questionBanks) {
        List<Question> questions = new ArrayList<>();
        
        // 获取各个考试
        Exam javaExam = findExamByTitle(exams, "Java基础编程考试");
        Exam dsExam = findExamByTitle(exams, "数据结构与算法考试");
        Exam webExam = findExamByTitle(exams, "Web开发技术考试");
        Exam networkExam = findExamByTitle(exams, "计算机网络考试");
        Exam dbExam = findExamByTitle(exams, "数据库系统考试");
        Exam seExam = findExamByTitle(exams, "软件工程考试");
        Exam algorithmExam = findExamByTitle(exams, "算法与数据结构考试");
        Exam osExam = findExamByTitle(exams, "操作系统考试");
        
        // 获取各个题目库
        QuestionBank javaBank = findQuestionBankBySubject(questionBanks, "Java编程");
        QuestionBank dbBank = findQuestionBankBySubject(questionBanks, "数据库");
        QuestionBank webBank = findQuestionBankBySubject(questionBanks, "Web开发");
        QuestionBank networkBank = findQuestionBankBySubject(questionBanks, "计算机网络");
        QuestionBank algorithmBank = findQuestionBankBySubject(questionBanks, "算法");
        QuestionBank osBank = findQuestionBankBySubject(questionBanks, "操作系统");
        QuestionBank seBank = findQuestionBankBySubject(questionBanks, "软件工程");
        
        // Java考试题目
        if (javaExam != null) {
            // 1. 论述题 - 面向对象编程
            Question q1 = createQuestion(javaExam, javaBank, "面向对象编程概念阐述", 
                "请详细阐述面向对象编程的三大特性（封装、继承、多态）及其在实际开发中的应用。", 
                QuestionType.ESSAY, new BigDecimal("25.00"));
            q1.setReferenceAnswer("封装：隐藏对象内部实现细节，仅暴露必要接口；继承：子类继承父类特性并可扩展；多态：同一接口可有多种实现形式。");
            questions.add(q1);
            
            // 2. 简答题 - 异常处理
            Question q2 = createQuestion(javaExam, javaBank, "Java异常处理机制", 
                "简述Java中异常处理的机制，包括try-catch-finally语句的使用和自定义异常的创建。", 
                QuestionType.SHORT_ANSWER, new BigDecimal("15.00"));
            q2.setReferenceAnswer("try-catch捕获异常，finally确保资源释放，throws声明异常，自定义异常继承Exception类。");
            questions.add(q2);
            
            // 3. 编程题 - 算法实现
            Question q3 = createQuestion(javaExam, javaBank, "链表反转实现", 
                "用Java实现单向链表的反转算法，要求时间复杂度O(n)，空间复杂度O(1)。", 
                QuestionType.CODING, new BigDecimal("30.00"));
            q3.setReferenceAnswer("使用三个指针prev、current、next，迭代遍历链表，逐步调整指针方向实现反转。");
            questions.add(q3);
            
            // 4. 单选题 - Java基础
            Question q4 = createQuestion(javaExam, javaBank, "Java访问修饰符", 
                "在Java中，以下哪个访问修饰符的访问范围最大？", 
                QuestionType.SINGLE_CHOICE, new BigDecimal("5.00"));
            questions.add(q4);
            
            // 5. 多选题 - Java集合
            Question q19 = createQuestion(javaExam, javaBank, "Java集合特性分析", 
                "以下关于Java集合框架的描述，哪些是正确的？", 
                QuestionType.MULTIPLE_CHOICE, new BigDecimal("8.00"));
            questions.add(q19);
        }
        
        // 数据库考试题目
        if (dbExam != null) {
            // 6. 案例分析题 - SQL查询
            Question q5 = createQuestion(dbExam, dbBank, "SQL查询设计", 
                "设计一个学生管理系统的数据库，包含学生表、课程表、选课表。编写SQL语句查询每个学生的平均成绩，并按平均成绩降序排列。", 
                QuestionType.CASE_ANALYSIS, new BigDecimal("25.00"));
            q5.setReferenceAnswer("CREATE TABLE语句设计三个表，SELECT语句使用JOIN和GROUP BY计算平均成绩，ORDER BY DESC排序。");
            questions.add(q5);
            
            // 7. 简答题 - 数据库设计
            Question q6 = createQuestion(dbExam, dbBank, "数据库设计原则", 
                "简述数据库设计中的范式理论，重点说明第一范式、第二范式和第三范式的要求。", 
                QuestionType.SHORT_ANSWER, new BigDecimal("18.00"));
            q6.setReferenceAnswer("1NF要求属性不可分割；2NF要求非键属性完全依赖主键；3NF要求非键属性不传递依赖主键。");
            questions.add(q6);
            
            // 8. 单选题 - 事务特性
            Question q20 = createQuestion(dbExam, dbBank, "数据库事务ACID特性", 
                "数据库事务的ACID特性中，A代表什么？", 
                QuestionType.SINGLE_CHOICE, new BigDecimal("4.00"));
            questions.add(q20);
        }
        
        // Web开发考试题目
        if (webExam != null) {
            // 9. 编程题 - JavaScript
            Question q7 = createQuestion(webExam, webBank, "JavaScript闭包应用", 
                "使用JavaScript闭包实现一个计数器函数，每次调用返回递增的数字，要求封装计数变量。", 
                QuestionType.CODING, new BigDecimal("20.00"));
            q7.setReferenceAnswer("function createCounter() { let count = 0; return function() { return ++count; }; }");
            questions.add(q7);
            
            // 10. 简答题 - CSS布局
            Question q8 = createQuestion(webExam, webBank, "CSS Flexbox布局", 
                "解释CSS Flexbox布局模型的主要概念和常用属性。", 
                QuestionType.SHORT_ANSWER, new BigDecimal("15.00"));
            q8.setReferenceAnswer("Flexbox包含容器和项目，主轴和交叉轴，常用属性有justify-content、align-items、flex-direction等。");
            questions.add(q8);
        }
        
        // 操作系统考试题目
        if (osExam != null) {
            // 11. 判断题 - 进程管理
            Question q9 = createQuestion(osExam, osBank, "进程与线程关系", 
                "判断题：在同一个进程中的多个线程共享相同的内存空间。", 
                QuestionType.TRUE_FALSE, new BigDecimal("5.00"));
            questions.add(q9);
            
            // 12. 判断题 - 死锁
            Question q10 = createQuestion(osExam, osBank, "银行家算法功效", 
                "判断题：银行家算法可以完全避免死锁的发生。", 
                QuestionType.TRUE_FALSE, new BigDecimal("5.00"));
            questions.add(q10);
        }
        
        // 算法考试题目
        if (algorithmExam != null) {
            // 13. 编程题 - 二叉树
            Question q11 = createQuestion(algorithmExam, algorithmBank, "二叉树遍历算法", 
                "实现二叉树的前序、中序、后序遍历算法（递归和非递归版本），并分析时间空间复杂度。", 
                QuestionType.CODING, new BigDecimal("35.00"));
            q11.setReferenceAnswer("前序遍历：根节点->左子树->右子树；中序遍历：左子树->根节点->右子树；后序遍历：左子树->右子树->根节点。递归版本简单，非递归版本需用栈辅助。时间复杂度均为O(n)，空间复杂度视实现而定。");
            questions.add(q11);
            
            // 14. 论述题 - 动态规划
            Question q12 = createQuestion(algorithmExam, algorithmBank, "动态规划应用", 
                "以最长公共子序列问题为例，详细阐述动态规划算法的设计思路和实现步骤。", 
                QuestionType.ESSAY, new BigDecimal("30.00"));
            q12.setReferenceAnswer("确定状态转移方程：LCS(i,j) = LCS(i-1,j-1)+1 (如果字符相同) 或 max(LCS(i-1,j), LCS(i,j-1))，构建二维DP表，从底向上计算。");
            questions.add(q12);
            
            // 15. 计算题 - 时间复杂度
            Question q13 = createQuestion(algorithmExam, algorithmBank, "算法复杂度分析", 
                "分析以下嵌套循环的时间复杂度：for(i=1;i<=n;i++) for(j=1;j<=i;j++) sum++;", 
                QuestionType.CALCULATION, new BigDecimal("12.00"));
            q13.setReferenceAnswer("外层循环n次，内层循环分别1,2,3...n次，总次数为1+2+...+n=n(n+1)/2，时间复杂度为O(n²)。");
            questions.add(q13);
        }
        
        // 数据结构考试题目
        if (dsExam != null) {
            // 16. 编程题 - 栈应用
            Question q14 = createQuestion(dsExam, algorithmBank, "括号匹配检验", 
                "使用栈数据结构实现括号匹配检验算法，支持()、[]、{}三种括号类型。", 
                QuestionType.CODING, new BigDecimal("25.00"));
            q14.setReferenceAnswer("遍历字符串，遇到左括号入栈，遇到右括号检查栈顶是否匹配并出栈，最后检查栈是否为空。");
            questions.add(q14);
            
            // 17. 简答题 - 图遍历
            Question q15 = createQuestion(dsExam, algorithmBank, "图的遍历算法", 
                "比较深度优先搜索(DFS)和广度优先搜索(BFS)的特点和应用场景。", 
                QuestionType.SHORT_ANSWER, new BigDecimal("16.00"));
            q15.setReferenceAnswer("DFS使用栈（递归），适合路径搜索；BFS使用队列，适合最短路径。DFS空间复杂度O(h)，BFS为O(w)。");
            questions.add(q15);
        }
        
        // 软件工程考试题目
        if (seExam != null) {
            // 18. 案例分析题 - 软件开发模型
            Question q16 = createQuestion(seExam, seBank, "软件开发模型比较", 
                "分析瀑布模型、螺旋模型、敏捷开发模型的特点、优缺点和适用场景。", 
                QuestionType.CASE_ANALYSIS, new BigDecimal("25.00"));
            questions.add(q16);
            
            // 19. 简答题 - 软件测试
            Question q17 = createQuestion(seExam, seBank, "软件测试类型", 
                "简述单元测试、集成测试、系统测试、验收测试的目的和特点。", 
                QuestionType.SHORT_ANSWER, new BigDecimal("18.00"));
            questions.add(q17);
        }
        
        // 计算机网络考试题目
        if (networkExam != null) {
            // 20. 简答题 - OSI模型
            Question q18 = createQuestion(networkExam, networkBank, "OSI七层模型", 
                "简述OSI七层网络模型各层的主要功能和协议示例。", 
                QuestionType.SHORT_ANSWER, new BigDecimal("20.00"));
            q18.setReferenceAnswer("物理层(传输比特流)、数据链路层(帧传输,以太网)、网络层(路由,IP)、传输层(端到端,TCP/UDP)、会话层(会话管理)、表示层(数据格式)、应用层(用户接口,HTTP/FTP)。");
            questions.add(q18);
            
            // 21-25. 更多题目覆盖所有题型
            Question q21 = createQuestion(networkExam, networkBank, "TCP三次握手", 
                "请详细描述TCP三次握手的过程，并说明每一步的作用。", 
                QuestionType.ESSAY, new BigDecimal("22.00"));
            q21.setReferenceAnswer("第一次：客户端发送SYN，第二次：服务器回复SYN+ACK，第三次：客户端发送ACK确认连接建立。");
            questions.add(q21);
            
            Question q22 = createQuestion(networkExam, networkBank, "IP地址分类", 
                "以下IP地址192.168.1.100属于哪一类？", 
                QuestionType.SINGLE_CHOICE, new BigDecimal("6.00"));
            questions.add(q22);
            
            Question q23 = createQuestion(networkExam, networkBank, "子网掩码计算", 
                "计算网络地址192.168.1.0/24可以容纳多少台主机？", 
                QuestionType.CALCULATION, new BigDecimal("10.00"));
            q23.setReferenceAnswer("/24表示前24位为网络位，后8位为主机位，2^8-2=254台主机（减去网络地址和广播地址）。");
            questions.add(q23);
            
            Question q24 = createQuestion(networkExam, networkBank, "网络协议层次", 
                "判断题：HTTP协议工作在OSI模型的应用层。", 
                QuestionType.TRUE_FALSE, new BigDecimal("4.00"));
            questions.add(q24);
            
            Question q25 = createQuestion(networkExam, networkBank, "网络安全威胁", 
                "以下哪些属于常见的网络安全威胁？", 
                QuestionType.MULTIPLE_CHOICE, new BigDecimal("8.00"));
            questions.add(q25);
        }
        
        return questionRepository.saveAll(questions);
    }
    
    /**
     * 创建评分标准
     */
    private void createRubricCriteria(List<Question> questions) {
        List<RubricCriterion> criteria = new ArrayList<>();
        
        // 为每个题目创建评分标准
        for (Question question : questions) {
            if (question.getQuestionType() == QuestionType.ESSAY || 
                question.getQuestionType() == QuestionType.CODING ||
                question.getQuestionType() == QuestionType.CASE_ANALYSIS) {
                
                // 内容完整性标准
                RubricCriterion criterion1 = new RubricCriterion();
                criterion1.setQuestion(question);
                criterion1.setCriterionText("内容完整性和准确性");
                criterion1.setPoints(new BigDecimal("10.00"));
                criterion1.setOrderIndex(1);
                criteria.add(criterion1);
                
                // 逻辑清晰度标准
                RubricCriterion criterion2 = new RubricCriterion();
                criterion2.setQuestion(question);
                criterion2.setCriterionText("逻辑清晰度");
                criterion2.setPoints(new BigDecimal("8.00"));
                criterion2.setOrderIndex(2);
                criteria.add(criterion2);
                
                // 实例举证标准（针对论述题和案例分析题）
                if (question.getQuestionType() == QuestionType.ESSAY || 
                    question.getQuestionType() == QuestionType.CASE_ANALYSIS) {
                    RubricCriterion criterion3 = new RubricCriterion();
                    criterion3.setQuestion(question);
                    criterion3.setCriterionText("实例举证合理性");
                    criterion3.setPoints(new BigDecimal("7.00"));
                    criterion3.setOrderIndex(3);
                    criteria.add(criterion3);
                }
                
                // 代码规范标准（针对编程题）
                if (question.getQuestionType() == QuestionType.CODING) {
                    RubricCriterion criterion4 = new RubricCriterion();
                    criterion4.setQuestion(question);
                    criterion4.setCriterionText("代码规范性和可读性");
                    criterion4.setPoints(new BigDecimal("5.00"));
                    criterion4.setOrderIndex(3);
                    criteria.add(criterion4);
                    
                    RubricCriterion criterion5 = new RubricCriterion();
                    criterion5.setQuestion(question);
                    criterion5.setCriterionText("算法效率和优化");
                    criterion5.setPoints(new BigDecimal("7.00"));
                    criterion5.setOrderIndex(4);
                    criteria.add(criterion5);
                }
            }
        }
        
        rubricCriterionRepository.saveAll(criteria);
    }
    
    /**
     * 为选择题创建选项
     */
    private void createQuestionOptions(List<Question> questions) {
        List<Question> choiceQuestions = questions.stream()
            .filter(q -> q.getQuestionType() == QuestionType.SINGLE_CHOICE || 
                        q.getQuestionType() == QuestionType.MULTIPLE_CHOICE ||
                        q.getQuestionType() == QuestionType.TRUE_FALSE)
            .toList();
        
        for (Question question : choiceQuestions) {
            if (question.getTitle().contains("Java访问修饰符")) {
                // 第4题 - Java访问修饰符单选题
                QuestionOption option1 = new QuestionOption("public", true, 1);
                option1.setQuestion(question);
                QuestionOption option2 = new QuestionOption("protected", false, 2);
                option2.setQuestion(question);
                QuestionOption option3 = new QuestionOption("default", false, 3);
                option3.setQuestion(question);
                QuestionOption option4 = new QuestionOption("private", false, 4);
                option4.setQuestion(question);
                question.addOption(option1);
                question.addOption(option2);
                question.addOption(option3);
                question.addOption(option4);
            } else if (question.getTitle().contains("Java集合特性分析")) {
                // 第19题 - Java集合特性分析多选题
                QuestionOption option1 = new QuestionOption("ArrayList基于数组实现，支持随机访问", true, 1);
                option1.setQuestion(question);
                QuestionOption option2 = new QuestionOption("LinkedList基于链表实现，插入删除效率高", true, 2);
                option2.setQuestion(question);
                QuestionOption option3 = new QuestionOption("HashMap允许null键和null值", true, 3);
                option3.setQuestion(question);
                QuestionOption option4 = new QuestionOption("TreeMap基于红黑树实现，保持键的有序性", true, 4);
                option4.setQuestion(question);
                QuestionOption option5 = new QuestionOption("HashSet允许重复元素", false, 5);
                option5.setQuestion(question);
                question.addOption(option1);
                question.addOption(option2);
                question.addOption(option3);
                question.addOption(option4);
                question.addOption(option5);
            } else if (question.getTitle().contains("数据库事务ACID特性")) {
                // 第20题 - 数据库事务ACID特性单选题
                QuestionOption option1 = new QuestionOption("原子性、一致性、隔离性、持久性", true, 1);
                option1.setQuestion(question);
                QuestionOption option2 = new QuestionOption("自动性、一致性、完整性、持久性", false, 2);
                option2.setQuestion(question);
                QuestionOption option3 = new QuestionOption("原子性、连续性、隔离性、数据性", false, 3);
                option3.setQuestion(question);
                QuestionOption option4 = new QuestionOption("自动性、连续性、完整性、数据性", false, 4);
                option4.setQuestion(question);
                question.addOption(option1);
                question.addOption(option2);
                question.addOption(option3);
                question.addOption(option4);
            } else if (question.getTitle().contains("IP地址分类")) {
                // 第22题 - IP地址分类单选题
                QuestionOption option1 = new QuestionOption("A类", false, 1);
                option1.setQuestion(question);
                QuestionOption option2 = new QuestionOption("B类", false, 2);
                option2.setQuestion(question);
                QuestionOption option3 = new QuestionOption("C类", true, 3);
                option3.setQuestion(question);
                QuestionOption option4 = new QuestionOption("D类", false, 4);
                option4.setQuestion(question);
                question.addOption(option1);
                question.addOption(option2);
                question.addOption(option3);
                question.addOption(option4);
            } else if (question.getTitle().contains("网络安全威胁")) {
                // 第25题 - 网络安全威胁多选题
                QuestionOption option1 = new QuestionOption("DDoS攻击", true, 1);
                option1.setQuestion(question);
                QuestionOption option2 = new QuestionOption("SQL注入", true, 2);
                option2.setQuestion(question);
                QuestionOption option3 = new QuestionOption("跨站脚本攻击(XSS)", true, 3);
                option3.setQuestion(question);
                QuestionOption option4 = new QuestionOption("中间人攻击", true, 4);
                option4.setQuestion(question);
                QuestionOption option5 = new QuestionOption("正常的HTTP请求", false, 5);
                option5.setQuestion(question);
                question.addOption(option1);
                question.addOption(option2);
                question.addOption(option3);
                question.addOption(option4);
                question.addOption(option5);
            } else if (question.getQuestionType() == QuestionType.TRUE_FALSE) {
                // 判断题选项
                if (question.getTitle().contains("进程与线程关系") || 
                    question.getTitle().contains("网络协议层次")) {
                    // 正确答案
                    QuestionOption option1 = new QuestionOption("正确", true, 1);
                    option1.setQuestion(question);
                    QuestionOption option2 = new QuestionOption("错误", false, 2);
                    option2.setQuestion(question);
                    question.addOption(option1);
                    question.addOption(option2);
                } else {
                    // 错误答案（银行家算法不能完全避免死锁）
                    QuestionOption option1 = new QuestionOption("正确", false, 1);
                    option1.setQuestion(question);
                    QuestionOption option2 = new QuestionOption("错误", true, 2);
                    option2.setQuestion(question);
                    question.addOption(option1);
                    question.addOption(option2);
                }
            }
        }
        questionRepository.saveAll(choiceQuestions);
    }
    
    /**
     * 创建题目的辅助方法
     */
    private Question createQuestion(Exam exam, QuestionBank questionBank, String title, String content, 
                                  QuestionType type, BigDecimal maxScore) {
        Question question = new Question();
        question.setExam(exam);
        question.setQuestionBank(questionBank);
        question.setTitle(title);
        question.setContent(content);
        question.setQuestionType(type);
        question.setMaxScore(maxScore);
        return question;
    }
    
    /**
     * 根据标题查找考试
     */
    private Exam findExamByTitle(List<Exam> exams, String title) {
        return exams.stream()
            .filter(exam -> exam.getTitle().equals(title))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 根据学科查找题目库
     */
    private QuestionBank findQuestionBankBySubject(List<QuestionBank> questionBanks, String subject) {
        return questionBanks.stream()
            .filter(bank -> bank.getSubject().equals(subject))
            .findFirst()
            .orElse(null);
    }
}
