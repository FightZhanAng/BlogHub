package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class AlbumDTO {

    @NotBlank(message = "相册标题不能为空")
    @Size(max = 100, message = "标题最多100个字符")
    private String title;

    @Size(max = 500, message = "描述最多500个字符")
    private String description;

    private String coverImage;

    @Size(max = 50, message = "宝宝昵称最多50个字符")
    private String babyName;

    private LocalDate babyBirthDate;

    private Boolean isPrivate = true;
}
