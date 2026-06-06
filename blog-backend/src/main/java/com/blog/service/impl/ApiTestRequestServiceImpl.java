package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.ApiTestRequestDTO;
import com.blog.entity.ApiTestRequest;
import com.blog.exception.BusinessException;
import com.blog.mapper.ApiTestRequestMapper;
import com.blog.service.ApiTestRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTestRequestServiceImpl implements ApiTestRequestService {

    private final ApiTestRequestMapper requestMapper;

    @Override
    public List<ApiTestRequest> listByCollection(Long collectionId) {
        return requestMapper.selectList(
            new LambdaQueryWrapper<ApiTestRequest>()
                .eq(ApiTestRequest::getCollectionId, collectionId)
                .orderByAsc(ApiTestRequest::getCreatedAt)
        );
    }

    @Override
    public ApiTestRequest save(ApiTestRequestDTO dto, Long userId) {
        ApiTestRequest request = new ApiTestRequest();
        request.setCollectionId(dto.getCollectionId());
        request.setUserId(userId);
        request.setName(dto.getName());
        request.setMethod(dto.getMethod());
        request.setUrl(dto.getUrl());
        request.setHeaders(dto.getHeaders());
        request.setBodyType(dto.getBodyType());
        request.setBody(dto.getBody());
        requestMapper.insert(request);
        log.info("保存 API 测试请求: id={}, name={}", request.getId(), dto.getName());
        return request;
    }

    @Override
    public ApiTestRequest update(Long id, ApiTestRequestDTO dto, Long userId) {
        ApiTestRequest request = requestMapper.selectById(id);
        if (request == null || !request.getUserId().equals(userId)) {
            throw new BusinessException(404, "请求不存在");
        }
        request.setName(dto.getName());
        request.setMethod(dto.getMethod());
        request.setUrl(dto.getUrl());
        request.setHeaders(dto.getHeaders());
        request.setBodyType(dto.getBodyType());
        request.setBody(dto.getBody());
        requestMapper.updateById(request);
        return request;
    }

    @Override
    public void delete(Long id, Long userId) {
        ApiTestRequest request = requestMapper.selectById(id);
        if (request == null || !request.getUserId().equals(userId)) {
            throw new BusinessException(404, "请求不存在");
        }
        requestMapper.deleteById(id);
        log.info("删除 API 测试请求: id={}", id);
    }
}
