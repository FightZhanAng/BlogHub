package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量更新排序请求
 */
@Data
public class MenuSortRequest {

    @NotNull(message = "ID 列表不能为空")
    private List<Long> ids;
}
