"""
密码生成器 - Python入门项目1
功能：生成指定长度和复杂度的随机密码
"""

import random
import string


def generate_password(length=12, use_uppercase=True, use_lowercase=True,
                     use_digits=True, use_symbols=True):
    """
    生成随机密码

    参数:
        length: 密码长度 (默认: 12)
        use_uppercase: 是否包含大写字母 (默认: True)
        use_lowercase: 是否包含小写字母 (默认: True)
        use_digits: 是否包含数字 (默认: True)
        use_symbols: 是否包含特殊字符 (默认: True)

    返回:
        生成的密码字符串
    """
    # 定义字符集
    char_pool = ''

    if use_uppercase:
        char_pool += string.ascii_uppercase  # A-Z
    if use_lowercase:
        char_pool += string.ascii_lowercase  # a-z
    if use_digits:
        char_pool += string.digits  # 0-9
    if use_symbols:
        char_pool += string.punctuation  # !@#$%^&*() 等

    # 检查是否至少选择了一种字符类型
    if not char_pool:
        raise ValueError("至少需要选择一种字符类型！")

    # 生成密码
    password = ''.join(random.choice(char_pool) for _ in range(length))

    return password


def generate_multiple_passwords(count, length=12, **kwargs):
    """
    生成多个密码

    参数:
        count: 生成密码的数量
        length: 每个密码的长度
        **kwargs: 其他选项 (use_uppercase, use_lowercase等)

    返回:
        密码列表
    """
    return [generate_password(length, **kwargs) for _ in range(count)]


def check_password_strength(password):
    """
    检查密码强度

    参数:
        password: 要检查的密码

    返回:
        强度等级 (弱/中/强)
    """
    score = 0

    # 检查长度
    if len(password) >= 8:
        score += 1
    if len(password) >= 12:
        score += 1

    # 检查字符类型
    if any(c.islower() for c in password):
        score += 1
    if any(c.isupper() for c in password):
        score += 1
    if any(c.isdigit() for c in password):
        score += 1
    if any(c in string.punctuation for c in password):
        score += 1

    # 评估强度
    if score <= 2:
        return "弱"
    elif score <= 4:
        return "中"
    else:
        return "强"


def main():
    """主函数 - 交互式命令行界面"""
    print("=" * 50)
    print("🔐 密码生成器")
    print("=" * 50)

    try:
        # 获取用户输入
        length = int(input("\n请输入密码长度 (8-32, 默认12): ") or "12")
        length = max(8, min(32, length))  # 限制在8-32之间

        count = int(input("要生成几个密码? (1-10, 默认1): ") or "1")
        count = max(1, min(10, count))  # 限制在1-10之间

        # 字符类型选择
        print("\n请选择要包含的字符类型:")
        use_uppercase = input("包含大写字母? (y/n, 默认y): ").lower() != 'n'
        use_lowercase = input("包含小写字母? (y/n, 默认y): ").lower() != 'n'
        use_digits = input("包含数字? (y/n, 默认y): ").lower() != 'n'
        use_symbols = input("包含特殊字符? (y/n, 默认y): ").lower() != 'n'

        # 生成密码
        print("\n" + "=" * 50)
        print("生成的密码:")
        print("=" * 50)

        passwords = generate_multiple_passwords(
            count=count,
            length=length,
            use_uppercase=use_uppercase,
            use_lowercase=use_lowercase,
            use_digits=use_digits,
            use_symbols=use_symbols
        )

        for i, pwd in enumerate(passwords, 1):
            strength = check_password_strength(pwd)
            print(f"\n密码 #{i}: {pwd}")
            print(f"强度: {strength}")

        # 保存到文件选项
        save = input("\n\n是否保存到文件? (y/n): ").lower()
        if save == 'y':
            filename = "passwords.txt"
            with open(filename, 'w', encoding='utf-8') as f:
                f.write("生成的密码\n")
                f.write("=" * 50 + "\n\n")
                for i, pwd in enumerate(passwords, 1):
                    f.write(f"密码 #{i}: {pwd}\n")
                    f.write(f"强度: {check_password_strength(pwd)}\n\n")
            print(f"✅ 密码已保存到 {filename}")

    except ValueError as e:
        print(f"❌ 错误: {e}")
    except KeyboardInterrupt:
        print("\n\n👋 再见!")


if __name__ == "__main__":
    main()
