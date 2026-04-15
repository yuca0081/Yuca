# 笔记模块（yuca-note）

笔记模块提供完整的笔记管理功能，支持笔记本、文件夹、文档的树形组织结构，以及标签分类、Markdown 编辑、自动保存等特性。

## 模块结构

```
yuca-note/
└── src/main/java/org/yuca/note/
    ├── controller/          # REST 接口
    │   ├── NoteBookController.java      # 笔记本管理
    │   ├── NoteItemController.java      # 节点管理（文件夹 + 文档）
    │   └── NoteTagController.java       # 标签管理
    ├── service/             # 业务逻辑
    │   ├── NoteBookService.java         # 笔记本服务
    │   ├── NoteItemService.java         # 节点服务
    │   └── NoteTagService.java          # 标签服务
    ├── mapper/              # 数据访问层
    │   ├── NoteBookMapper.java
    │   ├── NoteItemMapper.java
    │   ├── NoteTagMapper.java
    │   ├── NoteItemTagMapper.java
    │   └── NoteVersionMapper.java
    ├── entity/              # 数据实体
    │   ├── NoteBook.java                # 笔记本
    │   ├── NoteItem.java                # 节点（文件夹/文档统一表）
    │   ├── NoteTag.java                 # 标签
    │   ├── NoteItemTag.java             # 文档-标签关联
    │   └── NoteVersion.java             # 版本历史
    ├── dto/                 # 数据传输对象
    │   ├── request/         # 请求 DTO
    │   └── response/        # 响应 DTO
    ├── enums/               # 枚举定义
    │   ├── ItemType.java                # 节点类型：FOLDER / DOCUMENT
    │   ├── DocumentType.java            # 内容类型：MARKDOWN / RICH_TEXT
    │   └── DocumentStatus.java          # 文档状态：DRAFT / PUBLISHED / ARCHIVED
    └── util/                # 工具类
        └── NoteDataIntegrityUtils.java  # 数据完整性校验
```

## 数据模型

### 核心实体关系

```
用户 ──1:N──> 笔记本（note_book）
笔记本 ──1:N──> 节点（note_item）树形结构，支持无限层级嵌套
文档 ──M:N──> 标签（note_tag）通过 note_item_tag 关联
文档 ──1:N──> 版本历史（note_version，预留）
```

### 单表设计

`note_item` 表统一存储文件夹和文档两种节点类型，通过 `type` 字段区分：

| 字段 | 说明 | 文件夹 | 文档 |
|------|------|--------|------|
| type | 节点类型 | FOLDER | DOCUMENT |
| content | 文档内容 | NULL | 有值 |
| contentType | 内容格式 | NULL | MARKDOWN / RICH_TEXT |
| summary | 摘要 | NULL | 前500字 |
| status | 状态 | NULL | DRAFT / PUBLISHED / ARCHIVED |
| viewCount | 浏览次数 | NULL | 有值 |
| wordCount | 字数统计 | NULL | 有值 |
| childCount | 子项数量 | 有值 | NULL |

## 功能清单

### 1. 笔记本管理

| 功能 | 接口 | 说明 |
|------|------|------|
| 创建笔记本 | `POST /note/books` | 支持名称、描述、图标、主题颜色 |
| 获取笔记本列表 | `GET /note/books` | 返回当前用户所有笔记本 |
| 获取笔记本详情 | `GET /note/books/{id}` | 含节点数量等统计 |
| 更新笔记本 | `PUT /note/books/{id}` | 修改名称、描述、图标等 |
| 删除笔记本 | `DELETE /note/books/{id}` | 级联删除所有节点 |
| 设置默认笔记本 | `POST /note/books/{id}/default` | 设为用户默认笔记本 |

### 2. 节点管理（文件夹 + 文档）

| 功能 | 接口 | 说明 |
|------|------|------|
| 创建节点 | `POST /note/items` | 创建文件夹或文档 |
| 获取节点详情 | `GET /note/items/{id}` | 返回完整内容 |
| 更新节点 | `PUT /note/items/{id}` | 更新标题、内容、状态等 |
| 删除节点 | `DELETE /note/items/{id}` | 逻辑删除，文件夹级联删除 |
| 移动节点 | `POST /note/items/{id}/move` | 移至新父节点，防循环引用 |
| 获取树形结构 | `GET /note/items/books/{bookId}/tree` | 获取笔记本完整树 |
| 获取子节点列表 | `GET /note/items/{id}/children` | 获取直接子节点 |
| 获取最近文档 | `GET /note/items/recent` | 按更新时间倒序 |
| 获取置顶文档 | `GET /note/items/pinned` | 获取所有置顶文档 |
| 批量更新排序 | `POST /note/items/batch-sort` | 批量更新排序序号 |

### 3. 标签管理

| 功能 | 接口 | 说明 |
|------|------|------|
| 创建标签 | `POST /note/tags` | 支持名称和颜色 |
| 获取标签列表 | `GET /note/tags` | 返回用户所有标签 |
| 获取标签详情 | `GET /note/tags/{id}` | 含使用次数 |
| 更新标签 | `PUT /note/tags/{id}` | 修改名称、颜色 |
| 删除标签 | `DELETE /note/tags/{id}` | 自动清理关联关系 |
| 获取标签下的文档 | `GET /note/tags/{id}/items` | 按标签筛选文档 |
| 给文档添加标签 | `POST /note/items/{id}/tags` | 支持批量添加 |
| 移除文档标签 | `DELETE /note/items/{id}/tags/{tagId}` | 单个移除 |
| 获取文档标签列表 | `GET /note/items/{id}/tags` | 获取文档的所有标签 |

### 4. 编辑器功能（前端）

- **Markdown 编辑器**：支持 Markdown 语法输入与实时预览
- **自动保存**：2 秒防抖自动保存，防止数据丢失
- **手动保存**：底部工具栏提供手动保存按钮
- **字数统计**：实时显示当前文档字数
- **状态指示**：显示保存中、已保存等状态
- **标题编辑**：支持内联编辑文档标题

### 5. 树形结构管理（前端）

- **展开/折叠**：点击文件夹展开或收起子节点
- **右键菜单**：支持新建文件夹、新建文档、重命名、删除
- **层级缩进**：可视化展示树形层级关系
- **空状态提示**：无内容时引导用户创建

### 6. 笔记本卡片展示（前端）

- **网格/列表视图**：支持切换卡片展示模式
- **常用分组**：展示最近使用的笔记本
- **最近文档预览**：卡片上展示最近编辑的文档
- **文档计数**：显示每个笔记本的文档数量
- **时间格式化**：友好的相对时间展示（今天、昨天、N天前）

### 7. UI 交互特性（前端）

- **可调整侧栏宽度**：拖动分隔条调整左右面板比例
- **侧栏折叠**：一键折叠/展开左侧导航栏
- **搜索过滤**：实时搜索过滤笔记本列表
- **快捷键**：`Ctrl+J` 快速聚焦搜索框
- **毛玻璃效果**：半透明背景 + backdrop-filter 视觉效果
- **响应式布局**：适配桌面端和移动端
- **返回主页**：固定位置返回按钮

## 关键设计

### 数据完整性保障

- **所有权校验**：所有操作验证数据归属当前用户
- **循环引用防护**：移动节点时禁止移入自身后代节点
- **级联删除**：删除文件夹时自动递归删除所有子节点
- **计数自动维护**：自动维护父节点 `childCount` 和笔记本 `itemCount`
- **标签关联清理**：删除文档时自动清理标签关联和使用计数

### 软删除

所有实体均使用 `@TableLogic` 逻辑删除（`deleted` 字段：0 正常，1 已删除），数据不会物理删除。

### 版本历史

`note_version` 表已预留，支持未来实现文档版本管理和历史回溯功能。
