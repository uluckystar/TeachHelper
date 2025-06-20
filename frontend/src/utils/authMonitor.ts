// 认证状态监控工具
import { useAuthStore } from '@/stores/auth'

let tokenCheckInterval: number | null = null

export function startAuthMonitoring() {
  if (tokenCheckInterval) {
    clearInterval(tokenCheckInterval)
  }

  // 每60秒简单检查一次关键状态不一致 (减少干预)
  tokenCheckInterval = window.setInterval(() => {
    const authStore = useAuthStore()
    
    // 只检查localStorage和store的严重不一致情况
    const storageToken = localStorage.getItem('token')
    
    if (import.meta.env.DEV) {
      console.log('Auth Monitor - Periodic check:', {
        timestamp: new Date().toISOString(),
        storeHasToken: !!authStore.token,
        storageHasToken: !!storageToken,
        isAuthenticated: authStore.isAuthenticated
      })
    }
    
    // 只处理明显的状态不一致：localStorage token被外部移除但store还有
    if (!storageToken && authStore.token) {
      console.warn('Auth Monitor - Token removed from localStorage, clearing store')
      authStore.logout()
      return
    }
    
    // 避免过度干预，不做复杂的同步操作
  }, 60000) // 60秒

  if (import.meta.env.DEV) {
    console.log('Auth Monitor - Started simplified monitoring (60s interval)')
  }
}

export function stopAuthMonitoring() {
  if (tokenCheckInterval) {
    clearInterval(tokenCheckInterval)
    tokenCheckInterval = null
    if (import.meta.env.DEV) {
      console.log('Auth Monitor - Stopped periodic checking')
    }
  }
}

// 检查token格式是否有效
export function validateTokenFormat(token: string): boolean {
  if (!token) return false
  
  // JWT token应该有三个部分，用.分隔
  const parts = token.split('.')
  if (parts.length !== 3) return false
  
  // 检查每个部分是否是有效的base64
  try {
    parts.forEach(part => {
      atob(part.replace(/-/g, '+').replace(/_/g, '/'))
    })
    return true
  } catch (error) {
    return false
  }
}

// 解析JWT token获取过期时间
export function getTokenExpiration(token: string): Date | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
    
    if (payload.exp) {
      return new Date(payload.exp * 1000)
    }
    
    return null
  } catch (error) {
    console.error('Failed to parse token:', error)
    return null
  }
}

// 检查token是否即将过期（5分钟内）
export function isTokenExpiringSoon(token: string): boolean {
  const expiration = getTokenExpiration(token)
  if (!expiration) return false
  
  const now = new Date()
  const fiveMinutesFromNow = new Date(now.getTime() + 5 * 60 * 1000)
  
  return expiration <= fiveMinutesFromNow
}
