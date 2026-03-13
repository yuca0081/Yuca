# 项目1：密码生成器

## 📚 学习目标

通过这个项目，你将学会：

1. **Python基础语法**
   - 函数定义和参数
   - 字符串操作
   - 条件判断和循环

2. **标准库的使用**
   - `random` 模块 - 生成随机选择
   - `string` 模块 - 字符常量

3. **文件操作**
   - 打开和写入文件
   - 使用 `with` 语句管理资源

4. **异常处理**
   - `try-except` 捕获错误
   - `ValueError` 处理无效输入

5. **用户交互**
   - `input()` 获取用户输入
   - `print()` 格式化输出

## 🚀 运行方法

```bash
# 进入项目目录
cd python-projects/01-password-generator

# 运行程序
python password_generator.py
```

## 💡 核心知识点解析

### 1. random.choice()
从序列中随机选择一个元素：
```python
import random
characters = "abc123"
random.choice(characters)  # 可能返回 'a', 'b', 'c', '1', '2', '3'
```

### 2. string 模块常量
```python
import string

string.ascii_lowercase  # 'abcdefghijklmnopqrstuvwxyz'
string.ascii_uppercase  # 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
string.digits           # '0123456789'
string.punctuation      # 特殊字符
```

### 3. 列表推导式
```python
# 生成12个随机字符
password = ''.join(random.choice(char_pool) for _ in range(12))
```

### 4. 文件写入
```python
with open('file.txt', 'w', encoding='utf-8') as f:
    f.write("内容")
```

### 5. 字符串检查
```python
any(c.isupper() for c in password)  # 是否有大写字母
any(c.isdigit() for c in password)  # 是否有数字
```

## ✨ 功能特点

- ✅ 可自定义密码长度（8-32位）
- ✅ 可选择字符类型（大小写、数字、符号）
- ✅ 批量生成多个密码
- ✅ 密码强度检测
- ✅ 保存到文件功能

## 🎯 练习建议

1. **基础练习**：
   - 修改密码强度检测算法
   - 添加更多字符类型选项

2. **进阶练习**：
   - 添加GUI界面（使用tkinter）
   - 生成易记的密码（如单词+数字）
   - 添加密码历史记录

3. **挑战练习**：
   - 检查密码是否在常见密码列表中
   - 生成语音友好的密码（避免混淆字符如0/O, 1/l/I）

## 📝 代码结构

```
generate_password()           # 核心功能：生成单个密码
generate_multiple_passwords()  # 批量生成
check_password_strength()      # 密码强度检测
main()                         # 交互式界面
```

## 🔍 代码亮点

1. **默认参数**：函数参数有默认值，使用更灵活
2. **错误处理**：优雅处理用户输入错误
3. **用户友好**：清晰的提示和格式化输出
4. **代码复用**：辅助函数可以被其他代码导入使用
