# Yuca 个人网页前端项目

基于 Vue 3 + TypeScript + Naive UI 构建的简约现代个人主页。

## 技术栈

- **Vue 3** - 渐进式前端框架
- **TypeScript** - 类型安全的 JavaScript
- **Vite** - 快速的前端构建工具
- **Naive UI** - 简约现代的 Vue 3 组件库
- **Vue Router** - Vue 官方路由管理器
- **Pinia** - Vue 官方状态管理方案
- **Axios** - HTTP 请求客户端

## 项目特性

- ✅ 简约现代的卡片式设计
- ✅ 支持自定义背景图片
- ✅ 响应式布局，适配移动端
- ✅ TypeScript 类型安全
- ✅ 路由守卫，权限控制
- ✅ Axios 拦截器，自动处理 Token
- ✅ 与后端 API 完美对接

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

访问：http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 项目结构

```
yuca-frontend/
├── public/                      # 静态资源
│   └── background.jpg           # 背景图片（可自定义）
├── src/
│   ├── api/                     # API 请求封装
│   │   ├── index.ts             # Axios 实例配置
│   │   └── user.ts              # 用户相关 API
│   ├── assets/                  # 静态资源
│   │   └── styles/              # 全局样式
│   │       └── main.css         # 主样式文件
│   ├── components/              # 公共组件
│   ├── composables/             # 组合式函数
│   │   └── useTime.ts           # 时间相关逻辑
│   ├── router/                  # 路由配置
│   │   └── index.ts             # 路由定义
│   ├── stores/                  # Pinia 状态管理
│   │   ├── user.ts              # 用户状态
│   │   └── app.ts               # 应用全局状态
│   ├── types/                   # TypeScript 类型定义
│   │   └── api.ts               # API 类型
│   ├── utils/                   # 工具函数
│   │   └── format.ts            # 格式化工具
│   ├── views/                   # 页面组件
│   │   ├── Home.vue             # 首页
│   │   ├── Blog.vue             # 博客页
│   │   ├── Login.vue            # 登录页
│   │   └── Profile.vue          # 个人资料页
│   ├── App.vue                  # 根组件
│   └── main.ts                  # 应用入口
├── .env.development             # 开发环境变量
├── .env.production              # 生产环境变量
├── package.json
├── tsconfig.json
└── vite.config.ts               # Vite 配置
```

## 自定义配置

### 修改背景图片

1. 将你的背景图片放到 `public/background.jpg`
2. 或者修改 `src/assets/styles/main.css` 中的 `background-image` 属性

### 修改主题色

编辑 `src/main.ts` 中的主题配置：

```typescript
themeOverrides: {
  common: {
    primaryColor: '#14b8a6',           // 主色调
    primaryColorHover: '#0f766e',      // 悬停色
    primaryColorPressed: '#134e4a'     // 按下色
  }
}
```

### 修改 API 地址

编辑 `.env.development` 或 `.env.production`：

```bash
VITE_API_BASE_URL=http://localhost:8500
```

## 与后端对接

后端 API 地址配置在 `.env` 文件中。开发模式下，Vite 会自动代理 `/api` 请求到后端服务器。

### API 响应格式

后端返回的 ApiResponse 格式：

```typescript
{
  code: number,
  data: any,
  message: string
}
```

### 认证流程

1. 用户登录，后端返回 JWT Token
2. Token 存储在 localStorage 和 Pinia Store
3. 后续请求自动在 Header 中添加 `Authorization: Bearer {token}`
4. Token 过期自动跳转登录页

## 部署

### 构建生产版本

```bash
npm run build
```

### 部署到 Nginx

1. 将 `dist/` 目录上传到服务器
2. 配置 Nginx：

```nginx
server {
  listen 80;
  server_name your-domain.com;
  root /var/www/yuca-frontend/dist;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  # API 代理
  location /api/ {
    proxy_pass http://localhost:8500;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

## 学习资源

- [Vue 3 官方文档](https://cn.vuejs.org/)
- [Naive UI 官方文档](https://www.naiveui.com/zh-CN)
- [Vite 官方文档](https://cn.vitejs.dev/)
- [Vue Router 官方文档](https://router.vuejs.org/zh/)
- [Pinia 官方文档](https://pinia.vuejs.org/zh/)
- [TypeScript 官方文档](https://www.typescriptlang.org/zh/)

## License

MIT

