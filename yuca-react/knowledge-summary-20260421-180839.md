## 对话知识点总结

### 主题：React 笔记应用 — Markdown 编辑与渲染

**知识点：react-markdown 编辑/预览切换模式**
- **原理说明：** 通过一个 `editing` 状态布尔值控制内容区显示 textarea（编辑态）还是 ReactMarkdown（预览态）。点击预览区切换到编辑态，textarea 的 `onBlur` 或按 Esc 切回预览态。使用绝对定位（`absolute inset-0`）确保两种模式都完整填充父容器。
- **举例：** 笔记模块中，用户点击内容区 → `setEditing(true)` → 显示 textarea 可编辑原始 Markdown；失焦或按 Esc → `setEditing(false)` → 显示渲染后的 HTML。
- **延伸知识：** `remark-breaks` 插件让单个换行符渲染为 `<br>`，符合用户直觉；`rehype-highlight` 提供代码块语法高亮。

---

**知识点：useAutoSave 自定义 Hook 与状态同步**
- **原理说明：** 自动保存 Hook 监听表单数据变化，定时触发保存回调。保存成功后需同步更新 UI 状态（如侧栏树节点的标题），否则会出现编辑区和侧栏标题不一致的问题。
- **举例：** 修改笔记标题后，自动保存回调中调用 `setTreeNodes(prev => ...)` 递归更新树结构中对应节点的 `title`，实现侧栏标题与编辑区实时同步。
- **延伸知识：** React 中异步操作后的状态更新需用函数式 `setState(prev => ...)` 避免闭包陷阱。

---

### 主题：树形结构的 UI 交互设计

**知识点：文件夹的三点菜单（ContextMenu）模式**
- **原理说明：** 悬停时通过 `opacity-0 group-hover:opacity-100` 显示操作按钮，点击后渲染一个绝对定位的下拉菜单。通过 `useRef` + `document.addEventListener('mousedown')` 监听外部点击来关闭菜单。
- **举例：** NoteTree 中 `FolderMenu` 组件提供"创建"、"重命名"、"删除"三个操作，`menuRef` 指向菜单容器，点击外部区域时调用 `setOpen(false)` 关闭。
- **延伸知识：** 也可使用 Radix UI 的 `DropdownMenu` 组件实现类似功能，自带焦点管理和键盘导航。

---

### 主题：后端级联删除与递归 SQL

**知识点：PostgreSQL 递归 CTE 查询子孙节点**
- **原理说明：** 使用 `WITH RECURSIVE` 公共表表达式实现树的递归遍历。锚点查询选定根节点，递归部分通过 `parent_id = t.id` 自连接逐层向下查找，最终返回所有后代节点。
- **举例：** 删除文件夹时，`getDescendants(itemId)` 用递归 CTE 查出所有子孙，按 ID 倒序排列后逐一删除（先删子再删父），最后删除文件夹自身。
- **延伸知识：** 递归 CTE 在深层嵌套时可能有性能问题，可考虑用 `ltree` 扩展或物化路径（materialized path）模式优化。

---

**知识点：不要依赖缓存的 childCount 做删除判断**
- **原理说明：** 数据库中的冗余计数字段（如 `childCount`）可能因并发操作或未及时更新而不准确。删除文件夹时应始终走级联删除逻辑，而非仅在 `childCount > 0` 时才级联。
- **举例：** 原代码 `if (item.isFolder() && item.getChildCount() != null && item.getChildCount() > 0)` 条件过于严格，改为 `if (item.isFolder())` 始终执行级联删除，即使 `getDescendants` 返回空列表也能正确处理空文件夹。
- **延伸知识：** 类似的冗余字段同步问题在树形结构中很常见，如 `sort_order`、`depth` 等，更新时需保证事务一致性。

---

### 主题：前端确认弹窗的差异化提示

**知识点：根据删除目标类型显示不同警告信息**
- **原理说明：** 删除操作的风险等级不同（删除单个文档 vs 删除文件夹 vs 删除整个笔记本），确认弹窗的描述文字应明确告知用户操作的影响范围，避免误删。
- **举例：** 删除文件夹时提示"其中的所有子文件和子文件夹也将被删除"；删除笔记本时提示"其中的所有内容将被删除"；删除单个文档则仅确认是否删除该项。

---

### 学习建议

1. **递归 SQL 深入学习**：了解 PostgreSQL `WITH RECURSIVE` 的性能特征、深度限制，以及替代方案（如 `ltree` 扩展、闭包表 Closure Table），在数据量增长后可能需要优化策略。
2. **React 自定义 Hook 设计模式**：当前 `useAutoSave` 是一个典型的"副作用 Hook"模式，可进一步学习 `useDebounce`、`useSWR` 等类似模式，理解如何在 Hook 中正确管理定时器、请求取消和竞态条件。
