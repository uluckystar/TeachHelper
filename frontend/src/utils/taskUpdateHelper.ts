/**
 * 任务状态更新工具
 * 处理任务操作后的状态同步，避免频繁的页面刷新
 */

export interface TaskUpdate {
  taskId: string
  status?: string
  progress?: number
  processedCount?: number
  totalCount?: number
  error?: string
  result?: any
  [key: string]: any
}

/**
 * 更新任务列表中的单个任务 - 智能更新，只更新有变化的字段
 */
export function updateTaskInList(tasks: any[], taskId: string, update: TaskUpdate): boolean {
  const taskIndex = tasks.findIndex(t => (t.taskId || t.id) === taskId)
  if (taskIndex > -1) {
    const currentTask = tasks[taskIndex]
    let hasChanges = false
    
    // 检查是否有实际变化
    const fieldsToCheck = ['status', 'progress', 'processedCount', 'totalCount', 'error', 'result']
    for (const field of fieldsToCheck) {
      if (update[field] !== undefined && update[field] !== currentTask[field]) {
        hasChanges = true
        break
      }
    }
    
    // 只有在有实际变化时才更新
    if (hasChanges) {
      tasks[taskIndex] = {
        ...currentTask,
        ...update,
        updatedAt: new Date().toISOString()
      }
      console.log(`📝 任务 ${taskId} 状态更新:`, {
        old: { status: currentTask.status, progress: currentTask.progress },
        new: { status: update.status, progress: update.progress }
      })
    }
    
    return hasChanges
  }
  return false
}

/**
 * 统计任务状态数量
 */
export function calculateTaskStats(tasks: any[]) {
  const stats = {
    running: 0,
    pending: 0,
    completed: 0,
    failed: 0,
    paused: 0,
    cancelled: 0,
    total: tasks.length
  }

  tasks.forEach(task => {
    const status = task.status?.toLowerCase()
    if (status === 'running') stats.running++
    else if (status === 'pending') stats.pending++
    else if (status === 'completed') stats.completed++
    else if (status === 'failed') stats.failed++
    else if (status === 'paused') stats.paused++
    else if (status === 'cancelled') stats.cancelled++
  })

  return stats
}

/**
 * 任务操作成功后的状态预测更新
 * 在WebSocket更新到达之前提供即时的UI反馈
 */
export function preUpdateTaskStatus(tasks: any[], taskId: string, operation: 'pause' | 'resume' | 'cancel'): boolean {
  const taskIndex = tasks.findIndex(t => (t.taskId || t.id) === taskId)
  if (taskIndex > -1) {
    const task = tasks[taskIndex]
    
    switch (operation) {
      case 'pause':
        if (task.status === 'RUNNING') {
          task.status = 'PAUSED'
          task.updatedAt = new Date().toISOString()
          return true
        }
        break
      case 'resume':
        if (task.status === 'PAUSED') {
          task.status = 'RUNNING'
          task.updatedAt = new Date().toISOString()
          return true
        }
        break
      case 'cancel':
        if (['RUNNING', 'PAUSED', 'PENDING'].includes(task.status)) {
          task.status = 'CANCELLED'
          task.completedAt = new Date().toISOString()
          task.updatedAt = new Date().toISOString()
          return true
        }
        break
    }
  }
  return false
}

/**
 * 智能任务更新策略
 * 根据操作类型决定是否需要重新加载数据
 */
export function shouldReloadTasks(operation: string, currentTasks: any[]): boolean {
  // 对于删除操作，需要重新加载
  if (operation === 'delete' || operation === 'clear') {
    return true
  }
  
  // 对于新增任务，如果当前列表为空，需要重新加载
  if (operation === 'create' && currentTasks.length === 0) {
    return true
  }
  
  // 其他操作依赖WebSocket更新
  return false
}
