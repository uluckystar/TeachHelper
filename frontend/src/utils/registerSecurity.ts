// 注册安全配置
export const REGISTER_CONFIG = {
  // 允许的角色
  ALLOWED_ROLES: ['STUDENT', 'TEACHER'] as const,
  
  // 密码强度要求
  PASSWORD_REQUIREMENTS: {
    minLength: 8,
    requireLetters: true,
    requireNumbers: true,
    forbiddenPasswords: ['password', '123456', '12345678', 'qwerty', 'abc123']
  },
  
  // 用户名规则
  USERNAME_RULES: {
    // 已弃用：旧的正则表达式模式，由新的验证逻辑替代
    pattern: /^[\u4e00-\u9fff\u3400-\u4dbfa-zA-Z0-9_]{2,50}$/,
    reservedWords: ['admin', 'root', 'system', 'administrator', 'teacher', 'student', '管理员', '教师', '学生', '系统']
  },
  
  // 角色权限配置
  ROLE_PERMISSIONS: {
    STUDENT: {
      canCreateAdmin: false,
      canCreateTeacher: false,
      canCreateStudent: false,
      description: '学生用户，只能参与考试和查看自己的成绩'
    },
    TEACHER: {
      canCreateAdmin: false,
      canCreateTeacher: false,
      canCreateStudent: false,
      description: '教师用户，可以创建和管理考试、题目和评估'
    },
    ADMIN: {
      canCreateAdmin: true,
      canCreateTeacher: true,
      canCreateStudent: true,
      description: '管理员用户，拥有系统的完全管理权限'
    }
  }
}

// 角色类型定义
export type UserRole = typeof REGISTER_CONFIG.ALLOWED_ROLES[number] | 'ADMIN'

// 注册验证函数
export const validateRegistration = {
  // 验证用户名
  username: (username: string): string | null => {
    if (!username) {
      return '用户名不能为空'
    }
    
    // 检查字符是否合法（中文、英文、数字、下划线）
    const validCharPattern = /^[\u4e00-\u9fff\u3400-\u4dbfa-zA-Z0-9_]+$/
    if (!validCharPattern.test(username)) {
      return '用户名只能包含中文、英文、数字和下划线'
    }
    
    // 检查长度：中文字符2-50个，英文字符3-50个
    const chineseCharCount = (username.match(/[\u4e00-\u9fff\u3400-\u4dbf]/g) || []).length
    const totalLength = username.length
    
    if (totalLength > 50) {
      return '用户名长度不能超过50个字符'
    }
    
    // 如果包含中文字符，最少2个字符
    if (chineseCharCount > 0) {
      if (totalLength < 2) {
        return '包含中文的用户名至少需要2个字符'
      }
    } else {
      // 纯英文数字组合，最少3个字符
      if (totalLength < 3) {
        return '英文用户名至少需要3个字符'
      }
    }
    
    // 检查保留词（支持中英文，大小写不敏感）
    const lowerUsername = username.toLowerCase()
    for (const reserved of REGISTER_CONFIG.USERNAME_RULES.reservedWords) {
      if (lowerUsername === reserved.toLowerCase() || username === reserved) {
        return '用户名不能使用保留词'
      }
    }
    
    return null
  },
  
  // 验证密码
  password: (password: string): string | null => {
    if (!password) {
      return '密码不能为空'
    }
    
    if (password.length < REGISTER_CONFIG.PASSWORD_REQUIREMENTS.minLength) {
      return `密码长度至少${REGISTER_CONFIG.PASSWORD_REQUIREMENTS.minLength}个字符`
    }
    
    if (REGISTER_CONFIG.PASSWORD_REQUIREMENTS.requireLetters && !/[a-zA-Z]/.test(password)) {
      return '密码必须包含字母'
    }
    
    if (REGISTER_CONFIG.PASSWORD_REQUIREMENTS.requireNumbers && !/[0-9]/.test(password)) {
      return '密码必须包含数字'
    }
    
    for (const forbidden of REGISTER_CONFIG.PASSWORD_REQUIREMENTS.forbiddenPasswords) {
      if (password.toLowerCase().includes(forbidden)) {
        return '密码过于简单，请使用更强的密码'
      }
    }
    
    return null
  },
  
  // 验证角色
  roles: (roles: string[]): string | null => {
    if (!roles || roles.length === 0) {
      return '请选择角色'
    }
    
    if (roles.length > 1) {
      return '一个用户只能有一个角色'
    }
    
    const role = roles[0]
    if (!REGISTER_CONFIG.ALLOWED_ROLES.includes(role as any)) {
      return '只允许注册学生或教师角色'
    }
    
    return null
  },
  
  // 验证邮箱
  email: (email: string): string | null => {
    if (!email) {
      return '邮箱不能为空'
    }
    
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailPattern.test(email)) {
      return '邮箱格式不正确'
    }
    
    return null
  }
}

// 安全检查函数
export const securityChecks = {
  // 检查是否可以注册指定角色
  canRegisterRole: (currentUserRole: UserRole | null, targetRole: string): boolean => {
    // 公开注册只允许学生和教师
    if (!currentUserRole) {
      return REGISTER_CONFIG.ALLOWED_ROLES.includes(targetRole as any)
    }
    
    // 根据当前用户角色检查权限
    const permissions = REGISTER_CONFIG.ROLE_PERMISSIONS[currentUserRole]
    if (!permissions) {
      return false
    }
    
    switch (targetRole) {
      case 'ADMIN':
        return permissions.canCreateAdmin
      case 'TEACHER':
        return permissions.canCreateTeacher
      case 'STUDENT':
        return permissions.canCreateStudent
      default:
        return false
    }
  },
  
  // 获取可注册的角色列表
  getAvailableRoles: (currentUserRole: UserRole | null): string[] => {
    if (!currentUserRole) {
      return [...REGISTER_CONFIG.ALLOWED_ROLES]
    }
    
    const roles: string[] = []
    
    if (securityChecks.canRegisterRole(currentUserRole, 'STUDENT')) {
      roles.push('STUDENT')
    }
    
    if (securityChecks.canRegisterRole(currentUserRole, 'TEACHER')) {
      roles.push('TEACHER')
    }
    
    if (securityChecks.canRegisterRole(currentUserRole, 'ADMIN')) {
      roles.push('ADMIN')
    }
    
    return roles
  }
}
