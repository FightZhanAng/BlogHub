package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.ApiTestRequestDTO;
import com.blog.entity.ApiTestCollection;
import com.blog.entity.ApiTestRequest;
import com.blog.exception.BusinessException;
import com.blog.mapper.ApiTestCollectionMapper;
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

    private final ApiTestCollectionMapper collectionMapper;

    /** 校验集合存在且属于当前用户，否则按不存在处理 */
    private void assertCollectionOwned(Long collectionId, Long userId) {
        if (collectionId == null) {
            throw new BusinessException(400, "collectionId 不能为空");
        }
        ApiTestCollection collection = collectionMapper.selectById(collectionId);
        if (collection == null || collection.getUserId() == null || !collection.getUserId().equals(userId)) {
            throw new BusinessException(404, "集合不存在");
        }
    }

    @Override
    public List<ApiTestRequest> listByCollection(Long collectionId, Long userId) {
        // 之前只按 collectionId 查、不校验归属，属于 IDOR：
        // 任意登录用户可以读取他人集合里保存的请求（headers 里常带 Token/API Key）
        assertCollectionOwned(collectionId, userId);
        return requestMapper.selectList(
            new LambdaQueryWrapper<ApiTestRequest>()
                .eq(ApiTestRequest::getCollectionId, collectionId)
                .eq(ApiTestRequest::getUserId, userId)
                .orderByAsc(ApiTestRequest::getCreatedAt)
        );
    }

    @Override
    public ApiTestRequest save(ApiTestRequestDTO dto, Long userId) {
        // 同样要校验：否则可以把请求塞进别人的集合
        assertCollectionOwned(dto.getCollectionId(), userId);
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
        if (request == null || request.getUserId() == null || !request.getUserId().equals(userId)) {
            throw new BusinessException(404, "请求不存在");
        }
        if (dto.getCollectionId() != null && !dto.getCollectionId().equals(request.getCollectionId())) {
            assertCollectionOwned(dto.getCollectionId(), userId);
            request.setCollectionId(dto.getCollectionId());
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
        if (request == null || request.getUserId() == null || !request.getUserId().equals(userId)) {
            throw new BusinessException(404, "请求不存在");
        }
        requestMapper.deleteById(id);
        log.info("删除 API 测试请求: id={}", id);
    }
}
