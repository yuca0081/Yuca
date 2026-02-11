package org.yuca.yuca.user.entity;

/**
 * 密码值对象
 */
public class Password {

    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 20;

    private final String value;

    private Password(String value) {
        this.value = value;
    }

    /**
     * 创建原始密码（用于注册、修改密码）
     */
    public static Password ofPlain(String plainPassword) {
        validatePlainPassword(plainPassword);
        return new Password(plainPassword);
    }

    /**
     * 创建加密后的密码（从数据库读取）
     */
    public static Password ofEncoded(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Encoded password cannot be empty");
        }
        return new Password(encodedPassword);
    }

    private static void validatePlainPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Password length must be between " + MIN_LENGTH + " and " + MAX_LENGTH
            );
        }
        // 密码必须包含字母和数字
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.**");
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("Password must contain both letters and numbers");
        }
    }

    public String getValue() {
        return value;
    }

    /**
     * 获取原始密码值（用于加密）
     */
    public String getPlainValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Password password = (Password) o;
        return value.equals(password.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "******";
    }
}
