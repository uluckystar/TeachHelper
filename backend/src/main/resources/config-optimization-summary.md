# Spring Boot 配置文件优化总结

## 优化前的问题

1. **重复配置严重**：JWT、日志、监控等配置在多个文件中重复
2. **结构不清晰**：各环境配置混乱，缺少明确的分层
3. **AI配置不统一**：不同环境的AI配置方式不一致
4. **维护困难**：修改一个配置需要同时修改多个文件

## 优化后的结构

### 1. 配置分层架构

```
application.yml          (基础配置层)
├── 通用应用配置：JWT、文件上传、监控等
├── 默认AI配置参数
└── API文档配置

application-dev.yml      (开发环境层)
├── MySQL主数据库
├── DeepSeek Chat + Ollama Embedding
├── 详细日志配置
└── 开发专用设置

application-prod.yml     (生产环境层)
├── PostgreSQL数据库
├── OpenAI全套服务
├── 生产级日志和监控
└── 环境变量支持

application-test.yml     (测试环境层)
├── H2内存数据库
├── AI服务Mock
├── 测试专用配置
└── 简化的安全设置
```

### 2. 关键优化点

#### A. 配置继承和覆盖
- **基础配置**在`application.yml`中定义
- **环境特定配置**在对应的profile文件中覆盖
- **环境变量支持**：生产环境通过环境变量动态配置

#### B. AI配置统一化
- **开发环境**：混合配置（DeepSeek Chat + Ollama Embedding）
- **生产环境**：OpenAI全套服务
- **测试环境**：Mock服务

#### C. 数据库配置优化
- **开发**：MySQL（便于调试）
- **生产**：PostgreSQL（性能和扩展性）
- **测试**：H2内存（快速重置）

#### D. 日志配置分级
- **开发**：详细DEBUG日志，便于调试
- **生产**：INFO级别，结构化日志，支持链路追踪
- **测试**：DEBUG日志，关注测试相关信息

### 3. 环境变量配置

#### 开发环境变量（可选）
```bash
export DEEPSEEK_API_KEY=your_deepseek_key
export OLLAMA_BASE_URL=http://localhost:11434
export DB_USERNAME=root
export DB_PASSWORD=12345
```

#### 生产环境变量（必需）
```bash
export OPENAI_API_KEY=your_openai_key
export DB_HOST=prod-db-host
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
export LOG_LEVEL=INFO
export AI_CONCURRENT_REQUESTS=20
export AI_RATE_LIMIT=200
```

### 4. 使用方式

#### 启动不同环境
```bash
# 开发环境（默认）
java -jar app.jar

# 明确指定开发环境
java -jar app.jar --spring.profiles.active=dev

# 生产环境
java -jar app.jar --spring.profiles.active=prod

# 测试环境
java -jar app.jar --spring.profiles.active=test
```

#### IDE中的配置
在IDE中设置Active profiles：
- IDEA：Run Configuration -> Active profiles: `dev`
- VS Code：launch.json中添加`"args": ["--spring.profiles.active=dev"]`

### 5. 主要优势

1. **清晰的职责分离**：每个配置文件职责明确
2. **减少重复**：公共配置统一管理
3. **环境灵活性**：通过环境变量轻松调整
4. **维护便利**：修改公共配置只需要一个地方
5. **部署友好**：生产环境完全通过环境变量配置

### 6. 注意事项

1. **敏感信息**：不要在配置文件中硬编码敏感信息
2. **环境变量**：生产环境务必通过环境变量配置
3. **数据库迁移**：生产环境使用`validate`，开发环境使用`update`
4. **AI服务**：确保对应的AI服务可用
5. **日志路径**：确保应用有权限写入日志目录
