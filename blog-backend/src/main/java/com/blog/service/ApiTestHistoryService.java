package com.blog.service;

import com.blog.entity.ApiTestHistory;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ApiTestHistoryService {
    Page<ApiTestHistory> listByUser(Long userId, int page, int size);
    void delete(Long id, Long userId);
    void clear(Long userId);
}
