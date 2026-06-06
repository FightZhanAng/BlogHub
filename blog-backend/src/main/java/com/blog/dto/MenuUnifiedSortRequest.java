package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 统一排序请求项
 */
@Data
public class MenuUnifiedSortRequest {

    @NotNull(message = "排序列表不能为空")
    private List<SortEntry> entries;

    @Data
    public static class SortEntry {
        @NotBlank(message = "类型不能为空")
        private String type;  // "group" 或 "item"

        @NotNull(message = "ID 不能为空")
        private Long id;
    }
}
