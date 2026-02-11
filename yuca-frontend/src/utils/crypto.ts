import SHA256 from 'crypto-js/sha256'

/**
 * 使用 SHA-256 哈希密码
 * @param password 原始密码
 * @returns 哈希后的密码
 */
export function hashPassword(password: string): string {
  return SHA256(password).toString()
}
