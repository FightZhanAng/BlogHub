package com.blog.common.enums;

/**
 * 文章状态枚举
 */
public enum PostStatus {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布");

    private final int code;
    private final String desc;

    PostStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static PostStatus fromCode(Integer code) {
        if (code == null) return PUBLISHED;
        for (PostStatus s : values()) {
            if (s.code == code) return s;
        }
        return PUBLISHED;
    }
}
