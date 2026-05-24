package com.blog.common.enums;

/**
 * 用户角色枚举
 */
public enum UserRole {

    ADMIN("admin", "管理员"),
    USER("user", "普通用户");

    private final String code;
    private final String desc;

    UserRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static UserRole fromCode(String code) {
        if (code == null) return USER;
        for (UserRole r : values()) {
            if (r.code.equals(code)) return r;
        }
        return USER;
    }
}
