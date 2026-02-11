/**
 * 工具模块类型定义
 */

/**
 * 工具
 */
export interface Tool {
  id: string
  name: string
  description: string
  icon: any
  route: string
  category: string
}

/**
 * 工具操作类型
 */
export type ToolAction = 'format' | 'compress' | 'encode' | 'decode' | 'generate' | 'validate'

/**
 * 工具处理结果
 */
export interface ToolResult {
  success: boolean
  data: string
  error?: string
}

/**
 * JSON 工具选项
 */
export interface JsonOptions {
  indent: number
  sortKeys: boolean
}

/**
 * JWT 令牌信息
 */
export interface JwtInfo {
  header: Record<string, any>
  payload: Record<string, any>
  signature: string
  valid: boolean
  error?: string
}

/**
 * 哈希算法类型
 */
export type HashAlgorithm = 'md5' | 'sha-1' | 'sha-256' | 'sha-512'

/**
 * 正则测试结果
 */
export interface RegexTestResult {
  match: boolean
  matches: string[]
  groups: Record<string, string>[]
  error?: string
}

/**
 * UUID 格式
 */
export type UuidVersion = 'v4' | 'v1'
