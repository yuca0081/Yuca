import { sha256 as _sha256 } from 'js-sha256'

export function sha256(message: string): string {
  return _sha256(message)
}
