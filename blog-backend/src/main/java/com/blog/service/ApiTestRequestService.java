package com.blog.service;

import com.blog.dto.ApiTestRequestDTO;
import com.blog.entity.ApiTestRequest;
import java.util.List;

public interface ApiTestRequestService {
    List<ApiTestRequest> listByCollection(Long collectionId);
    ApiTestRequest save(ApiTestRequestDTO dto, Long userId);
    ApiTestRequest update(Long id, ApiTestRequestDTO dto, Long userId);
    void delete(Long id, Long userId);
}
