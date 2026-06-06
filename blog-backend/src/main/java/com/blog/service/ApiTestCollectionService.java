package com.blog.service;

import com.blog.dto.ApiTestCollectionDTO;
import com.blog.entity.ApiTestCollection;
import java.util.List;

public interface ApiTestCollectionService {
    List<ApiTestCollection> listByUser(Long userId);
    ApiTestCollection create(ApiTestCollectionDTO dto, Long userId);
    ApiTestCollection update(Long id, ApiTestCollectionDTO dto, Long userId);
    void delete(Long id, Long userId);
}
