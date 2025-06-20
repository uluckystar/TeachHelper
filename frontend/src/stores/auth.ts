import { defineStore } from 'pinia'
import type { User, LoginRequest, RegisterRequest, AuthResponse } from '@/types/api'
import { authApi } from '@/api/auth'
import { validateTokenFormat, getTokenExpiration, isTokenExpiringSoon } from '@/utils/authMonitor'

interface AuthState {
  user: User | null
  token: string | null
  isInitialized: boolean
  _isLoading: boolean  // 防止竞态条件
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => {
    // 尝试从localStorage恢复用户信息，但避免解析错误
    let storedUser = null
    let storedToken = null
    
    try {
      const userStr = localStorage.getItem('user')
      const tokenStr = localStorage.getItem('token')
      
      if (userStr) {
        storedUser = JSON.parse(userStr)
      }
      if (tokenStr && tokenStr.trim()) {
        storedToken = tokenStr
      }
    } catch (error) {
      console.error('Error parsing stored auth data:', error)
      // 清理无效数据
      localStorage.removeItem('user')
      localStorage.removeItem('token')
    }
    
    if (import.meta.env.DEV) {
      console.log('Auth store initialization:', {
        hasStoredUser: !!storedUser,
        hasStoredToken: !!storedToken,
        tokenLength: storedToken?.length
      })
    }
    
    return {
      user: storedUser,
      token: storedToken,
      isInitialized: false,
      _isLoading: false
    }
  },

  getters: {
    isAuthenticated: (state) => !state._isLoading && !!state.token && !!state.user,
    isAdmin: (state) => state.user?.roles?.includes('ADMIN') || false,
    isTeacher: (state) => state.user?.roles?.includes('TEACHER') || false,
    isStudent: (state) => state.user?.roles?.includes('STUDENT') || false,
    hasRole: (state) => (role: string) => state.user?.roles?.includes(role) || false,
    primaryRole: (state) => state.user?.roles?.[0] || null
  },

  actions: {
    async login(credentials: LoginRequest): Promise<boolean> {
      try {
        const response = await authApi.login(credentials)
        this.setAuth(response)
        return true
      } catch (error) {
        console.error('Login failed:', error)
        return false
      }
    },

    async register(userData: RegisterRequest): Promise<boolean> {
      try {
        await authApi.register(userData)
        // 注册成功后，不会自动登录，需要手动登录
        return true
      } catch (error) {
        console.error('Registration failed:', error)
        return false
      }
    },

    async getCurrentUser(): Promise<void> {
      try {
        if (this.token) {
          if (import.meta.env.DEV) {
            console.log('Auth: Fetching current user with token:', this.token.substring(0, 20) + '...')
          }
          const user = await authApi.getCurrentUser()
          this.user = user
          // 同步到localStorage
          localStorage.setItem('user', JSON.stringify(user))
          if (import.meta.env.DEV) {
            console.log('Auth: User loaded successfully:', user)
          }
        }
      } catch (error: any) {
        console.error('Get current user failed:', error)
        if (import.meta.env.DEV) {
          console.log('Auth: getCurrentUser error details:', {
            status: error.response?.status,
            message: error.response?.data?.message,
            currentToken: this.token?.substring(0, 20) + '...'
          })
        }
        // token可能已过期或无效，清除认证状态
        this.logout()
        if (import.meta.env.DEV) {
          console.log('Auth: Cleared invalid token')
        }
      }
    },

    setAuth(authData: AuthResponse): void {
      // 验证token格式
      if (!validateTokenFormat(authData.token)) {
        console.error('Auth: Invalid token format received')
        return
      }
      
      // 检查token过期时间
      const expiration = getTokenExpiration(authData.token)
      if (expiration) {
        if (import.meta.env.DEV) {
          console.log('Auth: Token expires at:', expiration.toISOString())
        }
        
        if (expiration <= new Date()) {
          console.error('Auth: Received expired token')
          return
        }
      }
      
      // 从 AuthResponse 创建 User 对象
      this.user = {
        id: authData.id,
        username: authData.username,
        email: authData.email,
        roles: authData.roles
      }
      this.token = authData.token
      
      // 持久化到localStorage
      localStorage.setItem('token', authData.token)
      localStorage.setItem('user', JSON.stringify(this.user))
      
      if (import.meta.env.DEV) {
        console.log('Auth: setAuth completed', {
          user: this.user,
          token: !!this.token,
          tokenExpires: expiration?.toISOString()
        })
      }
    },

    logout(): void {
      if (import.meta.env.DEV) {
        console.log('Auth: Starting logout')
      }
      
      // 重置状态标志
      this._isLoading = false
      this.isInitialized = false  // 重置初始化状态，防止状态不一致
      
      // 清除认证数据
      this.user = null
      this.token = null
      
      // 清除localStorage
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      
      if (import.meta.env.DEV) {
        console.log('Auth: Logout completed')
      }
    },

    // 检查token有效性
    checkTokenValidity(): boolean {
      if (!this.token) {
        if (import.meta.env.DEV) {
          console.log('Auth: No token to check')
        }
        return false
      }

      // 检查token格式
      if (!validateTokenFormat(this.token)) {
        if (import.meta.env.DEV) {
          console.log('Auth: Invalid token format')
        }
        this.logout()
        return false
      }

      // 检查token是否过期
      const expiration = getTokenExpiration(this.token)
      if (expiration && expiration <= new Date()) {
        if (import.meta.env.DEV) {
          console.log('Auth: Token expired at:', expiration.toISOString())
        }
        this.logout()
        return false
      }

      // 检查token是否即将过期
      if (expiration && isTokenExpiringSoon(this.token)) {
        if (import.meta.env.DEV) {
          console.log('Auth: Token expiring soon:', expiration.toISOString())
        }
        // 这里可以添加自动刷新token的逻辑
      }

      return true
    },

    // 初始化认证状态
    async initAuth(): Promise<void> {
      // 防止重复初始化或并发初始化
      if (this._isLoading || this.isInitialized) {
        if (import.meta.env.DEV) {
          console.log('Auth: Skipping initialization - already loading or initialized', {
            isLoading: this._isLoading,
            isInitialized: this.isInitialized
          })
        }
        return
      }
      
      this._isLoading = true
      
      try {
        if (import.meta.env.DEV) {
          console.log('Auth: Starting initialization', {
            hasToken: !!this.token,
            hasUser: !!this.user
          })
        }
        
        // 只在有token时才进行用户验证，避免不必要的API调用
        if (this.token) {
          // 检查token有效性
          if (!this.checkTokenValidity()) {
            if (import.meta.env.DEV) {
              console.log('Auth: Token validation failed during init')
            }
            // token无效，直接清除状态
            this.user = null
            this.token = null
            localStorage.removeItem('user')
            localStorage.removeItem('token')
            return
          }
          
          // 如果有有效token但没有用户信息，获取用户信息
          if (!this.user) {
            if (import.meta.env.DEV) {
              console.log('Auth: Fetching user info with valid token')
            }
            await this.getCurrentUser()
          }
        } else {
          // 没有token时，清除可能的过期用户信息
          if (this.user) {
            this.user = null
            localStorage.removeItem('user')
            if (import.meta.env.DEV) {
              console.log('Auth: Cleared stale user data without token')
            }
          }
        }
        
      } catch (error) {
        console.error('Auth: Initialization error:', error)
        // 初始化失败时清除状态
        this.logout()
      } finally {
        this._isLoading = false
        this.isInitialized = true
        
        if (import.meta.env.DEV) {
          console.log('Auth: Initialization complete', {
            isAuthenticated: this.isAuthenticated,
            hasUser: !!this.user,
            hasToken: !!this.token
          })
        }
      }
    }
  }
})
