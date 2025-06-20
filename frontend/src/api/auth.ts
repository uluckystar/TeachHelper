import api from '@/utils/request'
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  User
} from '@/types/api'

export const authApi = {
  // 登录
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', credentials)
    return response.data
  },

  // 注册
  async register(userData: RegisterRequest): Promise<string> {
    const response = await api.post<string>('/auth/register', userData)
    return response.data
  },

  // 获取当前用户信息
  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/auth/me')
    return response.data
  }
}
