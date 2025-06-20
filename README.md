# TeachHelper - AI 智能考试评估系统

一个基于 Spring Boot 和 Vue 3 构建的智能考试评分系统，集成了 AI 能力进行自动评分和智能分析。

## 🚀 项目特性

- **AI 智能评分**: 基于 Spring AI 框架，支持多种 AI 模型进行智能评分
- **题目管理**: 完整的题目创建、编辑、分类管理功能
- **知识库系统**: 支持文档上传、知识点管理和智能检索
- **评分标准管理**: 灵活的评分标准定义和应用
- **批量评估**: 支持批量上传和评估学生答案
- **实时反馈**: 提供详细的评分反馈和改进建议
- **用户权限管理**: 支持教师、学生等多角色权限控制

## 🏗️ 技术架构

### 后端技术栈

- **框架**: Spring Boot 3.2.5
- **AI 集成**: Spring AI 1.0.0-SNAPSHOT
- **数据库**: MySQL + Spring Data JPA
- **安全**: Spring Security + JWT
- **文档**: SpringDoc OpenAPI 3
- **构建工具**: Maven

### 前端技术栈

- **框架**: Vue 3 + TypeScript
- **UI 组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **构建工具**: Vite
- **代码规范**: ESLint + Prettier

## 📁 项目结构

```text
TeachHelper/
├── backend/           # Spring Boot 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/         # Java 源代码
│   │   │   └── resources/    # 配置文件和资源
│   │   └── test/             # 测试代码
│   ├── pom.xml               # Maven 配置文件
│   └── .gitignore           # 后端 Git 忽略文件
└── frontend/          # Vue 3 前端项目
    ├── src/
    │   ├── components/       # Vue 组件
    │   ├── views/           # 页面组件
    │   ├── router/          # 路由配置
    │   ├── stores/          # Pinia 状态管理
    │   └── utils/           # 工具函数
    ├── package.json         # 前端依赖配置
    └── .gitignore          # 前端 Git 忽略文件
```

## 🚀 快速开始

### 环境要求

- **Java**: JDK 17+
- **Node.js**: 16+
- **MySQL**: 8.0+
- **Maven**: 3.6+

### 后端启动

1. 克隆项目并进入后端目录

```bash
git clone <repository-url>
cd TeachHelper/backend
```

2. 配置数据库
   - 创建 MySQL 数据库
   - 修改 `src/main/resources/application.yml` 中的数据库配置

3. 启动后端服务

```bash
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 前端启动

1. 进入前端目录并安装依赖

```bash
cd TeachHelper/frontend
npm install
```

2. 启动开发服务器

```bash
npm run dev
```

前端应用将在 `http://localhost:3000` 启动

## 📚 API 文档

启动后端服务后，可以通过以下地址访问 API 文档：

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## 🔧 开发指南

### 后端开发

- 遵循 RESTful API 设计原则
- 使用 Spring Boot 自动配置
- 实体类使用 JPA 注解
- 服务层使用 `@Service` 注解
- 控制器使用 `@RestController` 注解

### 前端开发

- 使用 Composition API
- 组件采用 `<script setup>` 语法
- 样式使用 scoped CSS
- 状态管理使用 Pinia
- 类型安全使用 TypeScript

## 🤝 贡献指南

本项目为私有项目，如需贡献或协作请联系项目维护者。

## 📞 联系我们

如有问题或建议，欢迎联系项目维护者：

- 项目负责人：[jason]