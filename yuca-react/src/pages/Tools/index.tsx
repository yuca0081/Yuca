import { useState } from 'react'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Copy, Check, ArrowRightLeft, FileJson, Key, Binary, Link2, Clock, Hash, Regex, Fingerprint } from 'lucide-react'

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

const tools = [
  { id: 'json', label: 'JSON', icon: FileJson },
  { id: 'jwt', label: 'JWT', icon: Key },
  { id: 'base64', label: 'Base64', icon: Binary },
  { id: 'url', label: 'URL', icon: Link2 },
  { id: 'timestamp', label: '时间戳', icon: Clock },
  { id: 'hash', label: '哈希', icon: Hash },
  { id: 'regex', label: '正则', icon: Regex },
  { id: 'uuid', label: 'UUID', icon: Fingerprint },
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
      </Tabs>
    </div>
  )
}
