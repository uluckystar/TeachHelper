package com.teachhelper.service.answer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LibreOfficeConverterService {

    private static final String[] LIBREOFFICE_PATHS = {
        "/Applications/LibreOffice.app/Contents/MacOS/soffice", // macOS
        "/usr/bin/libreoffice", // Linux
        "/usr/bin/soffice", // Linux alternative
        "C:\\Program Files\\LibreOffice\\program\\soffice.exe", // Windows
        "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe" // Windows 32-bit
    };

    private String libreOfficePath;

    /**
     * 检测系统中的LibreOffice安装路径
     */
    private String detectLibreOfficePath() {
        if (libreOfficePath != null) {
            return libreOfficePath;
        }

        for (String path : LIBREOFFICE_PATHS) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                libreOfficePath = path;
                log.info("检测到LibreOffice路径: {}", path);
                return path;
            }
        }

        log.warn("未找到LibreOffice安装，请确保已安装LibreOffice");
        return null;
    }

    /**
     * 将doc文件转换为docx格式
     */
    public File convertDocToDocx(File sourceFile) throws IOException {
        String executablePath = detectLibreOfficePath();
        if (executablePath == null) {
            throw new IOException("未找到LibreOffice可执行文件");
        }

        // 在源文件目录下创建converted子目录
        File sourceDir = sourceFile.getParentFile();
        File targetDir = new File(sourceDir, "converted");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        try {
            // 构建转换命令
            List<String> command = new ArrayList<>();
            command.add(executablePath);
            command.add("--headless");
            command.add("--convert-to");
            command.add("docx");
            command.add(sourceFile.getAbsolutePath());
            command.add("--outdir");
            command.add(targetDir.getAbsolutePath());

            log.info("执行LibreOffice转换命令: {}", String.join(" ", command));

            // 执行转换
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 等待转换完成，最多等待30秒
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("LibreOffice转换超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("LibreOffice转换失败，退出码: " + exitCode);
            }

            // 查找转换后的文件
            String originalName = sourceFile.getName();
            String nameWithoutExt = originalName.replaceAll("\\.(doc|DOC)$", "");
            File convertedFile = new File(targetDir, nameWithoutExt + ".docx");

            if (!convertedFile.exists()) {
                throw new IOException("转换后的文件不存在: " + convertedFile.getAbsolutePath());
            }

            log.info("成功转换文档: {} -> {}", sourceFile.getName(), convertedFile.getName());
            return convertedFile;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("LibreOffice转换被中断", e);
        }
        // 注意：不再清理目录，因为现在文件保存在源目录下的converted文件夹中
    }

    /**
     * 将doc文件转换为HTML格式
     */
    public File convertDocToHtml(File sourceFile) throws IOException {
        String executablePath = detectLibreOfficePath();
        if (executablePath == null) {
            throw new IOException("未找到LibreOffice可执行文件");
        }

        // 在源文件目录下创建converted子目录
        File sourceDir = sourceFile.getParentFile();
        File targetDir = new File(sourceDir, "converted");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        try {
            // 构建转换命令
            List<String> command = new ArrayList<>();
            command.add(executablePath);
            command.add("--headless");
            command.add("--convert-to");
            command.add("html");
            command.add(sourceFile.getAbsolutePath());
            command.add("--outdir");
            command.add(targetDir.getAbsolutePath());

            log.info("执行LibreOffice HTML转换命令: {}", String.join(" ", command));

            // 执行转换
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 等待转换完成
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("LibreOffice HTML转换超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("LibreOffice HTML转换失败，退出码: " + exitCode);
            }

            // 查找转换后的文件
            String originalName = sourceFile.getName();
            String nameWithoutExt = originalName.replaceAll("\\.(doc|DOC)$", "");
            File convertedFile = new File(targetDir, nameWithoutExt + ".html");

            if (!convertedFile.exists()) {
                throw new IOException("转换后的HTML文件不存在: " + convertedFile.getAbsolutePath());
            }

            log.info("成功转换文档为HTML: {} -> {}", sourceFile.getName(), convertedFile.getName());
            return convertedFile;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("LibreOffice HTML转换被中断", e);
        }
        // 注意：不再清理目录，因为现在文件保存在源目录下的converted文件夹中
    }

    /**
     * 检查LibreOffice是否可用
     */
    public boolean isLibreOfficeAvailable() {
        return detectLibreOfficePath() != null;
    }

    /**
     * 注册清理钩子，在JVM关闭时清理临时文件
     */
    private void registerCleanupHook(File tempDir) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                deleteDirectory(tempDir);
                log.debug("清理临时目录: {}", tempDir.getAbsolutePath());
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}", tempDir.getAbsolutePath(), e);
            }
        }));
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
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
    }
} 