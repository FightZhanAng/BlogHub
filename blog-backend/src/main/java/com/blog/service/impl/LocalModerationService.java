package com.blog.service.impl;

import com.blog.common.SensitiveWordFilter;
import com.blog.service.ContentModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalModerationService implements ContentModerationService {

    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    public ModerationResult moderateText(String text) {
        if (text == null || text.isEmpty()) return ModerationResult.pass(text);
        if (!sensitiveWordFilter.isInitialized()) return ModerationResult.pass(text);

        int maxLevel = sensitiveWordFilter.getMaxLevel(text);
        if (maxLevel == 0) return ModerationResult.pass(text);

        if (maxLevel >= 2) {
            return ModerationResult.reject("内容包含违规词汇");
        }

        String filtered = sensitiveWordFilter.filter(text);
        return ModerationResult.replace(filtered);
    }

    @Override
    public ModerationResult moderateImage(String imageUrl) {
        // TODO: 接入阿里云内容安全图片审核
        return ModerationResult.pass(imageUrl);
    }
}
