package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.ApiTestHistory;
import com.blog.exception.BusinessException;
import com.blog.mapper.ApiTestHistoryMapper;
import com.blog.service.ApiTestHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTestHistoryServiceImpl implements ApiTestHistoryService {

    private final ApiTestHistoryMapper historyMapper;

    @Override
    public Page<ApiTestHistory> listByUser(Long userId, int page, int size) {
        return historyMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<ApiTestHistory>()
                .eq(ApiTestHistory::getUserId, userId)
                .orderByDesc(ApiTestHistory::getCreatedAt)
        );
    }

    @Override
    public void delete(Long id, Long userId) {
        ApiTestHistory history = historyMapper.selectById(id);
        if (history == null || !history.getUserId().equals(userId)) {
            throw new BusinessException(404, "历史记录不存在");
        }
        historyMapper.deleteById(id);
    }

    @Override
    public void clear(Long userId) {
        historyMapper.delete(
            new LambdaQueryWrapper<ApiTestHistory>()
                .eq(ApiTestHistory::getUserId, userId)
        );
        log.info("清空 API 测试历史: userId={}", userId);
    }
}
