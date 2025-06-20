// Element Plus tag type utilities
export type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined

export function getQuestionTypeTag(type: string): TagType {
  const colorMap: Record<string, TagType> = {
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'success',
    'TRUE_FALSE': 'info',
    'SHORT_ANSWER': 'warning',
    'ESSAY': 'danger',
    'CODING': 'primary',
    'CASE_ANALYSIS': 'warning',
    'CALCULATION': 'success'
  }
  return colorMap[type] || 'info'
}

// Alias for backward compatibility
export const getQuestionTypeColor = getQuestionTypeTag

export function getQuestionTypeText(type: string): string {
  const textMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '论述题',
    'CODING': '编程题',
    'CASE_ANALYSIS': '案例分析题',
    'CALCULATION': '计算题'
  }
  return textMap[type] || type
}

export function getStatusTag(status: string): TagType {
  const statusMap: Record<string, TagType> = {
    'PENDING': 'warning',
    'IN_PROGRESS': 'primary',
    'COMPLETED': 'success',
    'EVALUATED': 'success',
    'ENDED': 'warning',
    'CANCELLED': 'danger',
    'DRAFT': 'info',
    'PUBLISHED': 'success',
    'ARCHIVED': 'info',
    'ACTIVE': 'success',
    'INACTIVE': 'info'
  }
  return statusMap[status] || 'info'
}

export function getStatusText(status: string): string {
  const textMap: Record<string, string> = {
    'PENDING': '待开始',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'EVALUATED': '已评估',
    'ENDED': '已结束',
    'CANCELLED': '已取消',
    'DRAFT': '草稿',
    'PUBLISHED': '已发布',
    'ARCHIVED': '已归档',
    'ACTIVE': '进行中',
    'INACTIVE': '已结束'
  }
  return textMap[status] || status
}

export function getTaskStatusTag(status: string): TagType {
  const statusMap: Record<string, TagType> = {
    'RUNNING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'PENDING': 'info',
    'CANCELLED': 'info'
  }
  return statusMap[status] || 'info'
}

// Alias for backward compatibility
export const getTaskStatusType = getTaskStatusTag

export function getRoleTag(role: string): TagType {
  const roleMap: Record<string, TagType> = {
    'ADMIN': 'danger',
    'TEACHER': 'primary',
    'STUDENT': 'success',
    'GUEST': 'info'
  }
  return roleMap[role] || 'info'
}

export function getTaskTypeTag(type: string): TagType {
  const typeMap: Record<string, TagType> = {
    'AI_COMPREHENSIVE': 'primary',
    'AI_GRAMMAR': 'info',
    'AI_KEYWORD': 'warning',
    'MANUAL': 'success',
    'BATCH': 'primary'
  }
  return typeMap[type] || 'info'
}

export function getExamStatusType(status: string): TagType {
  return getStatusTag(status)
}

export function getExamStatusText(status: string): string {
  return getStatusText(status)
}

export function getExamStatusTag(status: string): TagType {
  return getStatusTag(status)
}

export function getScoreTag(score: number | null | undefined, maxScore: number | null | undefined): TagType {
  if (score === null || score === undefined || maxScore === null || maxScore === undefined) {
    return 'info'
  }
  
  const percentage = (score / maxScore) * 100
  
  if (percentage >= 90) return 'success'
  if (percentage >= 80) return 'primary'
  if (percentage >= 70) return 'warning'
  if (percentage >= 60) return 'info'
  return 'danger'
}
