/**
 * 防抖工具函数
 * 用于防止用户快速连续点击导致的重复请求
 */

const pendingRequests = new Map<string, boolean>()

/**
 * 防重复执行装饰器
 */
export function preventDuplicate<T extends (...args: any[]) => Promise<any>>(
  fn: T,
  getKey: (...args: Parameters<T>) => string = () => fn.name
): T {
  return (async (...args: Parameters<T>) => {
    const key = getKey(...args)
    
    if (pendingRequests.get(key)) {
      console.log(`🔒 操作 ${key} 正在进行中，忽略重复请求`)
      return
    }
    
    try {
      pendingRequests.set(key, true)
      console.log(`🚀 开始执行操作: ${key}`)
      
      const result = await fn(...args)
      
      console.log(`✅ 操作完成: ${key}`)
      return result
    } catch (error) {
      console.error(`❌ 操作失败: ${key}`, error)
      throw error
    } finally {
      pendingRequests.delete(key)
    }
  }) as T
}

/**
 * 为任务操作生成唯一键
 */
export function getTaskOperationKey(operation: string, taskId: string): string {
  return `${operation}-${taskId}`
}

/**
 * 检查操作是否正在进行
 */
export function isOperationPending(key: string): boolean {
  return pendingRequests.get(key) || false
}
