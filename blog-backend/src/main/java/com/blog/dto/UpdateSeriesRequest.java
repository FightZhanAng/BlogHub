package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateSeriesRequest {

    @Size(max = 100, message = "标题长度不能超过 100")
    private String title;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    @Size(max = 500, message = "封面图 URL 长度不能超过 500")
    private String coverImage;
}
