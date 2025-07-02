package com.teachhelper.service.answer;

import com.teachhelper.dto.response.ImportResult;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.Role;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.entity.User;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.StudentAnswerRepository;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.ai.AIEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Service
public class MajorAssignmentAnswerImportService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private StudentAnswerRepository studentAnswerRepository;
    @Autowired
    private AIEvaluationService aiEvaluationService;
    @Value("${app.upload.dir}")
    private String uploadDir;

    public ImportResult importMajorAssignmentAnswers(String subject, String assignment, Long questionId, Long userId) throws IOException {
        ImportResult result = new ImportResult();
        result.setSuccessfulStudents(new ArrayList<>());
        result.setFailedStudents(new ArrayList<>());
        result.setErrorMessages(new ArrayList<>());
        File baseDir = new File(uploadDir + File.separator + "answer" + File.separator + subject);
        File target = new File(baseDir, assignment);
        if (!target.exists()) {
            throw new IllegalArgumentException("未找到作业文件或目录: " + target.getAbsolutePath());
        }
        if (target.isDirectory()) {
            // 目录批量导入
            List<File> answerDocs = findAllAnswerDocuments(target);
            doBatchImport(answerDocs, questionId, userId, result);
        } else if (target.getName().toLowerCase().endsWith(".zip")) {
            // zip批量导入
            File tempDir = createTempDirectory("major_assignment_" + System.currentTimeMillis());
            try (ZipFile zipFile = new ZipFile(target, StandardCharsets.UTF_8)) {
                extractZipFile(zipFile, tempDir);
                List<File> answerDocs = findAllAnswerDocuments(tempDir);
                doBatchImport(answerDocs, questionId, userId, result);
            } finally {
                deleteDirectory(tempDir);
            }
        } else if (isAnswerDocument(target.getName())) {
            // 单文档导入
            List<File> answerDocs = List.of(target);
            doBatchImport(answerDocs, questionId, userId, result);
        } else {
            throw new IllegalArgumentException("不支持的作业文件类型: " + target.getName());
        }
        return result;
    }

    private File createTempDirectory(String prefix) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), prefix);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new IOException("无法创建临时目录: " + tempDir.getAbsolutePath());
        }
        return tempDir;
    }

    private void extractZipFile(ZipFile zipFile, File targetDir) throws IOException {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryFile = new File(targetDir, entry.getName());
            if (!entryFile.toPath().normalize().startsWith(targetDir.toPath().normalize())) {
                throw new IOException("Entry is outside the target dir: " + entry.getName());
            }
            if (entry.isDirectory()) {
                entryFile.mkdirs();
            } else {
                entryFile.getParentFile().mkdirs();
                try (InputStream inputStream = zipFile.getInputStream(entry);
                     FileOutputStream outputStream = new FileOutputStream(entryFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    private List<File> findAllAnswerDocuments(File dir) {
        List<File> docs = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    docs.addAll(findAllAnswerDocuments(file));
                } else if (isAnswerDocument(file.getName())) {
                    docs.add(file);
                }
            }
        }
        return docs;
    }

    private boolean isAnswerDocument(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".wps");
    }

    private void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) return;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    private User findOrCreateStudent(StudentInfo info) {
        Optional<User> userOpt = userRepository.findByStudentNumber(info.studentNumber);
        if (userOpt.isPresent()) return userOpt.get();
        // 无学号时用姓名唯一
        if ("No_student_number".equals(info.studentNumber)) {
            List<User> byNameList = userRepository.findByRealNameContaining(info.name);
            for (User u : byNameList) {
                if (u.getRealName().equals(info.name)) return u;
            }
        }
        User user = new User();
        user.setStudentNumber(info.studentNumber);
        user.setRealName(info.name);
        user.setUsername(info.studentNumber + "_" + info.name);
        user.setPassword("123456");
        user.setEmail(info.studentNumber + "@student.edu");
        user.setRoles(Set.of(Role.STUDENT));
        try {
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("创建学生失败", e);
            return null;
        }
    }

    private void createOrUpdateStudentAnswer(User student, Question question, String answerContent) {
        try {
            StudentAnswer existing = studentAnswerRepository.findByStudentIdAndQuestionId(student.getId(), question.getId());
            if (existing != null) {
                existing.setAnswerText(answerContent);
                existing.setCreatedAt(LocalDateTime.now());
                studentAnswerRepository.save(existing);
            } else {
                StudentAnswer ans = new StudentAnswer();
                ans.setStudent(student);
                ans.setQuestion(question);
                ans.setAnswerText(answerContent);
                ans.setEvaluated(false);
                ans.setCreatedAt(LocalDateTime.now());
                studentAnswerRepository.save(ans);
            }
        } catch (Exception e) {
            log.error("创建或更新学生答案失败", e);
        }
    }

    public static class StudentInfo {
        public String studentNumber;
        public String name;
        public StudentInfo(String studentNumber, String name) {
            this.studentNumber = studentNumber;
            this.name = name;
        }
    }

    /**
     * 获取指定科目下的大作业文档、zip和子目录列表
     */
    public List<String> getAvailableAssignments(String subject) {
        String basePath = uploadDir + File.separator + "answer";
        Path subjectDir = Paths.get(basePath, subject);
        if (!Files.exists(subjectDir)) {
            log.warn("大作业科目目录不存在: {}", subjectDir);
            return Collections.emptyList();
        }
        try {
            return Files.list(subjectDir)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase().endsWith(".zip") || isAnswerDocument(name) || Files.isDirectory(subjectDir.resolve(name)))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("读取大作业作业目录失败: subject={}", subject, e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量导入答案文档
     */
    private void doBatchImport(List<File> answerDocs, Long questionId, Long userId, ImportResult result) {
        if (answerDocs.isEmpty()) {
            result.getErrorMessages().add("未找到任何答案文档");
            return;
        }
        List<String> fileNames = answerDocs.stream().map(File::getName).collect(Collectors.toList());
        List<StudentInfo> studentInfos = StudentInfoLLMExtractor.batchExtract(fileNames, aiEvaluationService, userId);
        Map<String, StudentInfo> fileToStudent = new HashMap<>();
        for (int i = 0; i < fileNames.size(); i++) {
            fileToStudent.put(fileNames.get(i), studentInfos.get(i));
        }
        Map<String, Integer> nameCount = new HashMap<>();
        for (StudentInfo info : studentInfos) {
            if ("No_student_number".equals(info.studentNumber)) {
                String key = info.name;
                nameCount.put(key, nameCount.getOrDefault(key, 0) + 1);
                if (nameCount.get(key) > 1) {
                    info.name = info.name + "_" + nameCount.get(key);
                }
            }
        }
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new IllegalArgumentException("题目不存在: " + questionId));
        int success = 0, fail = 0;
        for (File doc : answerDocs) {
            StudentInfo info = fileToStudent.get(doc.getName());
            if (info == null) {
                result.getFailedStudents().add(doc.getName());
                result.getErrorMessages().add("LLM未能解析: " + doc.getName());
                fail++;
                continue;
            }
            User student = findOrCreateStudent(info);
            if (student == null) {
                result.getFailedStudents().add(info.name);
                result.getErrorMessages().add("无法创建学生: " + info.name);
                fail++;
                continue;
            }
            String answerContent = "【文件名】: " + doc.getName();
            createOrUpdateStudentAnswer(student, question, answerContent);
            result.getSuccessfulStudents().add(info.name);
            success++;
        }
        result.setTotalFiles(answerDocs.size());
        result.setSuccessCount(success);
        result.setFailedCount(fail);
        result.setSkippedCount(0);
    }
} 