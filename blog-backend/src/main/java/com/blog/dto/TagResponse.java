package com.blog.dto;

/**
 * 标签响应 DTO
 */
public class TagResponse {

    private Long id;
    private String name;
    private String slug;
    private Integer postCount;

    public TagResponse() {}

    public TagResponse(Long id, String name, String slug, Integer postCount) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.postCount = postCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Integer getPostCount() { return postCount; }
    public void setPostCount(Integer postCount) { this.postCount = postCount; }
}
