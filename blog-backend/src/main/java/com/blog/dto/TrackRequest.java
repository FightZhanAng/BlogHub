package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class TrackRequest {

    @NotBlank(message = "事件类型不能为空")
    private String event;

    private String url;

    private String referrer;

    private Long duration;

    /** 访客 ID */
    private String visitorId;

    /** 事件附加数据 */
    private Map<String, Object> data;

    /** 时间戳 */
    private Long ts;
}
