import { ref, reactive } from 'vue'

export interface TaskUpdate {
  taskId: string
  status: string
  progress?: number
  message?: string
  error?: string
  result?: any
}

class TaskWebSocketService {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectInterval = 3000
  private subscribers = new Map<string, ((update: TaskUpdate) => void)[]>()
  
  public isConnected = ref(false)
  public connectionStatus = ref<'connecting' | 'connected' | 'disconnected' | 'error'>('disconnected')

  constructor() {
    this.connect()
  }

  private connect() {
    try {
      this.connectionStatus.value = 'connecting'
      
      // 构建WebSocket URL
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = window.location.host
      const wsUrl = `${protocol}//${host}/ws/tasks`
      
      this.ws = new WebSocket(wsUrl)
      
      this.ws.onopen = () => {
        console.log('任务WebSocket连接已建立')
        this.isConnected.value = true
        this.connectionStatus.value = 'connected'
        this.reconnectAttempts = 0
      }
      
      this.ws.onmessage = (event) => {
        try {
          const update: TaskUpdate = JSON.parse(event.data)
          this.handleTaskUpdate(update)
        } catch (error) {
          console.error('解析WebSocket消息失败:', error)
        }
      }
      
      this.ws.onclose = (event) => {
        console.log('任务WebSocket连接已关闭:', event.code, event.reason)
        this.isConnected.value = false
        this.connectionStatus.value = 'disconnected'
        
        // 自动重连
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          setTimeout(() => {
            this.reconnectAttempts++
            console.log(`尝试重连任务WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
            this.connect()
          }, this.reconnectInterval)
        }
      }
      
      this.ws.onerror = (error) => {
        console.error('任务WebSocket错误:', error)
        this.connectionStatus.value = 'error'
      }
      
    } catch (error) {
      console.error('创建任务WebSocket连接失败:', error)
      this.connectionStatus.value = 'error'
    }
  }

  private handleTaskUpdate(update: TaskUpdate) {
    console.log('收到任务更新:', update)
    
    // 通知所有订阅者
    const allSubscribers = this.subscribers.get('*') || []
    const taskSubscribers = this.subscribers.get(update.taskId) || []
    
    const allCallbacks = allSubscribers.concat(taskSubscribers)
    allCallbacks.forEach(callback => {
      try {
        callback(update)
      } catch (error) {
        console.error('处理任务更新回调失败:', error)
      }
    })
    
    // 触发全局事件，供不使用hook的组件监听
    window.dispatchEvent(new CustomEvent('taskUpdate', { detail: update }))
  }

  // 订阅任务更新
  public subscribe(taskId: string | '*', callback: (update: TaskUpdate) => void) {
    if (!this.subscribers.has(taskId)) {
      this.subscribers.set(taskId, [])
    }
    this.subscribers.get(taskId)!.push(callback)
    
    // 返回取消订阅函数
    return () => {
      const subscribers = this.subscribers.get(taskId)
      if (subscribers) {
        const index = subscribers.indexOf(callback)
        if (index > -1) {
          subscribers.splice(index, 1)
        }
        if (subscribers.length === 0) {
          this.subscribers.delete(taskId)
        }
      }
    }
  }

  // 发送消息到服务器
  public sendMessage(message: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else {
      console.warn('WebSocket未连接，无法发送消息')
    }
  }

  // 手动重连
  public reconnect() {
    if (this.ws) {
      this.ws.close()
    }
    this.reconnectAttempts = 0
    this.connect()
  }

  // 关闭连接
  public disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.isConnected.value = false
    this.connectionStatus.value = 'disconnected'
  }
}

// 单例实例
let instance: TaskWebSocketService | null = null

export function useTaskWebSocket() {
  if (!instance) {
    instance = new TaskWebSocketService()
  }
  
  return {
    isConnected: instance.isConnected,
    connectionStatus: instance.connectionStatus,
    subscribe: instance.subscribe.bind(instance),
    sendMessage: instance.sendMessage.bind(instance),
    reconnect: instance.reconnect.bind(instance),
    disconnect: instance.disconnect.bind(instance)
  }
}

// 用于组件中的便捷hooks
export function useTaskUpdates(taskId?: string) {
  const taskUpdates = reactive<Record<string, TaskUpdate>>({})
  const { subscribe } = useTaskWebSocket()
  
  // 订阅指定任务或所有任务的更新
  const unsubscribe = subscribe(taskId || '*', (update) => {
    taskUpdates[update.taskId] = update
  })
  
  return {
    taskUpdates,
    unsubscribe
  }
}

export default TaskWebSocketService
