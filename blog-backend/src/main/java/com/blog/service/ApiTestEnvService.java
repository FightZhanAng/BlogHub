package com.blog.service;

import com.blog.dto.ApiTestEnvDTO;
import com.blog.entity.ApiTestEnv;
import java.util.List;

public interface ApiTestEnvService {
    List<ApiTestEnv> listByUser(Long userId);
    ApiTestEnv create(ApiTestEnvDTO dto, Long userId);
    ApiTestEnv update(Long id, ApiTestEnvDTO dto, Long userId);
    void delete(Long id, Long userId);
}
