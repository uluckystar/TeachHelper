# OCR功能使用说明

## 🎯 功能概述

TeachHelper系统已集成OCR（光学字符识别）功能，能够自动识别图片文件中的文字内容，特别适用于学生提交的手写作业拍照等场景。

## ✨ 主要特性

### 📷 图片格式支持
- `.jpg` / `.jpeg`
- `.png` 
- `.gif`
- `.bmp`
- `.tiff` / `.tif`
- `.webp`

### 🌐 语言支持
- **中文简体** (`chi_sim`)
- **英文** (`eng`)
- 自动识别中英文混合内容

### 🔧 技术特性
- 基于Tesseract 5.5.0引擎
- 智能文件格式检测
- 文件大小限制：10MB
- 自动错误处理和降级
- 详细的处理日志

## 🚀 安装和配置

### 1. 安装Tesseract (macOS)
```bash
# 安装Tesseract核心和语言包
brew install tesseract tesseract-lang

# 验证安装
tesseract --version
tesseract --list-langs
```

### 2. 启动服务
```bash
# 方式1: 使用提供的启动脚本（推荐）
cd backend
./start-with-ocr.sh

# 方式2: 手动启动（需要设置库路径）
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g -Djna.library.path=/opt/homebrew/Cellar/tesseract/5.5.0_1/lib"
```

### 3. 配置参数 (application.yml)
```yaml
app:
  ocr:
    enabled: true  # 启用/禁用OCR功能
    tesseract:
      datapath: /opt/homebrew/share/tessdata  # 训练数据路径
```

## 📋 使用流程

### 嵌套压缩包答案导入

1. **上传文件结构**：
   ```
   uploads/answer/科目名称/作业名称/
   ├── 班级压缩包.zip
   │   ├── 学生1-姓名.zip
   │   │   └── 答案图片.jpg  ← OCR自动识别
   │   └── 学生2-姓名.zip
   │       └── 答案文档.docx  ← 文档解析
   └── 其他班级压缩包/
   ```

2. **自动处理流程**：
   - 系统检测文件格式
   - 图片文件 → **OCR文字识别**
   - 文档文件 → 传统文档解析
   - 生成统一的答案记录

3. **结果展示**：
   ```
   【文件名】: 学生答案.jpg
   【文件格式】: JPG
   【文件大小】: 2.3 KB
   【处理方式】: 图片+OCR文字识别
   
   【答案内容】:
   这是OCR识别出的文字内容...
   ```

## 🔍 监控和调试

### 查看OCR状态
```bash
# 检查OCR服务状态
curl http://localhost:8080/api/debug/ocr-status

# 查看系统整体状态
curl http://localhost:8080/api/debug/status
```

### 日志分析
查看应用日志中的OCR相关信息：
```
🔍 开始OCR文字识别: example.jpg
✅ OCR识别成功: example.jpg - 耗时: 1500ms, 识别内容长度: 150 字符
```

## ⚠️ 常见问题

### 1. "Unable to load library 'tesseract'" 错误
**原因**: Java找不到Tesseract动态库
**解决方案**: 
- 使用提供的 `start-with-ocr.sh` 启动脚本
- 或手动设置JVM参数: `-Djna.library.path=/opt/homebrew/Cellar/tesseract/5.5.0_1/lib`

### 2. OCR识别结果为空
**可能原因**:
- 图片质量不佳（分辨率低、模糊）
- 图片中没有文字内容
- 字体过于特殊或手写不清晰

**改进建议**:
- 提高图片拍摄质量
- 确保光线充足、对比度清晰
- 避免图片倾斜或扭曲

### 3. 中文识别效果不佳
**检查语言包**:
```bash
# 确认中文语言包已安装
ls /opt/homebrew/share/tessdata/chi*
# 应该看到: chi_sim.traineddata
```

### 4. 性能优化
- **文件大小限制**: 单个图片限制10MB
- **并发处理**: 系统支持多个OCR任务并发执行
- **内存设置**: 建议JVM内存至少2GB (`-Xmx2g`)

## 🎯 最佳实践

### 学生提交建议
1. **图片质量**: 
   - 分辨率 ≥ 300 DPI
   - 清晰对焦，避免模糊
   - 充足光线，避免阴影

2. **拍摄角度**:
   - 正对文档，避免倾斜
   - 包含完整内容
   - 避免手指遮挡

3. **文字要求**:
   - 字迹清晰工整
   - 避免过于花哨的字体
   - 保持适当的字间距

### 教师使用建议
1. **批量处理**: 使用嵌套压缩包功能批量导入
2. **结果验证**: 检查OCR识别结果的准确性
3. **备选方案**: 对于OCR失败的文件，可要求学生重新提交

## 📈 功能扩展

### 未来可能的改进
- [ ] 支持更多图片格式
- [ ] 添加图片预处理（去噪、增强）
- [ ] 集成云端OCR服务作为备选
- [ ] 支持表格和公式识别
- [ ] 添加OCR结果置信度评分

---

**注意**: OCR技术有其局限性，建议将其作为辅助工具使用，重要内容仍需人工核验。 