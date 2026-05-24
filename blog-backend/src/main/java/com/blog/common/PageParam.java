package com.blog.common;

/**
 * 统一分页请求参数
 */
public class PageParam {

    private int page = 1;
    private int size = 10;
    private String keyword;

    public PageParam() {}

    public PageParam(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public PageParam(int page, int size, String keyword) {
        this.page = page;
        this.size = size;
        this.keyword = keyword;
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
