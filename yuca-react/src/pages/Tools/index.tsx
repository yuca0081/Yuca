import { useState } from 'react'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Copy, Check, ArrowRightLeft, FileJson, Key, Binary, Link2, Clock, Hash, Regex, Fingerprint, FileText } from 'lucide-react'

const inputCls = "bg-white border-2 border-foreground focus:border-[#FF6B35] rounded-none shadow-none"
const cardCls = "bg-white p-4 border-2 border-foreground shadow-[6px_6px_0_#2C1810] flex flex-col"
const codeCls = "bg-[#FFF5E6] px-3 py-2 border-2 border-foreground text-sm font-mono"

// JSON Tool
function JsonTool() {
  const [input, setInput] = useState('')
  const [output, setOutput] = useState('')
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)

  const formatJson = () => {
    try {
      const parsed = JSON.parse(input)
      setOutput(JSON.stringify(parsed, null, 2))
      setError('')
    } catch (e: any) {
      setError(e.message)
    }
  }

  const compressJson = () => {
    try {
      const parsed = JSON.parse(input)
      setOutput(JSON.stringify(parsed))
      setError('')
    } catch (e: any) {
      setError(e.message)
    }
  }

  const handleCopy = () => {
    navigator.clipboard.writeText(output)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className={cardCls}>
        <label className="text-sm font-medium mb-2">输入 JSON</label>
        <Textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder='{"key": "value"}'
          className={`flex-1 min-h-[300px] font-mono text-sm ${inputCls} resize-none`}
        />
        <div className="flex gap-3 mt-3">
          <button onClick={formatJson} className="btn-primary text-sm px-4 py-2">格式化</button>
          <button onClick={compressJson} className="btn-secondary text-sm px-4 py-2">压缩</button>
        </div>
      </div>
      <div className={cardCls}>
        <div className="flex items-center justify-between mb-2">
          <label className="text-sm font-medium">输出</label>
          <button onClick={handleCopy} className="text-[#6B5344] hover:text-[#FF6B35] transition-colors cursor-pointer">
            {copied ? <Check className="w-4 h-4 text-[#4A7C59]" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>
        {error ? (
          <div className="flex-1 bg-red-50 border-2 border-red-300 p-4 text-red-600 text-sm font-mono">
            {error}
          </div>
        ) : (
          <Textarea value={output} readOnly className={`flex-1 min-h-[300px] font-mono text-sm ${inputCls} resize-none`} />
        )}
      </div>
    </div>
  )
}

// Base64 Tool
function Base64Tool() {
  const [input, setInput] = useState('')
  const [output, setOutput] = useState('')
  const [mode, setMode] = useState<'encode' | 'decode'>('encode')
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState('')

  const convert = () => {
    try {
      if (mode === 'encode') {
        setOutput(btoa(unescape(encodeURIComponent(input))))
      } else {
        setOutput(decodeURIComponent(escape(atob(input))))
      }
      setError('')
    } catch {
      setError(mode === 'encode' ? '编码失败' : '解码失败，请检查输入')
    }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className={cardCls}>
        <label className="text-sm font-medium mb-2">{mode === 'encode' ? '原始文本' : 'Base64 字符串'}</label>
        <Textarea value={input} onChange={(e) => setInput(e.target.value)} placeholder={mode === 'encode' ? '输入要编码的文本' : '输入 Base64 字符串'} className={`flex-1 min-h-[300px] font-mono text-sm ${inputCls} resize-none`} />
        <div className="flex gap-3 mt-3">
          <button onClick={convert} className="btn-primary text-sm px-4 py-2">{mode === 'encode' ? '编码' : '解码'}</button>
          <button onClick={() => { setMode(mode === 'encode' ? 'decode' : 'encode'); setInput(''); setOutput(''); setError('') }} className="btn-secondary text-sm px-4 py-2 flex items-center gap-1">
            <ArrowRightLeft className="w-3 h-3" />切换
          </button>
        </div>
      </div>
      <div className={cardCls}>
        <div className="flex items-center justify-between mb-2">
          <label className="text-sm font-medium">{mode === 'encode' ? 'Base64 结果' : '解码结果'}</label>
          <button onClick={() => { navigator.clipboard.writeText(output); setCopied(true); setTimeout(() => setCopied(false), 2000) }} className="cursor-pointer text-[#6B5344] hover:text-[#FF6B35] transition-colors">
            {copied ? <Check className="w-4 h-4 text-[#4A7C59]" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>
        {error ? <div className="flex-1 bg-red-50 border-2 border-red-300 p-4 text-red-600 text-sm">{error}</div> : <Textarea value={output} readOnly className={`flex-1 min-h-[300px] font-mono text-sm ${inputCls} resize-none`} />}
      </div>
    </div>
  )
}

// URL Tool
function UrlTool() {
  const [input, setInput] = useState('')
  const [output, setOutput] = useState('')
  const [mode, setMode] = useState<'encode' | 'decode'>('encode')
  const [copied, setCopied] = useState(false)

  const convert = () => { setOutput(mode === 'encode' ? encodeURIComponent(input) : decodeURIComponent(input)) }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className={cardCls}>
        <label className="text-sm font-medium mb-2">{mode === 'encode' ? '原始 URL' : '编码后的 URL'}</label>
        <Textarea value={input} onChange={(e) => setInput(e.target.value)} placeholder={mode === 'encode' ? '输入要编码的 URL' : '输入要解码的 URL'} className={`flex-1 min-h-[300px] font-mono text-sm ${inputCls} resize-none`} />
        <div className="flex gap-3 mt-3">
          <button onClick={convert} className="btn-primary text-sm px-4 py-2">{mode === 'encode' ? '编码' : '解码'}</button>
          <button onClick={() => { setMode(mode === 'encode' ? 'decode' : 'encode'); setInput(''); setOutput('') }} className="btn-secondary text-sm px-4 py-2 flex items-center gap-1">
            <ArrowRightLeft className="w-3 h-3" />切换
          </button>
        </div>
      </div>
      <div className={cardCls}>
        <div className="flex items-center justify-between mb-2">
          <label className="text-sm font-medium">结果</label>
          <button onClick={() => { navigator.clipboard.writeText(output); setCopied(true); setTimeout(() => setCopied(false), 2000) }} className="cursor-pointer text-[#6B5344] hover:text-[#FF6B35] transition-colors">
            {copied ? <Check className="w-4 h-4 text-[#4A7C59]" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>
        <Textarea value={output} readOnly className={`flex-1 min-h-[300px] font-mono text-sm ${inputCls} resize-none`} />
      </div>
    </div>
  )
}

// Timestamp Tool
function TimestampTool() {
  const [timestamp, setTimestamp] = useState(Date.now().toString())
  const [dateStr, setDateStr] = useState(new Date().toISOString())

  const tsToDate = () => { try { setDateStr(new Date(Number(timestamp) * 1000).toLocaleString('zh-CN')) } catch {} }
  const dateToTs = () => { try { setTimestamp(Math.floor(new Date(dateStr).getTime() / 1000).toString()) } catch {} }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div className="bg-white p-6 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
        <h3 className="font-bold mb-4">时间戳 &rarr; 日期</h3>
        <Input value={timestamp} onChange={(e) => setTimestamp(e.target.value)} placeholder="输入时间戳" className={`h-11 mb-3 font-mono ${inputCls}`} />
        <button onClick={tsToDate} className="btn-primary text-sm px-4 py-2 w-full">转换</button>
        <p className="mt-3 text-sm text-[#6B5344] bg-[#FFF5E6] p-3 border-2 border-foreground">{dateStr}</p>
        <p className="mt-2 text-xs text-[#6B5344]">当前时间戳: {Math.floor(Date.now() / 1000)}</p>
      </div>
      <div className="bg-white p-6 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
        <h3 className="font-bold mb-4">日期 &rarr; 时间戳</h3>
        <Input value={dateStr} onChange={(e) => setDateStr(e.target.value)} placeholder="输入日期" className={`h-11 mb-3 ${inputCls}`} />
        <button onClick={dateToTs} className="btn-primary text-sm px-4 py-2 w-full">转换</button>
        <p className="mt-3 text-sm text-[#6B5344] bg-[#FFF5E6] p-3 border-2 border-foreground font-mono">{timestamp}</p>
      </div>
    </div>
  )
}

// Hash Tool
function HashTool() {
  const [input, setInput] = useState('')
  const [results, setResults] = useState<Record<string, string>>({})

  const generate = async () => {
    const encoder = new TextEncoder()
    const data = encoder.encode(input)
    const algorithms = ['SHA-1', 'SHA-256', 'SHA-512']
    const newResults: Record<string, string> = {}
    for (const alg of algorithms) {
      const hashBuffer = await crypto.subtle.digest(alg, data)
      const hashArray = Array.from(new Uint8Array(hashBuffer))
      newResults[alg] = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
    }
    setResults(newResults)
  }

  return (
    <div className="bg-white p-6 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
      <label className="text-sm font-medium mb-2 block">输入文本</label>
      <Input value={input} onChange={(e) => setInput(e.target.value)} placeholder="输入要哈希的文本" className={`h-11 mb-3 ${inputCls}`} />
      <button onClick={generate} className="btn-primary text-sm px-4 py-2">生成哈希</button>
      {Object.entries(results).map(([alg, hash]) => (
        <div key={alg} className="mt-3">
          <label className="text-xs font-medium text-[#6B5344]">{alg}</label>
          <div className="flex items-center gap-2 mt-1">
            <code className={`flex-1 text-xs p-2 break-all ${codeCls}`}>{hash}</code>
            <button onClick={() => navigator.clipboard.writeText(hash)} className="cursor-pointer text-[#6B5344] hover:text-[#FF6B35]"><Copy className="w-4 h-4" /></button>
          </div>
        </div>
      ))}
    </div>
  )
}

// Regex Tool
function RegexTool() {
  const [pattern, setPattern] = useState('')
  const [flags, setFlags] = useState('g')
  const [testStr, setTestStr] = useState('')
  const [matches, setMatches] = useState<string[]>([])
  const [error, setError] = useState('')

  const test = () => {
    try {
      const regex = new RegExp(pattern, flags)
      const allMatches = [...testStr.matchAll(regex)].flatMap(m => m.filter(Boolean))
      setMatches([...new Set(allMatches)])
      setError('')
    } catch (e: any) { setError(e.message); setMatches([]) }
  }

  return (
    <div className="space-y-6">
      <div className="bg-white p-4 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
        <div className="flex gap-3">
          <div className="flex-1"><Input value={pattern} onChange={(e) => setPattern(e.target.value)} placeholder="正则表达式" className={`h-11 font-mono ${inputCls}`} /></div>
          <Input value={flags} onChange={(e) => setFlags(e.target.value)} placeholder="标志" className={`w-24 h-11 font-mono text-center ${inputCls}`} />
          <button onClick={test} className="btn-primary text-sm px-4 py-2">测试</button>
        </div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className={cardCls}>
          <label className="text-sm font-medium mb-2 block">测试字符串</label>
          <Textarea value={testStr} onChange={(e) => setTestStr(e.target.value)} placeholder="输入测试文本" className={`min-h-[200px] ${inputCls} resize-none`} />
        </div>
        <div className={cardCls}>
          <label className="text-sm font-medium mb-2 block">匹配结果</label>
          {error ? <div className="text-red-600 text-sm">{error}</div> : (
            <div className="space-y-2">
              {matches.length === 0 ? <p className="text-[#6B5344] text-sm">无匹配</p> :
                matches.map((m, i) => <code key={i} className={`block px-3 py-1.5 text-sm font-mono ${codeCls}`}>{m}</code>)}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// UUID Tool
function UuidTool() {
  const [uuids, setUuids] = useState<string[]>([])
  const [count, setCount] = useState(1)
  const [copied, setCopied] = useState(false)

  const generate = () => { setUuids(Array.from({ length: count }, () => crypto.randomUUID())) }
  const copyAll = () => { navigator.clipboard.writeText(uuids.join('\n')); setCopied(true); setTimeout(() => setCopied(false), 2000) }

  return (
    <div className="bg-white p-6 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
      <div className="flex items-center gap-3 mb-4">
        <Input type="number" min={1} max={100} value={count} onChange={(e) => setCount(Number(e.target.value))} className={`w-24 h-11 text-center ${inputCls}`} />
        <button onClick={generate} className="btn-primary text-sm px-4 py-2">生成 UUID</button>
        {uuids.length > 0 && (
          <button onClick={copyAll} className="btn-secondary text-sm px-4 py-2 flex items-center gap-1">
            {copied ? <Check className="w-4 h-4 text-[#4A7C59]" /> : <Copy className="w-4 h-4" />}复制全部
          </button>
        )}
      </div>
      <div className="space-y-2">
        {uuids.map((uuid, i) => (
          <div key={i} className="flex items-center gap-2">
            <code className={`flex-1 px-3 py-2 text-sm font-mono ${codeCls}`}>{uuid}</code>
            <button onClick={() => navigator.clipboard.writeText(uuid)} className="cursor-pointer text-[#6B5344] hover:text-[#FF6B35]"><Copy className="w-4 h-4" /></button>
          </div>
        ))}
      </div>
    </div>
  )
}

// JWT Tool
function JwtTool() {
  const [token, setToken] = useState('')
  const [header, setHeader] = useState('')
  const [payload, setPayload] = useState('')
  const [error, setError] = useState('')

  const decode = () => {
    try {
      const parts = token.split('.')
      if (parts.length !== 3) throw new Error('无效的 JWT 格式')
      setHeader(JSON.stringify(JSON.parse(atob(parts[0])), null, 2))
      setPayload(JSON.stringify(JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))), null, 2))
      setError('')
    } catch (e: any) { setError(e.message); setHeader(''); setPayload('') }
  }

  return (
    <div className="space-y-6">
      <div className="bg-white p-4 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
        <label className="text-sm font-medium mb-2 block">JWT Token</label>
        <Textarea value={token} onChange={(e) => setToken(e.target.value)} placeholder="粘贴 JWT token" className={`min-h-[80px] font-mono text-sm ${inputCls} resize-none`} />
        <button onClick={decode} className="btn-primary text-sm px-4 py-2 mt-3">解码</button>
        {error && <p className="text-red-600 text-sm mt-2">{error}</p>}
      </div>
      {(header || payload) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white p-4 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
            <label className="text-sm font-medium mb-2 block">Header</label>
            <pre className={`p-4 text-sm font-mono overflow-auto ${codeCls}`}>{header}</pre>
          </div>
          <div className="bg-white p-4 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
            <label className="text-sm font-medium mb-2 block">Payload</label>
            <pre className={`p-4 text-sm font-mono overflow-auto ${codeCls}`}>{payload}</pre>
          </div>
        </div>
      )}
    </div>
  )
}

// MD5 implementation
function md5(input: string): string {
  function safeAdd(x: number, y: number) { const l = (x & 0xffff) + (y & 0xffff); return (((x >> 16) + (y >> 16) + (l >> 16)) << 16) | (l & 0xffff) }
  function bitRotateLeft(n: number, c: number) { return (n << c) | (n >>> (32 - c)) }
  function md5cmn(q: number, a: number, b: number, x: number, s: number, t: number) { return safeAdd(bitRotateLeft(safeAdd(safeAdd(a, q), safeAdd(x, t)), s), b) }
  function md5ff(a: number, b: number, c: number, d: number, x: number, s: number, t: number) { return md5cmn((b & c) | (~b & d), a, b, x, s, t) }
  function md5gg(a: number, b: number, c: number, d: number, x: number, s: number, t: number) { return md5cmn((b & d) | (c & ~d), a, b, x, s, t) }
  function md5hh(a: number, b: number, c: number, d: number, x: number, s: number, t: number) { return md5cmn(b ^ c ^ d, a, b, x, s, t) }
  function md5ii(a: number, b: number, c: number, d: number, x: number, s: number, t: number) { return md5cmn(c ^ (b | ~d), a, b, x, s, t) }
  function binlMD5(x: number[], len: number): number[] {
    x[len >> 5] |= 0x80 << (len % 32)
    x[(((len + 64) >>> 9) << 4) + 14] = len
    let a = 1732584193, b = -271733879, c = -1732584194, d = 271733878
    for (let i = 0; i < x.length; i += 16) {
      const oa = a, ob = b, oc = c, od = d
      a = md5ff(a, b, c, d, x[i], 7, -680876936); d = md5ff(d, a, b, c, x[i + 1], 12, -389564586)
      c = md5ff(c, d, a, b, x[i + 2], 17, 606105819); b = md5ff(b, c, d, a, x[i + 3], 22, -1044525330)
      a = md5ff(a, b, c, d, x[i + 4], 7, -176418897); d = md5ff(d, a, b, c, x[i + 5], 12, 1200080426)
      c = md5ff(c, d, a, b, x[i + 6], 17, -1473231341); b = md5ff(b, c, d, a, x[i + 7], 22, -45705983)
      a = md5ff(a, b, c, d, x[i + 8], 7, 1770035416); d = md5ff(d, a, b, c, x[i + 9], 12, -1958414417)
      c = md5ff(c, d, a, b, x[i + 10], 17, -42063); b = md5ff(b, c, d, a, x[i + 11], 22, -1990404162)
      a = md5ff(a, b, c, d, x[i + 12], 7, 1804603682); d = md5ff(d, a, b, c, x[i + 13], 12, -40341101)
      c = md5ff(c, d, a, b, x[i + 14], 17, -1502002290); b = md5ff(b, c, d, a, x[i + 15], 22, 1236535329)
      a = md5gg(a, b, c, d, x[i + 1], 5, -165796510); d = md5gg(d, a, b, c, x[i + 6], 9, -1069501632)
      c = md5gg(c, d, a, b, x[i + 11], 14, 643717713); b = md5gg(b, c, d, a, x[i], 20, -373897302)
      a = md5gg(a, b, c, d, x[i + 5], 5, -701558691); d = md5gg(d, a, b, c, x[i + 10], 9, 38016083)
      c = md5gg(c, d, a, b, x[i + 15], 14, -660478335); b = md5gg(b, c, d, a, x[i + 4], 20, -405537848)
      a = md5gg(a, b, c, d, x[i + 9], 5, 568446438); d = md5gg(d, a, b, c, x[i + 14], 9, -1019803690)
      c = md5gg(c, d, a, b, x[i + 3], 14, -187363961); b = md5gg(b, c, d, a, x[i + 8], 20, 1163531501)
      a = md5gg(a, b, c, d, x[i + 13], 5, -1444681467); d = md5gg(d, a, b, c, x[i + 6], 9, -51403784)
      c = md5gg(c, d, a, b, x[i + 11], 14, 1735328473); b = md5gg(b, c, d, a, x[i], 20, -1926607734)
      a = md5hh(a, b, c, d, x[i + 5], 4, -378558); d = md5hh(d, a, b, c, x[i + 8], 11, -2022574463)
      c = md5hh(c, d, a, b, x[i + 11], 16, 1839030562); b = md5hh(b, c, d, a, x[i + 14], 23, -35309556)
      a = md5hh(a, b, c, d, x[i + 1], 4, -1530992060); d = md5hh(d, a, b, c, x[i + 4], 11, 1272893353)
      c = md5hh(c, d, a, b, x[i + 7], 16, -155497632); b = md5hh(b, c, d, a, x[i + 10], 23, -1094730640)
      a = md5hh(a, b, c, d, x[i + 13], 4, 681279174); d = md5hh(d, a, b, c, x[i + 0], 11, -358537222)
      c = md5hh(c, d, a, b, x[i + 3], 16, -722521979); b = md5hh(b, c, d, a, x[i + 6], 23, 76029189)
      a = md5hh(a, b, c, d, x[i + 9], 4, -640364487); d = md5hh(d, a, b, c, x[i + 12], 11, -421815835)
      c = md5hh(c, d, a, b, x[i + 15], 16, 530742520); b = md5hh(b, c, d, a, x[i + 2], 23, -995338651)
      a = md5ii(a, b, c, d, x[i], 6, -198630844); d = md5ii(d, a, b, c, x[i + 7], 10, 1126891415)
      c = md5ii(c, d, a, b, x[i + 14], 15, -1416354905); b = md5ii(b, c, d, a, x[i + 5], 21, -57434055)
      a = md5ii(a, b, c, d, x[i + 12], 6, 1700485571); d = md5ii(d, a, b, c, x[i + 3], 10, -1894986606)
      c = md5ii(c, d, a, b, x[i + 10], 15, -1051523); b = md5ii(b, c, d, a, x[i + 1], 21, -2054922799)
      a = md5ii(a, b, c, d, x[i + 8], 6, 1873313359); d = md5ii(d, a, b, c, x[i + 15], 10, -30611744)
      c = md5ii(c, d, a, b, x[i + 6], 15, -1560198380); b = md5ii(b, c, d, a, x[i + 13], 21, 1309151649)
      a = md5ii(a, b, c, d, x[i + 4], 6, -145523070); d = md5ii(d, a, b, c, x[i + 11], 10, -1120210379)
      c = md5ii(c, d, a, b, x[i + 2], 15, 718787259); b = md5ii(b, c, d, a, x[i + 9], 21, -343485551)
      a = safeAdd(a, oa); b = safeAdd(b, ob); c = safeAdd(c, oc); d = safeAdd(d, od)
    }
    return [a, b, c, d]
  }
  function str2binl(str: string): number[] {
    const bin: number[] = []
    const mask = (1 << 8) - 1
    for (let i = 0; i < str.length * 8; i += 8) bin[i >> 5] |= (str.charCodeAt(i / 8) & mask) << (i % 32)
    return bin
  }
  function binl2hex(binarray: number[]): string {
    const hexTab = '0123456789abcdef'
    let str = ''
    for (let i = 0; i < binarray.length * 32; i += 8) str += hexTab.charAt((binarray[i >> 5] >>> (i % 32)) & 0xf) + hexTab.charAt((binarray[i >> 5] >>> (i % 32 + 4)) & 0xf)
    return str
  }
  // Handle UTF-8
  const utf8 = unescape(encodeURIComponent(input))
  return binl2hex(binlMD5(str2binl(utf8), utf8.length * 8))
}

// MD5 Tool
function Md5Tool() {
  const [input, setInput] = useState('')
  const [result, setResult] = useState('')
  const [copied, setCopied] = useState(false)
  const [mode, setMode] = useState<'text' | 'file'>('text')
  const [fileResult, setFileResult] = useState('')
  const [fileName, setFileName] = useState('')

  const generate = () => {
    if (!input) return
    setResult(md5(input))
  }

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setFileName(file.name)
    const buffer = await file.arrayBuffer()
    const bytes = new Uint8Array(buffer)
    // Process in chunks for large files
    let str = ''
    for (let i = 0; i < bytes.length; i++) str += String.fromCharCode(bytes[i])
    setFileResult(md5(str))
  }

  const copyResult = (text: string) => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="space-y-6">
      <div className="flex gap-3">
        <button onClick={() => { setMode('text'); setFileResult(''); setFileName('') }} className={`${mode === 'text' ? 'btn-primary' : 'btn-secondary'} text-sm px-4 py-2`}>文本模式</button>
        <button onClick={() => { setMode('file'); setResult('') }} className={`${mode === 'file' ? 'btn-primary' : 'btn-secondary'} text-sm px-4 py-2`}>文件模式</button>
      </div>
      {mode === 'text' ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className={cardCls}>
            <label className="text-sm font-medium mb-2">输入文本</label>
            <Textarea value={input} onChange={(e) => setInput(e.target.value)} placeholder="输入要计算 MD5 的文本" className={`flex-1 min-h-[200px] font-mono text-sm ${inputCls} resize-none`} />
            <button onClick={generate} className="btn-primary text-sm px-4 py-2 mt-3">生成 MD5</button>
          </div>
          <div className={cardCls}>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium">MD5 结果</label>
              <button onClick={() => copyResult(result)} className="cursor-pointer text-[#6B5344] hover:text-[#FF6B35] transition-colors">
                {copied ? <Check className="w-4 h-4 text-[#4A7C59]" /> : <Copy className="w-4 h-4" />}
              </button>
            </div>
            {result && <code className={`block break-all p-3 text-sm font-mono ${codeCls}`}>{result}</code>}
          </div>
        </div>
      ) : (
        <div className="bg-white p-6 border-2 border-foreground shadow-[6px_6px_0_#2C1810]">
          <label className="btn-primary text-sm px-4 py-2 inline-block cursor-pointer">
            选择文件
            <input type="file" onChange={handleFile} className="hidden" />
          </label>
          {fileName && <p className="text-sm text-[#6B5344] mb-3">文件: {fileName}</p>}
          {fileResult && (
            <div className="flex items-center gap-2">
              <code className={`flex-1 break-all p-3 text-sm font-mono ${codeCls}`}>{fileResult}</code>
              <button onClick={() => copyResult(fileResult)} className="cursor-pointer text-[#6B5344] hover:text-[#FF6B35]">
                {copied ? <Check className="w-4 h-4 text-[#4A7C59]" /> : <Copy className="w-4 h-4" />}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

const tools = [
  { id: 'json', label: 'JSON', icon: FileJson },
  { id: 'jwt', label: 'JWT', icon: Key },
  { id: 'base64', label: 'Base64', icon: Binary },
  { id: 'url', label: 'URL', icon: Link2 },
  { id: 'timestamp', label: '时间戳', icon: Clock },
  { id: 'hash', label: '哈希', icon: Hash },
  { id: 'regex', label: '正则', icon: Regex },
  { id: 'uuid', label: 'UUID', icon: Fingerprint },
  { id: 'md5', label: 'MD5', icon: FileText },
]

export default function Tools() {
  const [activeTab, setActiveTab] = useState('json')

  return (
    <div className="space-y-8">
      <h1 className="text-2xl sm:text-3xl font-bold">开发者工具</h1>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="bg-[#FFF5E6] border-2 border-foreground shadow-[4px_4px_0_#2C1810] rounded-none h-auto p-1 flex-wrap">
          {tools.map(({ id, label, icon: Icon }) => (
            <TabsTrigger key={id} value={id} className="data-[state=active]:bg-[#FF6B35] data-[state=active]:text-white data-[state=active]:shadow-none rounded-none px-3 py-2 text-sm cursor-pointer">
              <Icon className="w-4 h-4 mr-1.5" />
              {label}
            </TabsTrigger>
          ))}
        </TabsList>

        <TabsContent value="json" className="mt-6"><JsonTool /></TabsContent>
        <TabsContent value="jwt" className="mt-6"><JwtTool /></TabsContent>
        <TabsContent value="base64" className="mt-6"><Base64Tool /></TabsContent>
        <TabsContent value="url" className="mt-6"><UrlTool /></TabsContent>
        <TabsContent value="timestamp" className="mt-6"><TimestampTool /></TabsContent>
        <TabsContent value="hash" className="mt-6"><HashTool /></TabsContent>
        <TabsContent value="regex" className="mt-6"><RegexTool /></TabsContent>
        <TabsContent value="uuid" className="mt-6"><UuidTool /></TabsContent>
        <TabsContent value="md5" className="mt-6"><Md5Tool /></TabsContent>
      </Tabs>
    </div>
  )
}
