package org.yuca.yuca.user.entity;

import java.util.regex.Pattern;

/**
 * 手机号值对象
 */
public class Phone {

    // 中国大陆手机号正则
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^1[3-9]\\d{9}$");

    private final String value;

    public Phone(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        String cleanValue = value.replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(cleanValue).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        this.value = cleanValue;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone phone = (Phone) o;
        return value.equals(phone.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
