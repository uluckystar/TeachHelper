# TeachHelper Frontend

基于 Vue 3 + TypeScript + Element Plus 构建的现代化教学评估系统前端应用。

## 🚀 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - JavaScript 的超集，提供静态类型检查
- **Element Plus** - 基于 Vue 3 的企业级 UI 组件库
- **Vue Router** - Vue.js 官方路由管理器
- **Pinia** - Vue 的现代化状态管理库
- **Axios** - HTTP 客户端库
- **Vite** - 现代化前端构建工具

## 📁 项目结构

```
src/
├── api/              # API 服务层
│   ├── auth.ts       # 认证相关 API
│   ├── exam.ts       # 考试管理 API
│   ├── question.ts   # 题目管理 API
│   ├── answer.ts     # 答案提交 API
│   └── evaluation.ts # 评估相关 API
├── components/       # 公共组件
├── layouts/          # 布局组件
│   └── MainLayout.vue
├── router/           # 路由配置
│   └── index.ts
├── stores/           # Pinia 状态管理
│   └── auth.ts       # 认证状态管理
├── types/            # TypeScript 类型定义
│   └── api.ts
├── utils/            # 工具函数
│   └── request.ts    # HTTP 请求封装
├── views/            # 页面组件
│   ├── auth/         # 认证相关页面
│   ├── exam/         # 考试管理页面
│   ├── question/     # 题目管理页面
│   ├── student/      # 学生相关页面
│   └── evaluation/   # 评估相关页面
├── App.vue           # 根组件
└── main.ts           # 应用入口
```

## 🔧 开发环境设置

### 前置要求

- Node.js >= 16
- npm 或 yarn

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

应用将在 http://localhost:3000 启动

### 构建生产版本

```bash
npm run build
```

### 类型检查

```bash
npm run type-check
```

### 代码检查

```bash
npm run lint
```

## 🏗️ 功能模块

### 1. 认证系统
- 用户登录/注册
- JWT Token 管理
- 路由权限控制
- 角色管理（管理员、教师、学生）

### 2. 考试管理
- 考试创建/编辑/删除
- 考试状态管理（草稿、已发布、已结束）
- 考试列表查看
- 考试时间设置

### 3. 题目管理
- 题目创建/编辑/删除
- 多种题型支持（单选、多选、判断、主观题）
- 评分标准设置
- 题目选项配置

### 4. 学生考试
- 在线答题界面
- 自动保存答案
- 考试时间倒计时
- 批量提交答案

### 5. 智能评估
- AI 自动评分
- 手动评分
- 评分统计分析
- 成绩导出

## 🔌 API 集成

前端通过 Axios 与后端 Spring Boot API 进行通信：

- **认证 API**: `/api/auth/**`
- **考试 API**: `/api/exams/**`
- **题目 API**: `/api/questions/**`
- **答案 API**: `/api/student-answers/**`
- **评估 API**: `/api/evaluations/**`

### 请求拦截器功能
- 自动添加 JWT Token
- 统一错误处理
- 请求/响应日志记录

## 🛡️ 安全特性

- JWT Token 认证
- 路由级权限控制
- API 请求加密
- XSS 防护
- CSRF 保护

## 🎨 UI/UX 特性

- 响应式设计，支持多设备
- Element Plus 组件库
- 深色/浅色主题切换
- 国际化支持（中文）
- 加载状态和错误处理

## 📱 页面路由

- `/login` - 登录页面
- `/register` - 注册页面
- `/` - 仪表板首页
- `/exams` - 考试列表
- `/exams/create` - 创建考试
- `/exams/:id` - 考试详情
- `/exams/:id/edit` - 编辑考试
- `/exams/:id/questions` - 题目管理
- `/exams/:examId/take` - 参加考试
- `/exams/:examId/evaluation` - 考试评估
- `/profile` - 个人资料

## 🔄 状态管理

使用 Pinia 进行状态管理：

### Auth Store
```typescript
interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
}
```

主要方法：
- `login()` - 用户登录
- `register()` - 用户注册
- `logout()` - 用户登出
- `getCurrentUser()` - 获取当前用户信息

## 🚦 开发指南

### 添加新页面
1. 在 `src/views/` 下创建 Vue 组件
2. 在 `src/router/index.ts` 中添加路由配置
3. 设置适当的权限和元数据

### 添加新 API
1. 在 `src/types/api.ts` 中定义 TypeScript 类型
2. 在 `src/api/` 下创建对应的 API 服务
3. 在组件中导入并使用

### 样式规范
- 使用 Element Plus 设计系统
- 组件样式使用 scoped 作用域
- 响应式设计，适配移动端

## 🧪 测试

```bash
# 运行单元测试
npm run test

# 运行 E2E 测试
npm run test:e2e
```

## 📦 部署

### 开发环境
```bash
npm run dev
```

### 生产环境
```bash
npm run build
npm run preview
```

### Docker 部署
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "run", "preview"]
```

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/新功能`)
3. 提交更改 (`git commit -am '添加新功能'`)
4. 推送到分支 (`git push origin feature/新功能`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 🔗 相关链接

- [Vue 3 文档](https://v3.vuejs.org/)
- [Element Plus 文档](https://element-plus.org/)
- [TypeScript 文档](https://www.typescriptlang.org/)
- [Vite 文档](https://vitejs.dev/)
- [Pinia 文档](https://pinia.vuejs.org/)

## 📞 联系我们

如有问题或建议，请通过以下方式联系：

- 邮箱: support@teachhelper.com
- GitHub Issues: [项目问题](https://github.com/your-repo/issues)
