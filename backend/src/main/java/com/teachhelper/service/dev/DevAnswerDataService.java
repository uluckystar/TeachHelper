package com.teachhelper.service.dev;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.entity.Student;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.StudentRepository;
import com.teachhelper.repository.StudentAnswerRepository;

/**
 * 开发环境学生答案数据服务
 * 负责创建学生答案的示例数据
 */
@Service
public class DevAnswerDataService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private StudentAnswerRepository studentAnswerRepository;
    
    /**
     * 创建学生答案数据
     */
    @Transactional
    public void createStudentAnswers() {
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            throw new RuntimeException("请先创建学生数据");
        }
        
        List<Question> questions = questionRepository.findAll();
        if (questions.isEmpty()) {
            throw new RuntimeException("请先创建题目数据");
        }
        
        createAnswersForAllStudents(students, questions);
    }
    
    /**
     * 清空学生答案数据
     */
    @Transactional
    public void clearAnswerData() {
        studentAnswerRepository.deleteAll();
    }
    
    /**
     * 为所有学生创建答案
     */
    private void createAnswersForAllStudents(List<Student> students, List<Question> questions) {
        List<StudentAnswer> answers = new ArrayList<>();
        
        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            
            // 为每种类型的题目创建答案示例
            createEssayAnswers(student, questions, answers, i);
            createShortAnswers(student, questions, answers, i);
            createCodingAnswers(student, questions, answers, i);
            createCaseAnalysisAnswers(student, questions, answers, i);
            createChoiceAnswers(student, questions, answers, i);
            createTrueFalseAnswers(student, questions, answers, i);
            createCalculationAnswers(student, questions, answers, i);
        }
        
        studentAnswerRepository.saveAll(answers);
    }
    
    /**
     * 创建论述题答案
     */
    private void createEssayAnswers(Student student, List<Question> questions, 
                                  List<StudentAnswer> answers, int studentIndex) {
        List<Question> essayQuestions = questions.stream()
            .filter(q -> q.getQuestionType() == QuestionType.ESSAY)
            .toList();
        
        for (Question question : essayQuestions) {
            StudentAnswer answer = new StudentAnswer();
            answer.setQuestion(question);
            answer.setStudent(student);
            answer.setCreatedAt(LocalDateTime.now().minusHours(studentIndex + 1));
            
            if (question.getTitle().contains("面向对象编程概念")) {
                String[] essayAnswers = {
                    "面向对象编程有三大特性：1.封装：将数据和方法包装在类中，隐藏内部实现；2.继承：子类可以继承父类的属性和方法；3.多态：同一接口的不同实现。实际应用中，封装保证了代码的安全性，继承提高了代码复用性，多态增强了系统的扩展性。",
                    "面向对象编程的核心是类和对象。封装实现了信息隐藏，继承实现了代码复用，多态实现了接口统一。在Java中，封装通过private修饰符实现，继承通过extends关键字，多态通过接口和抽象类实现。",
                    "封装、继承、多态是面向对象的基础。封装让对象的状态只能通过定义的方法访问，继承让子类获得父类的特性，多态让不同对象对同一消息做出不同响应。",
                    "面向对象编程包括封装、继承、多态。封装保护数据安全，继承减少代码重复，多态提供灵活性。实际开发中广泛应用于设计模式、框架设计等。",
                    "OOP三大特性：封装隐藏实现细节、继承实现代码复用、多态支持动态绑定。在企业开发中，这些特性帮助构建可维护、可扩展的系统架构。"
                };
                answer.setAnswerText(essayAnswers[studentIndex % essayAnswers.length]);
            } else if (question.getTitle().contains("动态规划应用")) {
                String[] dpAnswers = {
                    "动态规划解决最长公共子序列：1.定义状态dp[i][j]表示X[0..i]和Y[0..j]的LCS长度；2.状态转移：如果X[i]==Y[j]，dp[i][j]=dp[i-1][j-1]+1，否则dp[i][j]=max(dp[i-1][j], dp[i][j-1])；3.构建DP表，从小规模到大规模计算。",
                    "LCS问题的动态规划思路：将大问题分解为子问题，找出状态转移关系，构建二维表格逐步求解。关键是确定边界条件和递推公式。",
                    "最长公共子序列使用动态规划：创建二维数组，比较字符是否相同决定状态转移，最终得到最优解。时间复杂度O(mn)，空间复杂度O(mn)。",
                    "动态规划求LCS：1.分析问题结构；2.确定状态表示；3.推导状态转移方程；4.实现代码求解。这是典型的二维DP问题。"
                };
                answer.setAnswerText(dpAnswers[studentIndex % dpAnswers.length]);
            } else if (question.getTitle().contains("TCP三次握手")) {
                String[] tcpAnswers = {
                    "TCP三次握手过程：1.客户端发送SYN报文请求连接；2.服务器回复SYN+ACK确认并请求连接；3.客户端发送ACK确认连接建立。这样确保了双方的发送和接收能力都正常。",
                    "三次握手建立可靠连接：第一次握手确认客户端发送能力，第二次握手确认服务器收发能力，第三次握手确认客户端接收能力，完成双向确认。",
                    "TCP连接建立需要三次握手来同步序列号和确认号，防止旧连接请求突然到达导致错误连接。三次握手保证了连接的可靠性。"
                };
                answer.setAnswerText(tcpAnswers[studentIndex % tcpAnswers.length]);
            }
            
            // 前6个学生的答案已评分
            if (studentIndex < 6) {
                answer.setScore(new BigDecimal(String.valueOf(18 + studentIndex * 2))); // 18-28分
                answer.setFeedback(generateEssayFeedback(answer.getScore()));
                answer.setEvaluated(true);
                answer.setEvaluatedAt(LocalDateTime.now().minusMinutes(30 + studentIndex * 5));
            } else {
                answer.setEvaluated(false);
            }
            
            answers.add(answer);
        }
    }
    
    /**
     * 创建简答题答案
     */
    private void createShortAnswers(Student student, List<Question> questions, 
                                   List<StudentAnswer> answers, int studentIndex) {
        List<Question> shortQuestions = questions.stream()
            .filter(q -> q.getQuestionType() == QuestionType.SHORT_ANSWER)
            .toList();
        
        for (Question question : shortQuestions) {
            StudentAnswer answer = new StudentAnswer();
            answer.setQuestion(question);
            answer.setStudent(student);
            answer.setCreatedAt(LocalDateTime.now().minusHours(studentIndex + 2));
            
            if (question.getTitle().contains("Java异常处理")) {
                String[] javaAnswers = {
                    "Java异常处理机制：try块包含可能抛出异常的代码，catch块处理异常，finally块无论是否有异常都会执行。自定义异常需要继承Exception类或其子类。",
                    "异常处理：try-catch捕获并处理异常，finally保证资源释放，throws声明方法可能抛出的异常。自定义异常类继承Exception。",
                    "Java通过try-catch-finally处理异常，可以捕获运行时错误并进行相应处理，保证程序稳定性。自定义异常便于业务逻辑处理。",
                    "异常机制包含异常类层次、异常抛出、异常捕获和处理。finally确保清理代码执行，自定义异常提供具体业务异常信息。"
                };
                answer.setAnswerText(javaAnswers[studentIndex % javaAnswers.length]);
            } else if (question.getTitle().contains("数据库设计原则")) {
                String[] dbAnswers = {
                    "数据库范式：第一范式要求每个属性不可再分；第二范式要求非主键属性完全依赖于主键；第三范式要求非主键属性不能传递依赖于主键。",
                    "1NF消除重复组，2NF消除部分函数依赖，3NF消除传递函数依赖。范式化减少数据冗余，提高数据一致性。",
                    "范式理论指导数据库设计：1NF确保原子性，2NF避免插入删除异常，3NF进一步减少冗余。但过度范式化可能影响查询性能。"
                };
                answer.setAnswerText(dbAnswers[studentIndex % dbAnswers.length]);
            } else if (question.getTitle().contains("CSS Flexbox布局")) {
                String[] cssAnswers = {
                    "Flexbox包含flex容器和flex项目。主要属性：justify-content控制主轴对齐，align-items控制交叉轴对齐，flex-direction设置主轴方向。",
                    "Flexbox布局模型：容器设置display:flex，项目自动成为flex项目。可以灵活控制项目的排列、对齐和空间分配。",
                    "弹性盒子布局：通过flex属性控制项目缩放，gap设置间距，flex-wrap控制换行。现代CSS布局的重要技术。"
                };
                answer.setAnswerText(cssAnswers[studentIndex % cssAnswers.length]);
            }
            
            // 前5个学生的答案已评分
            if (studentIndex < 5) {
                answer.setScore(new BigDecimal(String.valueOf(12 + studentIndex * 2))); // 12-20分
                answer.setFeedback(generateShortAnswerFeedback(answer.getScore()));
                answer.setEvaluated(true);
                answer.setEvaluatedAt(LocalDateTime.now().minusMinutes(45 + studentIndex * 3));
            } else {
                answer.setEvaluated(false);
            }
            
            answers.add(answer);
        }
    }
    
    /**
     * 创建编程题答案
     */
    private void createCodingAnswers(Student student, List<Question> questions, 
                                   List<StudentAnswer> answers, int studentIndex) {
        List<Question> codingQuestions = questions.stream()
            .filter(q -> q.getQuestionType() == QuestionType.CODING)
            .toList();
        
        for (Question question : codingQuestions) {
            StudentAnswer answer = new StudentAnswer();
            answer.setQuestion(question);
            answer.setStudent(student);
            answer.setCreatedAt(LocalDateTime.now().minusHours(studentIndex + 3));
            
            if (question.getTitle().contains("链表反转")) {
                String[] codingAnswers = {
                    "```java\npublic ListNode reverseList(ListNode head) {\n    ListNode prev = null;\n    ListNode current = head;\n    while (current != null) {\n        ListNode next = current.next;\n        current.next = prev;\n        prev = current;\n        current = next;\n    }\n    return prev;\n}\n```",
                    "```java\npublic ListNode reverse(ListNode head) {\n    if (head == null || head.next == null) return head;\n    ListNode p1 = null, p2 = head, p3;\n    while (p2 != null) {\n        p3 = p2.next;\n        p2.next = p1;\n        p1 = p2;\n        p2 = p3;\n    }\n    return p1;\n}\n```",
                    "```java\n// 迭代方式反转链表\nListNode prev = null;\nListNode curr = head;\nwhile (curr != null) {\n    ListNode temp = curr.next;\n    curr.next = prev;\n    prev = curr;\n    curr = temp;\n}\nreturn prev;\n```",
                    "使用三个指针prev、curr、next，遍历链表并逐步调整指针方向实现反转。时间复杂度O(n)，空间复杂度O(1)。"
                };
                answer.setAnswerText(codingAnswers[studentIndex % codingAnswers.length]);
            } else if (question.getTitle().contains("JavaScript闭包")) {
                String[] jsAnswers = {
                    "```javascript\nfunction createCounter() {\n    let count = 0;\n    return function() {\n        return ++count;\n    };\n}\nconst counter = createCounter();\nconsole.log(counter()); // 1\nconsole.log(counter()); // 2\n```",
                    "```javascript\nfunction counter() {\n    var num = 0;\n    return function() {\n        return ++num;\n    }\n}\nvar myCounter = counter();\n```",
                    "function makeCounter() { let i = 0; return () => ++i; } // 使用箭头函数的闭包实现"
                };
                answer.setAnswerText(jsAnswers[studentIndex % jsAnswers.length]);
            } else if (question.getTitle().contains("二叉树遍历")) {
                String[] treeAnswers = {
                    "```java\n// 前序遍历递归\nvoid preorder(TreeNode root) {\n    if (root == null) return;\n    visit(root);\n    preorder(root.left);\n    preorder(root.right);\n}\n// 中序遍历\nvoid inorder(TreeNode root) {\n    if (root == null) return;\n    inorder(root.left);\n    visit(root);\n    inorder(root.right);\n}\n```",
                    "递归遍历简单直观，非递归需要使用栈。前序：根-左-右，中序：左-根-右，后序：左-右-根。时间复杂度都是O(n)。",
                    "三种遍历方式的递归和迭代实现，关键在于访问节点的时机不同。递归版本简洁，迭代版本需要手动维护栈。"
                };
                answer.setAnswerText(treeAnswers[studentIndex % treeAnswers.length]);
            } else if (question.getTitle().contains("括号匹配")) {
                String[] stackAnswers = {
                    "```java\npublic boolean isValid(String s) {\n    Stack<Character> stack = new Stack<>();\n    for (char c : s.toCharArray()) {\n        if (c == '(' || c == '[' || c == '{') {\n            stack.push(c);\n        } else {\n            if (stack.isEmpty()) return false;\n            char top = stack.pop();\n            if (!isMatch(top, c)) return false;\n        }\n    }\n    return stack.isEmpty();\n}\n```",
                    "使用栈数据结构，遇到左括号入栈，遇到右括号检查栈顶是否匹配。最后检查栈是否为空。",
                    "栈的典型应用：左括号压栈，右括号弹栈并检查匹配。时间复杂度O(n)，空间复杂度O(n)。"
                };
                answer.setAnswerText(stackAnswers[studentIndex % stackAnswers.length]);
            }
            
            // 前4个学生的答案已评分
            if (studentIndex < 4) {
                answer.setScore(new BigDecimal(String.valueOf(20 + studentIndex * 3))); // 20-29分
                answer.setFeedback(generateCodingFeedback(answer.getScore()));
                answer.setEvaluated(true);
                answer.setEvaluatedAt(LocalDateTime.now().minusMinutes(60 + studentIndex * 10));
            } else {
                answer.setEvaluated(false);
            }
            
            answers.add(answer);
        }
    }
    
    /**
     * 创建案例分析题答案
     */
    private void createCaseAnalysisAnswers(Student student, List<Question> questions, 
                                         List<StudentAnswer> answers, int studentIndex) {
        List<Question> caseQuestions = questions.stream()
            .filter(q -> q.getQuestionType() == QuestionType.CASE_ANALYSIS)
            .toList();
        
        for (Question question : caseQuestions) {
            StudentAnswer answer = new StudentAnswer();
            answer.setQuestion(question);
            answer.setStudent(student);
            answer.setCreatedAt(LocalDateTime.now().minusHours(studentIndex + 4));
            
            if (question.getTitle().contains("SQL查询设计")) {
                String[] sqlAnswers = {
                    "数据库设计：\nCREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(50), class_id INT);\nCREATE TABLE courses (id INT PRIMARY KEY, name VARCHAR(50), credits INT);\nCREATE TABLE enrollments (student_id INT, course_id INT, score DECIMAL(5,2));\n\n查询语句：\nSELECT s.name, AVG(e.score) as avg_score\nFROM students s\nJOIN enrollments e ON s.id = e.student_id\nGROUP BY s.id, s.name\nORDER BY avg_score DESC;",
                    "表结构设计包含学生表、课程表、选课表。使用JOIN连接表，GROUP BY分组计算平均分，ORDER BY降序排列。",
                    "学生管理系统需要三个核心表，通过外键关联。计算平均成绩使用聚合函数AVG，按学生分组后排序输出。",
                    "CREATE TABLE设计表结构，SELECT语句使用JOIN和聚合函数实现复杂查询，体现了关系数据库的设计原则。"
                };
                answer.setAnswerText(sqlAnswers[studentIndex % sqlAnswers.length]);
            } else if (question.getTitle().contains("软件开发模型")) {
                String[] modelAnswers = {
                    "瀑布模型：线性顺序开发，文档驱动，适合需求明确的项目；螺旋模型：迭代开发，风险驱动，适合大型复杂项目；敏捷开发：快速迭代，客户参与，适合需求变化频繁的项目。",
                    "三种模型各有特点：瀑布模型严格按阶段进行，螺旋模型注重风险分析，敏捷模型强调快速响应变化。选择应根据项目特性决定。",
                    "软件开发模型比较：瀑布模型适合传统项目，螺旋模型适合高风险项目，敏捷模型适合创新项目。每种模型都有其适用场景。"
                };
                answer.setAnswerText(modelAnswers[studentIndex % modelAnswers.length]);
            }
            
            // 前5个学生的答案已评分
            if (studentIndex < 5) {
                answer.setScore(new BigDecimal(String.valueOf(18 + studentIndex * 2))); // 18-26分
                answer.setFeedback(generateCaseAnalysisFeedback(answer.getScore()));
                answer.setEvaluated(true);
                answer.setEvaluatedAt(LocalDateTime.now().minusMinutes(40 + studentIndex * 6));
            } else {
                answer.setEvaluated(false);
            }
            
            answers.add(answer);
        }
    }
    
    /**
     * 创建选择题答案
     */
    private void createChoiceAnswers(Student student, List<Question> questions, 
                                   List<StudentAnswer> answers, int studentIndex) {
        List<Question> choiceQuestions = questions.stream()
            .filter(q -> q.getQuestionType() == QuestionType.SINGLE_CHOICE || 
                        q.getQuestionType() == QuestionType.MULTIPLE_CHOICE)
            .toList();
        
        for (Question question : choiceQuestions) {
            StudentAnswer answer = new StudentAnswer();
            answer.setQuestion(question);
            answer.setStudent(student);
            answer.setCreatedAt(LocalDateTime.now().minusHours(studentIndex + 1));
            
            if (question.getQuestionType() == QuestionType.SINGLE_CHOICE) {
                // 单选题答案
                if (question.getTitle().contains("Java访问修饰符")) {
                    String[] choices = {"A", "B", "C", "D"};
                    answer.setAnswerText(choices[studentIndex % choices.length]);
                    // 正确答案是A(public)，前70%学生答对
                    boolean correct = (studentIndex % 10 < 7) ? "A".equals(answer.getAnswerText()) : 
                                    "A".equals(choices[(studentIndex + 1) % choices.length]);
                    answer.setScore(correct ? question.getMaxScore() : BigDecimal.ZERO);
                } else if (question.getTitle().contains("IP地址分类")) {
                    String[] choices = {"A", "B", "C", "D"};
                    answer.setAnswerText(choices[studentIndex % choices.length]);
                    // 正确答案是C
                    boolean correct = (studentIndex % 10 < 6) ? "C".equals(answer.getAnswerText()) : 
                                    "C".equals(choices[(studentIndex + 2) % choices.length]);
                    answer.setScore(correct ? question.getMaxScore() : BigDecimal.ZERO);
                } else if (question.getTitle().contains("数据库事务ACID")) {
                    String[] choices = {"A", "B", "C", "D"};
                    answer.setAnswerText(choices[studentIndex % choices.length]);
                    // 正确答案是A
                    boolean correct = (studentIndex % 10 < 8) ? "A".equals(answer.getAnswerText()) : 
                                    "A".equals(choices[(studentIndex + 1) % choices.length]);
                    answer.setScore(correct ? question.getMaxScore() : BigDecimal.ZERO);
                }
            } else {
                // 多选题答案
                if (question.getTitle().contains("Java集合特性")) {
                    String[] multiChoices = {"A,B,C,D", "A,B,C", "A,B,D", "B,C,D", "A,C,D"};
                    answer.setAnswerText(multiChoices[studentIndex % multiChoices.length]);
                    // 正确答案是A,B,C,D，部分正确给部分分
                    String correctAnswer = "A,B,C,D";
                    if (correctAnswer.equals(answer.getAnswerText())) {
                        answer.setScore(question.getMaxScore());
                    } else if (answer.getAnswerText().split(",").length >= 3) {
                        answer.setScore(question.getMaxScore().multiply(new BigDecimal("0.6")));
                    } else {
                        answer.setScore(question.getMaxScore().multiply(new BigDecimal("0.3")));
                    }
                } else if (question.getTitle().contains("网络安全威胁")) {
                    String[] securityChoices = {"A,B,C,D", "A,B,C", "A,C,D", "B,C,D", "A,B"};
                    answer.setAnswerText(securityChoices[studentIndex % securityChoices.length]);
                    // 正确答案是A,B,C,D
                    String correctAnswer = "A,B,C,D";
                    if (correctAnswer.equals(answer.getAnswerText())) {
                        answer.setScore(question.getMaxScore());
                    } else if (answer.getAnswerText().split(",").length >= 3) {
                        answer.setScore(question.getMaxScore().multiply(new BigDecimal("0.7")));
                    } else {
                        answer.setScore(question.getMaxScore().multiply(new BigDecimal("0.4")));
                    }
                }
            }
            
            // 选择题都自动评分
            answer.setEvaluated(true);
            answer.setEvaluatedAt(LocalDateTime.now().minusMinutes(10 + studentIndex));
            answer.setFeedback("系统自动评分");
            
            answers.add(answer);
        }
    }
    
    /**
     * 创建判断题答案
     */
    private void createTrueFalseAnswers(Student student, List<Question> questions, 
                                      List<StudentAnswer> answers, int studentIndex) {
        List<Question> tfQuestions = questions.stream()
            .filter(q -> q.getQuestionType() == QuestionType.TRUE_FALSE)
            .toList();
        
        for (Question question : tfQuestions) {
            StudentAnswer answer = new StudentAnswer();
            answer.setQuestion(question);
            answer.setStudent(student);
            answer.setCreatedAt(LocalDateTime.now().minusHours(studentIndex + 1));
            
            if (question.getTitle().contains("进程与线程关系") || 
                question.getTitle().contains("网络协议层次")) {
                // 正确答案是"正确"
                answer.setAnswerText(studentIndex % 3 == 0 ? "正确" : "错误");
                boolean correct = "正确".equals(answer.getAnswerText());
                answer.setScore(correct ? question.getMaxScore() : BigDecimal.ZERO);
            } else {
                // 银行家算法题，正确答案是"错误"
                answer.setAnswerText(studentIndex % 4 == 0 ? "错误" : "正确");
                boolean correct = "错误".equals(answer.getAnswerText());
                answer.setScore(correct ? question.getMaxScore() : BigDecimal.ZERO);
            }
            
            // 判断题都自动评分
            answer.setEvaluated(true);
            answer.setEvaluatedAt(LocalDateTime.now().minusMinutes(5 + studentIndex));
            answer.setFeedback("系统自动评分");
            
            answers.add(answer);
        }
    }
    
    /**
     * 创建计算题答案
     */
    private void createCalculationAnswers(Student student, List<Question> questions, 
                                        List<StudentAnswer> answers, int studentIndex) {
        List<Question> calcQuestions = questions.stream()
            .filter(q -> q.getQuestionType() == QuestionType.CALCULATION)
            .toList();
        
        for (Question question : calcQuestions) {
            StudentAnswer answer = new StudentAnswer();
            answer.setQuestion(question);
            answer.setStudent(student);
            answer.setCreatedAt(LocalDateTime.now().minusHours(studentIndex + 2));
            
            if (question.getTitle().contains("算法复杂度分析")) {
                String[] calcAnswers = {
                    "外层循环执行n次，内层循环第i次执行i次，总次数为1+2+3+...+n = n(n+1)/2，时间复杂度为O(n²)。",
                    "嵌套循环总执行次数：∑(i=1 to n) i = n(n+1)/2，所以时间复杂度是O(n²)。",
                    "分析：外层n次，内层1到n次，总计n(n+1)/2次操作，渐近复杂度O(n²)。",
                    "循环次数计算：1+2+...+n = n*(n+1)/2，时间复杂度为O(n^2)。"
                };
                answer.setAnswerText(calcAnswers[studentIndex % calcAnswers.length]);
            } else if (question.getTitle().contains("子网掩码计算")) {
                String[] subnetAnswers = {
                    "/24表示前24位为网络位，后8位为主机位。主机数 = 2^8 - 2 = 254台（减去网络地址和广播地址）。",
                    "24位网络位，8位主机位，可用主机数 = 2^8 - 2 = 254个。",
                    "子网掩码/24意味着255.255.255.0，主机位8位，可容纳254台主机。",
                    "网络192.168.1.0/24，主机位数=32-24=8位，主机数=2^8-2=254台。"
                };
                answer.setAnswerText(subnetAnswers[studentIndex % subnetAnswers.length]);
            }
            
            // 前6个学生的计算题已评分
            if (studentIndex < 6) {
                answer.setScore(new BigDecimal(String.valueOf(8 + studentIndex))); // 8-13分
                answer.setFeedback(generateCalculationFeedback(answer.getScore()));
                answer.setEvaluated(true);
                answer.setEvaluatedAt(LocalDateTime.now().minusMinutes(25 + studentIndex * 3));
            } else {
                answer.setEvaluated(false);
            }
            
            answers.add(answer);
        }
    }
    
    /**
     * 生成论述题反馈
     */
    private String generateEssayFeedback(BigDecimal score) {
        int scoreInt = score.intValue();
        if (scoreInt >= 25) {
            return "优秀：论述全面深入，逻辑清晰，实例恰当，体现了深厚的理论功底。";
        } else if (scoreInt >= 20) {
            return "良好：内容较为完整，逻辑较清晰，但在深度分析方面还可以进一步加强。";
        } else if (scoreInt >= 15) {
            return "中等：基本概念掌握正确，但论述不够深入，缺乏有效的实例支撑。";
        } else {
            return "需要改进：概念理解不够准确，论述逻辑性不强，建议加强相关理论学习。";
        }
    }
    
    /**
     * 生成简答题反馈
     */
    private String generateShortAnswerFeedback(BigDecimal score) {
        int scoreInt = score.intValue();
        if (scoreInt >= 16) {
            return "回答准确完整，要点把握到位。";
        } else if (scoreInt >= 12) {
            return "回答基本正确，但部分要点不够详细。";
        } else if (scoreInt >= 8) {
            return "回答有一定正确性，但缺少关键要点。";
        } else {
            return "回答不够准确，需要进一步学习相关知识。";
        }
    }
    
    /**
     * 生成编程题反馈
     */
    private String generateCodingFeedback(BigDecimal score) {
        int scoreInt = score.intValue();
        if (scoreInt >= 25) {
            return "代码实现正确，逻辑清晰，效率优秀，注释详细。";
        } else if (scoreInt >= 20) {
            return "算法思路正确，实现基本无误，但在代码优化方面有提升空间。";
        } else if (scoreInt >= 15) {
            return "基本思路正确，但代码实现存在一些问题，建议注意边界条件处理。";
        } else {
            return "算法理解不够准确，代码实现存在较多问题，需要加强编程练习。";
        }
    }
    
    /**
     * 生成案例分析题反馈
     */
    private String generateCaseAnalysisFeedback(BigDecimal score) {
        int scoreInt = score.intValue();
        if (scoreInt >= 22) {
            return "分析全面深入，方案设计合理，体现了良好的实践能力。";
        } else if (scoreInt >= 18) {
            return "分析较为全面，设计基本合理，但在某些细节方面还可以完善。";
        } else if (scoreInt >= 14) {
            return "分析有一定深度，但方案设计不够完整，需要加强实践经验。";
        } else {
            return "分析不够深入，方案设计存在问题，建议多参考实际案例。";
        }
    }
    
    /**
     * 生成计算题反馈
     */
    private String generateCalculationFeedback(BigDecimal score) {
        int scoreInt = score.intValue();
        if (scoreInt >= 10) {
            return "计算过程正确，步骤清晰，结果准确。";
        } else if (scoreInt >= 8) {
            return "计算思路正确，但在某个步骤存在小错误。";
        } else if (scoreInt >= 6) {
            return "基本思路正确，但计算过程存在错误。";
        } else {
            return "计算方法不正确，需要重新理解相关概念。";
        }
    }
}
